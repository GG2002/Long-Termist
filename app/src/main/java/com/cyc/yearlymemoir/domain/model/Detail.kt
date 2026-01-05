package com.cyc.yearlymemoir.domain.model


data class Detail(
    val year: Int,
    val mdDate: UniversalDate,
    val fieldId: Int,
    val detail: String
)

data class YearlyDetail(
    val mdDate: UniversalDate,
    val fieldId: Int,
    val detail: String
)