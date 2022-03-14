package com.example.gh_coursework.data.database.mapper

import com.example.gh_coursework.data.database.response.PointDetailsResponse
import com.example.gh_coursework.domain.entity.PointDetailsDomain

fun mapPointDetailsEntityToDomain(details: PointDetailsResponse?): PointDetailsDomain? {
    return if (details != null) {
        with(details.pointDetails) {
            PointDetailsDomain(
                pointId,
                details.tagList.map(::mapPointTagEntityToDomain),
                caption,
                description
            )
        }
    } else {
        null
    }
}