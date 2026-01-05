package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.UniversalDate

// 将通用模型转换为 Room 实体，添加注解
@Entity(tableName = "details", primaryKeys = ["year", "md_date", "field_id"])
data class DetailEntity(
    val year: Int,
    val md_date: String,
    val field_id: Int,
    val detail: String
)

// 创建转换函数，方便在 Entity 和通用 Model 之间切换
fun DetailEntity.toDomainModel(): Detail {
    val mdDate = UniversalDate.parse(this.year, this.md_date)!!
    return Detail(
    year = this.year,
    mdDate = mdDate,
    fieldId = this.field_id,
    detail = this.detail
    )
}

fun Detail.toEntity(): DetailEntity {
    return DetailEntity(
    year = this.year,
        md_date = this.mdDate.getRawMDDate().toString(),
        field_id = this.fieldId,
        detail = this.detail
    )
}