package com.example.gh_coursework.ui.private_point.model

import com.example.gh_coursework.ui.point_details.model.PointTagModel

data class PrivatePointDetailsPreviewModel(
    val tagList: List<PointTagModel>,
    val caption: String,
    val description: String
)