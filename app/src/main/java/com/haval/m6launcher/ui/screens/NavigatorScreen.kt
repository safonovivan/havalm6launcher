package com.haval.m6launcher.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.haval.m6launcher.data.SettingsRepository
import com.haval.m6launcher.data.UserSettings
import com.haval.m6launcher.ui.components.NavPlayerPanel
import com.haval.m6launcher.ui.components.RouteManager
import com.haval.m6launcher.ui.components.YandexMapComponent
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session
import kotlinx.coroutines.flow.first
import java.util.Calendar

enum class RatioState(val mapWeight: Float) { MAP_70(0.7f), MAP_50(0.5f) }

// Глобальный флаг, чтобы не инициализировать карты дважды
object MapKitState {
    var isInitialized = false
}

@Composable
fun NavigatorScreen(
    settingsRepo: SettingsRepository, // <-- ТЕПЕРЬ ФУНКЦИЯ ПРИНИМАЕТ settingsRepo
    mapTheme: String,
    onNavigateToSettings: () -> Unit,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onPickFolder: () -> Unit
) {
    val context = LocalContext.current
    var isMapReady by remember { mutableStateOf(MapKitState.isInitialized) }

    // ИНИЦИАЛИЗАЦИЯ КАРТ
    LaunchedEffect(Unit) {
        if (!MapKitState.isInitialized) {
            val apiKey = settingsRepo.userSettings.first().yandexMapApiKey
            if (apiKey.isNotBlank()) {
                try {
                    MapKitFactory.setApiKey(apiKey)
                    MapKitFactory.initialize(context)
                    MapKitState.isInitialized = true
                    isMapReady = true
                } catch (e: Throwable) { }
            }
        }
    }

    DisposableEffect(Unit) {
        if (isMapReady) {
            MapKitFactory.getInstance().onStart()
        }
        onDispose {
            if (isMapReady) {
                MapKitFactory.getInstance().onStop()
            }
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted -> hasLocationPermission = isGranted }

    // СЛУШАТЕЛЬ GPS ДЛЯ КАРТЫ (без фильтра по 100м, чтобы стрелка двигалась плавно)
    var currentLoc by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { currentLoc = location }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            for (provider in lm.getProviders(true)) lm.requestLocationUpdates(provider, 1000L, 0f, listener)
        } catch (e: Exception) {}
        onDispose { lm.removeUpdates(listener) }
    }

    val isNightMode = remember(mapTheme) {
        when (mapTheme) { "light" -> false; "dark" -> true; else -> { val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); hour < 7 || hour >= 19 } }
    }

    var currentRatio by remember { mutableStateOf(RatioState.MAP_70) }
    var isSwapped by remember { mutableStateOf(false) }

    // УБРАЛИ АНИМАЦИЮ, ЧТОБЫ НЕ ТОРМОЗИЛО
    val mapWeight = currentRatio.mapWeight
    val playerWeight = 1f - mapWeight

    var mapInstance by remember { mutableStateOf<com.yandex.mapkit.map.Map?>(null) }
    var routeInfo by remember { mutableStateOf<String?>(null) }
    var isBuildingRoute by remember { mutableStateOf(false) }
    var destinationPoint by remember { mutableStateOf<Point?>(null) }
    var isTrafficVisible by remember { mutableStateOf(false) }
    var tappedPoint by remember { mutableStateOf<Point?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeoObject>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val searchManager = remember(isMapReady) {
        if (isMapReady) {
            try { SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED) } catch (e: Exception) { null }
        } else null
    }

    LaunchedEffect(Unit) { if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    LaunchedEffect(currentLoc, mapInstance) {
        if (currentLoc != null && mapInstance != null) {
            mapInstance?.move(CameraPosition(Point(currentLoc!!.latitude, currentLoc!!.longitude), 15f, 0f, 0f))
        }
    }

    fun buildRouteTo(target: Point) {
        if (currentLoc == null) {
            Toast.makeText(context, "Ожидание GPS...", Toast.LENGTH_SHORT).show()
            return
        }
        isBuildingRoute = true
        routeInfo = "Строим маршрут..."
        tappedPoint = null
        destinationPoint = target

        val startPoint = Point(currentLoc!!.latitude, currentLoc!!.longitude)

        RouteManager.buildRoute(
            from = startPoint, to = target,
            onSuccess = { route ->
                isBuildingRoute = false
                routeInfo = "В пути: ${route.metadata.weight.time.text} (${route.metadata.weight.distance.text})"
                mapInstance?.let { map ->
                    map.mapObjects.clear()
                    val polyline = map.mapObjects.addPolyline(route.geometry)
                    polyline.strokeWidth = 8f
                    polyline.setStrokeColor(AndroidColor.BLUE)
                    val points = route.geometry.points
                    if (points.isNotEmpty()) {
                        var minLat = points[0].latitude; var maxLat = points[0].latitude
                        var minLon = points[0].longitude; var maxLon = points[0].longitude
                        for (p in points) {
                            if (p.latitude < minLat) minLat = p.latitude
                            if (p.latitude > maxLat) maxLat = p.latitude
                            if (p.longitude < minLon) minLon = p.longitude
                            if (p.longitude > maxLon) maxLon = p.longitude
                        }
                        map.move(CameraPosition(Point((minLat + maxLat) / 2, (minLon + maxLon) / 2), 10f, 0f, 0f))
                    }
                }
            },
            onError = { error -> isBuildingRoute = false; routeInfo = "Ошибка: $error"; destinationPoint = null }
        )
    }

    val mapContent = remember {
        movableContentOf<Modifier> { modifier ->
            MapPanel(
                modifier = modifier, isMapReady = isMapReady, isNightMode = isNightMode, isTrafficVisible = isTrafficVisible, hasLocationPermission = hasLocationPermission,
                onMapReady = { mapInstance = it },
                onMapTap = { point -> if (destinationPoint == null && !isBuildingRoute) { tappedPoint = point; mapInstance?.mapObjects?.clear(); mapInstance?.mapObjects?.addPlacemark(point) } },
                searchQuery = searchQuery, searchResults = searchResults, isSearching = isSearching,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    if (query.length > 2) {
                        mapInstance?.cameraPosition?.target?.let { tp ->
                            isSearching = true
                            searchManager?.submit(query, Geometry.fromPoint(tp), SearchOptions(), object : Session.SearchListener {
                                override fun onSearchResponse(response: Response) { isSearching = false; searchResults = response.collection.children.mapNotNull { it.obj } }
                                override fun onSearchError(error: com.yandex.runtime.Error) { isSearching = false }
                            })
                        }
                    } else searchResults = emptyList()
                },
                onSearchResultClick = { p, name -> searchQuery = name; searchResults = emptyList(); buildRouteTo(p) },
                onMyLocationClick = {
                    if (currentLoc != null) mapInstance?.move(CameraPosition(Point(currentLoc!!.latitude, currentLoc!!.longitude), 15f, 0f, 0f))
                    else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onTrafficToggle = { isTrafficVisible = !isTrafficVisible },
                onZoomIn = { mapInstance?.let { val c = it.cameraPosition; it.move(CameraPosition(c.target, c.zoom + 1f, c.azimuth, c.tilt)) } },
                onZoomOut = { mapInstance?.let { val c = it.cameraPosition; it.move(CameraPosition(c.target, c.zoom - 1f, c.azimuth, c.tilt)) } },
                onClear = { mapInstance?.mapObjects?.clear(); routeInfo = null; destinationPoint = null; tappedPoint = null },
                routeInfo = routeInfo, destinationPoint = destinationPoint, tappedPoint = tappedPoint,
                onBuildRoute = { buildRouteTo(it) }, onCancelRoute = { tappedPoint = null; mapInstance?.mapObjects?.clear() }, onHideInfo = { routeInfo = null },
                onRatioClick = { currentRatio = if (currentRatio == RatioState.MAP_70) RatioState.MAP_50 else RatioState.MAP_70 },
                onSwapClick = { isSwapped = !isSwapped }
            )
        }
    }

    val playerContent = remember {
        movableContentOf<Modifier> { modifier ->
            NavPlayerPanel(
                modifier = modifier,
                settings = settings,
                onSettingsChange = onSettingsChange,
                onPickFolder = onPickFolder
            )
        }
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!isSwapped) {
            mapContent(Modifier.weight(mapWeight).fillMaxHeight())
            playerContent(Modifier.weight(playerWeight).fillMaxHeight())
        } else {
            playerContent(Modifier.weight(playerWeight).fillMaxHeight())
            mapContent(Modifier.weight(mapWeight).fillMaxHeight())
        }
    }
}

@Composable
fun MapPanel(
    modifier: Modifier, isMapReady: Boolean, isNightMode: Boolean, isTrafficVisible: Boolean, hasLocationPermission: Boolean,
    onMapReady: (com.yandex.mapkit.map.Map) -> Unit, onMapTap: (Point) -> Unit,
    searchQuery: String, searchResults: List<GeoObject>, isSearching: Boolean,
    onSearchQueryChange: (String) -> Unit, onSearchResultClick: (Point, String) -> Unit,
    onMyLocationClick: () -> Unit, onTrafficToggle: () -> Unit, onZoomIn: () -> Unit, onZoomOut: () -> Unit, onClear: () -> Unit,
    routeInfo: String?, destinationPoint: Point?, tappedPoint: Point?,
    onBuildRoute: (Point) -> Unit, onCancelRoute: () -> Unit, onHideInfo: () -> Unit, onRatioClick: () -> Unit, onSwapClick: () -> Unit
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isMapReady) {
                MapPlaceholder { }
            } else {
                YandexMapComponent(modifier = Modifier.fillMaxSize(), isNightMode = isNightMode, isTrafficVisible = isTrafficVisible, onMapReady = onMapReady, onMapTap = onMapTap)
                Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(0.8f)) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = onSearchQueryChange, placeholder = { Text("Куда едем?") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            else if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, null) }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                    )
                    if (searchResults.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(searchResults) { geoObj ->
                                    Column(modifier = Modifier.fillMaxWidth().clickable { geoObj.geometry.firstOrNull()?.point?.let { onSearchResultClick(it, geoObj.name ?: "") } }.padding(12.dp)) {
                                        Text(geoObj.name ?: "Без названия", fontSize = 16.sp)
                                        if (!geoObj.descriptionText.isNullOrBlank()) Text(geoObj.descriptionText!!, fontSize = 12.sp)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(onClick = onTrafficToggle, modifier = Modifier.size(48.dp), containerColor = if (isTrafficVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface) { Icon(Icons.Default.Traffic, null) }
                    FloatingActionButton(onClick = onMyLocationClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.MyLocation, null) }
                    FloatingActionButton(onClick = onZoomIn, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, null) }
                    FloatingActionButton(onClick = onZoomOut, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Remove, null) }
                    if (destinationPoint != null || tappedPoint != null) {
                        FloatingActionButton(onClick = onClear, modifier = Modifier.size(48.dp), contentColor = MaterialTheme.colorScheme.error) { Icon(Icons.Default.Close, null) }
                    }
                }
                tappedPoint?.let { pt -> RouteActionCard("Ехать сюда", onAction = { onBuildRoute(pt) }, onCancel = onCancelRoute) }
                routeInfo?.let { info -> RouteInfoCard(info, destinationPoint, onHideInfo) }
                Row(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSwapClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.SwapHoriz, "Swap", modifier = Modifier.size(24.dp))
                    }
                    Button(onClick = onRatioClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Splitscreen, "Ratio", modifier = Modifier.size(24.dp).rotate(90f))
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.RouteActionCard(buttonText: String, onAction: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onAction) { Text(buttonText) }
            TextButton(onClick = onCancel) { Text("Отмена") }
        }
    }
}

@Composable
fun BoxScope.RouteInfoCard(info: String, destPoint: Point?, onHide: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(info, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            if (destPoint != null) {
                Button(onClick = {
                    val yandexNaviIntent = Intent(Intent.ACTION_VIEW, Uri.parse("yandexnavi://build_route_on_map?lat_to=${destPoint.latitude}&lon_to=${destPoint.longitude}"))
                    yandexNaviIntent.setPackage("ru.yandex.yandexnavi")
                    try { context.startActivity(yandexNaviIntent) } catch (e: Exception) { Toast.makeText(context, "Навигатор не найден", Toast.LENGTH_SHORT).show() }
                    onHide()
                }) { Icon(Icons.Default.PlayArrow, null); Spacer(modifier = Modifier.width(8.dp)); Text("В путь") }
            }
        }
    }
}

@Composable
fun MapPlaceholder(onNavigateToSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Map, "Map", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Карта не настроена", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}