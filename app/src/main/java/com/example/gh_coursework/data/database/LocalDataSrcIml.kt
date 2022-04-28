package com.example.gh_coursework.data.database

import android.os.Environment
import android.util.Log
import com.example.gh_coursework.data.database.dao.*
import com.example.gh_coursework.data.database.entity.PointCoordinatesEntity
import com.example.gh_coursework.data.database.entity.PointDetailsEntity
import com.example.gh_coursework.data.database.entity.RoutePointEntity
import com.example.gh_coursework.data.database.mapper.images.mapPointImageDomainToEntity
import com.example.gh_coursework.data.database.mapper.images.mapPointImageEntityToDomain
import com.example.gh_coursework.data.database.mapper.images.mapRouteImageDomainToEntity
import com.example.gh_coursework.data.database.mapper.images.mapRouteImageEntityToDomain
import com.example.gh_coursework.data.database.mapper.point_details.mapPointDetailsDomainToEntity
import com.example.gh_coursework.data.database.mapper.point_details.mapPointDetailsEntityToDomain
import com.example.gh_coursework.data.database.mapper.point_details.mapPointDetailsEntityToPointCompleteDetailsDomain
import com.example.gh_coursework.data.database.mapper.point_preview.mapPointDomainToEntity
import com.example.gh_coursework.data.database.mapper.point_preview.mapPointEntityToDomain
import com.example.gh_coursework.data.database.mapper.point_tag.mapPointTagEntityToDomain
import com.example.gh_coursework.data.database.mapper.point_tag.mapPointsTagsDomainToEntity
import com.example.gh_coursework.data.database.mapper.point_tag.mapTagDomainToEntity
import com.example.gh_coursework.data.database.mapper.public.mapPublicRouteDomainToEntity
import com.example.gh_coursework.data.database.mapper.route_details.mapRoutePointsImagesResponseToDomain
import com.example.gh_coursework.data.database.mapper.route_preview.mapRouteDomainToEntity
import com.example.gh_coursework.data.database.mapper.route_preview.mapRoutePointEntityToDomain
import com.example.gh_coursework.data.database.mapper.route_preview.mapRouteResponseToDomain
import com.example.gh_coursework.data.database.mapper.route_tag.mapRouteTagEntityToDomain
import com.example.gh_coursework.data.database.mapper.route_tag.mapRouteTagsDomainToEntity
import com.example.gh_coursework.data.datasource.TravelDatasource
import com.example.gh_coursework.domain.entity.*
import com.example.gh_coursework.ui.helper.routeTags
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File


class LocalDataSrcIml(
    private val routeDao: RoutePreviewDao,
    private val pointDetailsDao: PointDetailsDao,
    private val tagDao: PointTagDao,
    private val imageDao: ImageDao,
    private val routeTagDao: RouteTagDao
) : TravelDatasource.Local {

    //PointPreview
    override suspend fun addPointOfInterestCoordinates(poi: PointPreviewDomain) {
        pointDetailsDao.insertPointCoordinatesAndCreateDetails(mapPointDomainToEntity(poi))
    }

    override fun getAllPointsDetails(): Flow<List<PointCompleteDetailsDomain>> {
        return pointDetailsDao.getAllPointsDetails()
            .map { it.map(::mapPointDetailsEntityToPointCompleteDetailsDomain) }
    }

    override suspend fun deleteAllPoints() {
        pointDetailsDao.deleteAllPoints()
    }

    override suspend fun deletePoint(pointId: String) {
        pointDetailsDao.deletePoint(pointId)
    }

    //PointDetails
    override suspend fun updatePointOfInterestDetails(poi: PointDetailsDomain) {
        pointDetailsDao.updatePointDetails(mapPointDetailsDomainToEntity(poi))
    }

    override fun makePrivateRoutePublic(routeId: String) {
        routeDao.makePrivateRoutePublic(routeId)
    }

    override suspend fun saveFirebaseRouteToLocal(
        route: PublicRouteDomain,
        points: List<PublicRoutePointDomain>
    ) {
        Log.e("e", "saving route $route")
        routeDao.insertRoute(mapPublicRouteDomainToEntity(route))
        imageDao.deleteAllRouteLocalStoredImages(route.routeId)
        addRouteImages(route.imageList.map { RouteImageDomain(route.routeId, it, true) })
        addRouteTagsList(route.tagsList.map {
            RouteTagsDomain(
                route.routeId,
                routeTags.indexOf(it).toLong() + 1
            )
        })

        val routePointList = mutableListOf<RoutePointEntity>()

        points.forEachIndexed { index, point ->
            pointDetailsDao.insertPointCoordinatesAndCreateDetails(
                PointCoordinatesEntity(
                    point.pointId,
                    point.x,
                    point.y,
                    point.isRoutePoint
                )
            )

            pointDetailsDao.updatePointDetails(PointDetailsEntity(point.pointId, point.caption, point.description))

            routePointList.add(RoutePointEntity(route.routeId, point.pointId, index))
            imageDao.deleteAllPointLocalStoredImages(point.pointId)
            addPointImages(point.imageList.map { PointImageDomain(point.pointId, it, true) })
        }

        routeDao.insertRoutePointsList(routePointList)
    }

    override suspend fun addPointImages(images: List<PointImageDomain>) {
        imageDao.addPointImages(images.map(::mapPointImageDomainToEntity))
    }

    override suspend fun deletePointImage(image: PointImageDomain) {
        imageDao.deletePointImage(mapPointImageDomainToEntity(image))
    }

    override fun getPointOfInterestDetails(id: String): Flow<PointDetailsDomain> {
        return pointDetailsDao.getPointDetails(id).map { mapPointDetailsEntityToDomain(it) }
    }

    override fun getPointImages(pointId: String): Flow<List<PointImageDomain>> {
        return imageDao.getPointImages(pointId).map { it.map(::mapPointImageEntityToDomain) }
    }

    //PointTag
    override suspend fun addPointTag(tag: PointTagDomain) {
        tagDao.addTag(mapTagDomainToEntity(tag))
    }

    override suspend fun addPointsTagsList(pointsTagsList: List<PointsTagsDomain>) {
        tagDao.addTagsToPoint(pointsTagsList.map(::mapPointsTagsDomainToEntity))
    }

    override suspend fun deletePointTag(tag: PointTagDomain) {
        tagDao.deleteTag(mapTagDomainToEntity(tag))
    }

    override fun getPointTagList(): Flow<List<PointTagDomain>> {
        return tagDao.getPointTags()
            .map { tagList -> tagList.map(::mapPointTagEntityToDomain) }
    }

    override fun getPointsTagsList(pointId: String): Flow<List<PointTagDomain>> {
        return getPointOfInterestDetails(pointId).map { it.tagList }
    }

    override suspend fun removePointsTagsList(pointsTagsList: List<PointsTagsDomain>) {
        tagDao.deleteTagsFromPoint(pointsTagsList.map(::mapPointsTagsDomainToEntity))
    }

    //RoutePreview
    override suspend fun addRoute(
        route: RouteDomain,
        coordinatesList: List<PointCoordinatesEntity>
    ) {
        val routePointEntitiesList = mutableListOf<RoutePointEntity>()
        var position = 0

        coordinatesList.forEach {
            addPointOfInterestCoordinates(mapPointEntityToDomain(it))
            routePointEntitiesList.add(
                RoutePointEntity(
                    route.routeId,
                    it.pointId,
                    position
                )
            )
            position++
        }

        routeDao.addRoute(mapRouteDomainToEntity(route), routePointEntitiesList)
    }

    override fun getRouteDetails(routeId: String): Flow<RouteDomain> {
        return routeDao.getRouteDetails(routeId).map(::mapRouteResponseToDomain)
    }

    @OptIn(FlowPreview::class)
    override fun getRoutesList(): Flow<List<RouteDomain>> {
        return routeDao.getRoutesResponse().map { it.map(::mapRouteResponseToDomain) }
    }

    override fun getPublicRoutesList(): Flow<List<RouteDomain>> {
        return routeDao.getPublicRoutesResponse().map{ it.map(::mapRouteResponseToDomain) }
    }

    override fun getRoutePointsList(routeId: String): Flow<List<RoutePointDomain>> {
        return routeDao.getRoutePoints(routeId).map { it.map(::mapRoutePointEntityToDomain) }
    }

    override suspend fun updateRoute(route: RouteDomain) {
        routeDao.updateRouteDetails(mapRouteDomainToEntity(route))
    }

    override suspend fun deleteAllRoutes() {
        routeDao.deleteAllRoutes()
    }

    override suspend fun deleteRoute(route: RouteDomain) {
        routeDao.deleteRoute(mapRouteDomainToEntity(route))
    }

    //RouteImage
    override suspend fun addRouteImages(images: List<RouteImageDomain>) {
        imageDao.addRouteImages(images.map(::mapRouteImageDomainToEntity))
    }

    override suspend fun deleteRouteImage(image: RouteImageDomain) {
        imageDao.deleteRouteImage(mapRouteImageDomainToEntity(image))
    }

    override fun getRouteImages(routeId: String): Flow<List<RouteImageDomain>> {
        return imageDao.getRouteImages(routeId).map { image ->
            image.map(::mapRouteImageEntityToDomain)
        }
    }

    override fun getRoutePointsImagesList(routeId: String): Flow<List<RoutePointsImagesDomain>> {
        return routeDao.getRoutePointsImages(routeId).map { image ->
            image.map(::mapRoutePointsImagesResponseToDomain)
        }
    }

    //RouteTag
    override suspend fun addRouteTagsList(routeTagsList: List<RouteTagsDomain>) {
        routeTagDao.addRouteTags(routeTagsList.map(::mapRouteTagsDomainToEntity))
    }

    override suspend fun deleteTagsFromRoute(routeTagsList: List<RouteTagsDomain>) {
        routeTagDao.deleteTagsFromRoute(routeTagsList.map(::mapRouteTagsDomainToEntity))
    }

    override fun getRouteTags(): Flow<List<RouteTagDomain>> {

        return routeTagDao.getTagsList()
            .map { it.map(::mapRouteTagEntityToDomain) }
    }
}