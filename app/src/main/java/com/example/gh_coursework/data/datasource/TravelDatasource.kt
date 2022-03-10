package com.example.gh_coursework.data.datasource

import com.example.gh_coursework.domain.entity.PointDetailsDomain
import com.example.gh_coursework.domain.entity.PointPreviewDomain
import com.example.gh_coursework.domain.entity.RouteDomain
import com.example.gh_coursework.domain.entity.RoutePointPreviewDomain
import kotlinx.coroutines.flow.Flow

interface TravelDatasource {
    interface Local {
        suspend fun addOrUpdatePointOfInterestDetails(poi: PointDetailsDomain)
        suspend fun addPointOfInterestCoordinates(poi: PointPreviewDomain)
        suspend fun addRoute(route: RouteDomain)
        suspend fun addRouteDetails(route: RouteDomain)
        suspend fun deletePoint(pointId: Int)
        fun getPointOfInterestPreview(): Flow<List<PointPreviewDomain>>
        fun getRoutePreview(routeId: Int): Flow<List<RoutePointPreviewDomain>>
        fun getPointOfInterestDetails(id: Int): Flow<PointDetailsDomain?>
        fun getRouteDetails(routeId: Int): Flow<RouteDomain>
    }

    interface Remote
}