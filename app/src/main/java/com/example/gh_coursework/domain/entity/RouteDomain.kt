package com.example.gh_coursework.domain.entity

data class RouteDomain(
    val routeId: String,
    val name: String?,
    val description: String?,
    val tagsList: List<RouteTagDomain>,
    val imageList: List<RouteImageDomain>,
    val isPublic: Boolean
)
