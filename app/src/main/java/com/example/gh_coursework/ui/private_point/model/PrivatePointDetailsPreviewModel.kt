package com.example.gh_coursework.ui.private_point.model

import com.example.gh_coursework.ui.point_details.model.PointTagModel
import com.example.gh_coursework.ui.model.ImageModel.PointImageModel

data class PrivatePointDetailsPreviewModel(
    val pointId: Long,
    val imageList: List<PointImageModel>,
    val tagList: List<PointTagModel>,
    val caption: String,
    val description: String
)