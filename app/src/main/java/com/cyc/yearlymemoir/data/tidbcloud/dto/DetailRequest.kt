package com.cyc.yearlymemoir.data.tidbcloud.dto

import com.cyc.yearlymemoir.domain.model.Detail
import kotlinx.serialization.Serializable

@Serializable
data class DetailRequest(
    val detail_id: Int = 0,
    val year: Int,
    val md_date: String,
    val yearly: String,
    val field_id: Int,
    val detail: String
)

fun DetailRequest.toDomainModel(): Detail = Detail(
    detailId = this.detail_id,
    year = this.year,
    mdDate = this.md_date,
    yearly = this.yearly != "0",
    fieldId = this.field_id,
    detail = this.detail
)

fun Detail.toRequest(): DetailRequest = DetailRequest(
    detail_id = this.detailId,
    year = this.year,
    md_date = this.mdDate,
    yearly = if (this.yearly) "1" else "0",
    field_id = this.fieldId,
    detail = this.detail
)