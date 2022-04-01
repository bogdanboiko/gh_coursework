package com.example.gh_coursework.domain.usecase.route_points

import com.example.gh_coursework.domain.entity.RoutePointDomain
import kotlinx.coroutines.flow.Flow

interface GetRoutePointsListUseCase {
    fun invoke(routeId: Long): Flow<List<RoutePointDomain>>
}