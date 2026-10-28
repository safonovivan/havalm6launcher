package com.haval.m6launcher.ui.components

import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingRouterType // <-- ДОБАВЛЕН ИМПОРТ
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.runtime.Error

object RouteManager {
    private val drivingRouter: DrivingRouter by lazy {
        // ИСПРАВЛЕНИЕ: передаем тип COMBINED
        DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
    }

    fun buildRoute(
        from: Point,
        to: Point,
        onSuccess: (DrivingRoute) -> Unit,
        onError: (Error) -> Unit
    ) {
        // Конструктор RequestPoint для Android SDK
        val points = listOf(
            RequestPoint(from, RequestPointType.WAYPOINT, null, null, null),
            RequestPoint(to, RequestPointType.WAYPOINT, null, null, null)
        )

        val drivingOptions = DrivingOptions().apply { routesCount = 1 }
        val vehicleOptions = VehicleOptions()

        val routeListener = object : DrivingSession.DrivingRouteListener {
            override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
                if (routes.isNotEmpty()) onSuccess(routes[0])
            }

            override fun onDrivingRoutesError(error: Error) {
                onError(error)
            }
        }

        drivingRouter.requestRoutes(points, drivingOptions, vehicleOptions, routeListener)
    }
}