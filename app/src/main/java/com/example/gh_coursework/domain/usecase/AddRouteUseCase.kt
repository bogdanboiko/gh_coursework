package com.example.gh_coursework.domain.usecase

import com.example.gh_coursework.domain.entity.RouteDomain

interface AddRouteUseCase {
    suspend fun invoke(route: RouteDomain)
}