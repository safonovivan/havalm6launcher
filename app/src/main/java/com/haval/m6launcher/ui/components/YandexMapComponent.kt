package com.haval.m6launcher.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.traffic.TrafficLayer
import com.yandex.mapkit.map.InputListener
import java.lang.ref.WeakReference

@Composable
fun YandexMapComponent(
    modifier: Modifier = Modifier,
    isNightMode: Boolean,
    isTrafficVisible: Boolean,
    onMapReady: (com.yandex.mapkit.map.Map) -> Unit,
    onMapTap: (Point) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var trafficLayer by remember { mutableStateOf<TrafficLayer?>(null) }

    // ИСПОЛЬЗУЕМ InputListener ВМЕСТО MapObjectTapListener
    val inputListenerRef: WeakReference<InputListener> = remember {
        val listener: InputListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                onMapTap(point)
            }

            override fun onMapLongTap(map: Map, point: Point) {
                // Можно оставить пустым, если долгий тап не нужен
            }
        }
        WeakReference(listener)
    }

    LaunchedEffect(Unit) {
        try {
            val view = MapView(context)

            var startPoint: Point? = null
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    startPoint = Point(lastKnown.latitude, lastKnown.longitude)
                }
            }

            if (startPoint != null) {
                view.mapWindow.map.move(CameraPosition(startPoint, 15.0f, 0.0f, 0.0f))
            }

            view.mapWindow.map.isNightModeEnabled = isNightMode

            // Создаём слой пробок
            val layer = MapKitFactory.getInstance().createTrafficLayer(view.mapWindow)
            layer.setTrafficVisible(isTrafficVisible)
            trafficLayer = layer

            // Включаем слой локации
            val userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(view.mapWindow)
            userLocationLayer.isVisible = true

            // ДОБАВЛЯЕМ СЛУШАТЕЛЬ НАЖАТИЙ НА КАРТУ
            view.mapWindow.map.addInputListener(inputListenerRef)

            mapView = view
            onMapReady(view.mapWindow.map)
        } catch (e: Exception) {
            errorText = "Ошибка карты: ${e.localizedMessage}"
        }
    }

    LaunchedEffect(isNightMode, isTrafficVisible) {
        mapView?.mapWindow?.map?.isNightModeEnabled = isNightMode
        trafficLayer?.setTrafficVisible(isTrafficVisible)
    }

    if (mapView != null) {
        DisposableEffect(lifecycleOwner) {
            val lifecycle = lifecycleOwner.lifecycle
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView?.onStart()
                    Lifecycle.Event.ON_STOP -> mapView?.onStop()
                    else -> {}
                }
            }
            lifecycle.addObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                mapView?.onStart()
            }
            onDispose {
                lifecycle.removeObserver(observer)
                mapView?.onStop()
            }
        }

        AndroidView(factory = { mapView!! }, modifier = modifier)
    } else if (errorText != null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorText!!, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
        }
    }
}