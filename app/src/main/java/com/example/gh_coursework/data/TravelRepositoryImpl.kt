package com.example.gh_coursework.data

import com.example.gh_coursework.data.database.mapper.route_preview.mapRoutePointDomainToEntity
import com.example.gh_coursework.data.datasource.TravelDatasource
import com.example.gh_coursework.domain.entity.*
import com.example.gh_coursework.domain.repository.TravelRepository
import kotlinx.coroutines.flow.Flow

class TravelRepositoryImpl(
    private val localDataSrcIml: TravelDatasource.Local,
    private val remoteDataSrcImpl: TravelDatasource.Remote
) : TravelRepository {

    //Deleted routes and points
    override suspend fun addDeletedPoint(point: DeletedPointDomain) {
        localDataSrcIml.addDeletedPoint(point)
    }

    override suspend fun addDeletedRoute(route: DeletedRouteDomain) {
        localDataSrcIml.addDeletedRoute(route)
    }

    override suspend fun clearDeletedPointsTable() {
        localDataSrcIml.clearDeletedPointsTable()
    }

    override suspend fun clearDeletedRoutesTable() {
        localDataSrcIml.clearDeletedRoutesTable()
    }

    override fun getDeletedPoints() = localDataSrcIml.getDeletedPoints()

    override fun getDeletedRoutes() = localDataSrcIml.getDeletedRoutes()

    override suspend fun deleteRemotePoint(pointId: String) {
        remoteDataSrcImpl.deletePoint(pointId)
    }

    override suspend fun deleteRemoteRoute(routeId: String) {
        remoteDataSrcImpl.deleteRoute(routeId)
    }

    //PointPreview
    override suspend fun addPointCoordinatesWithDetails(poi: PointPreviewDomain) {
        localDataSrcIml.addPointOfInterestCoordinates(poi)
    }

    override suspend fun deleteAllPoints() {
        localDataSrcIml.deleteAllPoints()
    }

    //PointDetails
    override suspend fun updatePointDetails(poi: PointDetailsDomain) {
        localDataSrcIml.updatePointOfInterestDetails(poi)
    }

    override fun getAllPointsDetails() = localDataSrcIml.getAllPointsDetails()

    override suspend fun saveFirebaseRouteToLocal(
        route: PublicRouteDomain,
        points: List<PublicRoutePointDomain>
    ) {
        localDataSrcIml.saveFirebaseRouteToLocal(route, points)
    }

    override fun getUserRoutes(userId: String) = remoteDataSrcImpl.getUserRoutes(userId)


    override suspend fun addPointImages(images: List<PointImageDomain>) {
        localDataSrcIml.addPointImages(images)
    }

    override suspend fun deletePointImage(image: PointImageDomain) {
        localDataSrcIml.deletePointImage(image)
    }

    override fun getPointOfInterestDetails(id: String) = localDataSrcIml.getPointOfInterestDetails(id)

    override fun getPointImages(pointId: String) = localDataSrcIml.getPointImages(pointId)

    //PointTag
    override suspend fun addPointTag(tag: PointTagDomain) {
        localDataSrcIml.addPointTag(tag)
    }

    override suspend fun addPointsTagsList(pointsTagsList: List<PointsTagsDomain>) {
        localDataSrcIml.addPointsTagsList(pointsTagsList)
    }

    override suspend fun deletePoint(pointId: String) {
        localDataSrcIml.deletePoint(pointId)
    }

    override suspend fun deletePointTag(tag: PointTagDomain) {
        localDataSrcIml.deletePointTag(tag)
    }

    override fun getPointTagList() = localDataSrcIml.getPointTagList()

    override fun getPointsTagsList(pointId: String) = localDataSrcIml.getPointsTagsList(pointId)

    override suspend fun removePointsTagsList(pointsTagsList: List<PointsTagsDomain>) {
        localDataSrcIml.removePointsTagsList(pointsTagsList)
    }

    //RoutePreview
    override suspend fun addRoute(route: RouteDomain, coordinatesList: List<PointDomain>) {
        localDataSrcIml.addRoute(route, coordinatesList.map(::mapRoutePointDomainToEntity))
    }

    override fun getRoutesList() = localDataSrcIml.getRoutesList()

    override fun getPublicRoutesList(): Flow<List<RouteDomain>> = localDataSrcIml.getPublicRoutesList()

    override suspend fun deleteAllRoutes() {
        localDataSrcIml.deleteAllRoutes()
    }

    override suspend fun deleteRoute(route: RouteDomain) {
        localDataSrcIml.deleteRoute(route)
    }

    //RouteDetails
    override fun getRouteDetails(routeId: String) = localDataSrcIml.getRouteDetails(routeId)

    override fun getRoutePointsList(routeId: String) = localDataSrcIml.getRoutePointsList(routeId)

    override suspend fun updateRoute(route: RouteDomain) {
        localDataSrcIml.updateRoute(route)
    }

    //RouteImage
    override suspend fun addRouteImages(images: List<RouteImageDomain>) {
        localDataSrcIml.addRouteImages(images)
    }

    override suspend fun deleteRouteImage(image: RouteImageDomain) {
        localDataSrcIml.deleteRouteImage(image)
    }

    override fun getRouteImages(routeId: String) = localDataSrcIml.getRouteImages(routeId)

    override fun getRoutePointsImagesList(routeId: String) =
        localDataSrcIml.getRoutePointsImagesList(routeId)

    //RouteTag
    override suspend fun addRouteTagsList(routeTagsList: List<RouteTagsDomain>) {
        localDataSrcIml.addRouteTagsList(routeTagsList)
    }

    override suspend fun deleteTagsFromRoute(routeTagsList: List<RouteTagsDomain>) {
        localDataSrcIml.deleteTagsFromRoute(routeTagsList)
    }

    override fun getRouteTags() = localDataSrcIml.getRouteTags()

    override suspend fun uploadRouteToFirebase(
        route: RouteDomain,
        routePoints: List<PointDomain>,
        currentUser: String
    ) {
        remoteDataSrcImpl.uploadRouteToFirebase(route, routePoints, currentUser)
    }

    override fun fetchRoutePoints(routeId: String): Flow<List<PublicRoutePointDomain>> {
        return remoteDataSrcImpl.fetchRoutePoints(routeId)
    }

    override suspend fun saveRouteImagesToFirebase(imageList: List<RouteImageDomain>, routeId: String) {
        remoteDataSrcImpl.saveRouteImages(imageList, routeId)
    }

    override suspend fun savePointImagesToFirebase(imageList: List<PointImageDomain>, pointId: String) {
        remoteDataSrcImpl.savePointImages(imageList, pointId)
    }

    override fun makePrivateRoutePublic(routeId: String) {
        remoteDataSrcImpl.makePrivateRoutePublic(routeId)
        localDataSrcIml.makePrivateRoutePublic(routeId)
    }
}