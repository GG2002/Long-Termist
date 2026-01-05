package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.YearlyDetail

@Entity(tableName = "details_yearly", primaryKeys = ["md_date", "field_id"])
data class YearlyDetailEntity(
    val md_date: String,
    val field_id: Int,
    val detail: String
)

fun YearlyDetailEntity.toDomainModel(): YearlyDetail {
    return YearlyDetail(
        mdDate = UniversalDate.parse(1, this.md_date)!!,
        fieldId = this.field_id,
        detail = this.detail
    )
}

fun YearlyDetail.toEntity(): YearlyDetailEntity {
    return YearlyDetailEntity(
        md_date = this.mdDate.getRawMDDate().toString(),
        field_id = this.fieldId,
        detail = this.detail
    )
}