package com.example.gh_coursework.ui.public_route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.gh_coursework.data.remote.FirestorePagingSource
import com.example.gh_coursework.domain.entity.PublicRoutePointDomain
import com.example.gh_coursework.domain.usecase.public.FetchRoutePointsUseCase
import com.example.gh_coursework.ui.public_route.mapper.mapRoutePointDomainToModel
import com.example.gh_coursework.ui.public_route.model.RoutePointModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class PublicRouteViewModel(
    private val query: Query,
    private val fetchRoutePointsUseCase: FetchRoutePointsUseCase
) : ViewModel() {
    val routeList = Pager(
            PagingConfig(pageSize = 10)
        ) {
            FirestorePagingSource(query)
        }.flow.cachedIn(viewModelScope)

    fun fetchRoutePoints(routeId: String): Flow<List<RoutePointModel>> {
        return fetchRoutePointsUseCase.invoke(routeId).map { it.map(::mapRoutePointDomainToModel) }
    }
}