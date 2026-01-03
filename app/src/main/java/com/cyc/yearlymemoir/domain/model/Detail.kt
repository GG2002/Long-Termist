package com.cyc.yearlymemoir.domain.model


data class Detail(
    val detailId: Int, val year: Int, val mdDate: String,
    val yearly: Boolean, val fieldId: Int, val detail: String
)