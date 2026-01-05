package com.cyc.yearlymemoir.data.tidbcloud.dto

import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.UniversalDate
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

fun DetailRequest.toDomainModel(): Detail  {
    val mdDate = UniversalDate.parse(this.year, this.md_date)!!
    return Detail(
        detailId = this.detail_id,
        mdDate = mdDate,
        yearly = this.yearly != "0",
        fieldId = this.field_id,
        detail = this.detail
    )
}

fun Detail.toRequest(): DetailRequest {
    return DetailRequest(
        detail_id = this.detailId,
        year = this.mdDate.getRawYear(),
        md_date = this.mdDate.getRawMDDate().toString(),
        yearly = if (this.yearly) "1" else "0",
        field_id = this.fieldId,
        detail = this.detail
    )
}