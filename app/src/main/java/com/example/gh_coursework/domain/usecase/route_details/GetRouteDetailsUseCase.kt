package com.example.gh_coursework.domain.usecase.route_details

import com.example.gh_coursework.domain.entity.RouteDomain
import kotlinx.coroutines.flow.Flow

interface GetRouteDetailsUseCase {
    fun invoke(routeId: Long): Flow<RouteDomain>
}