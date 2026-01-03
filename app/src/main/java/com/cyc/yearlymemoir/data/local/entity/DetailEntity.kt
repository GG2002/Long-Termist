package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import com.cyc.yearlymemoir.domain.model.Detail

// 将通用模型转换为 Room 实体，添加注解
@Entity(tableName = "details", primaryKeys = ["year", "md_date", "field_id"])
data class DetailEntity(
    val detail_id: Int = 0,
    val year: Int,
    val md_date: String,
    val yearly: Boolean,
    val field_id: Int,
    val detail: String
)

// 创建转换函数，方便在 Entity 和通用 Model 之间切换
fun DetailEntity.toDomainModel(): Detail = Detail(
    detailId = this.detail_id,
    year = this.year,
    mdDate = this.md_date,
    yearly = this.yearly,
    fieldId = this.field_id,
    detail = this.detail
)

fun Detail.toEntity(): DetailEntity = DetailEntity(
    detail_id = this.detailId,
    year = this.year,
    md_date = this.mdDate,
    yearly = this.yearly,
    field_id = this.fieldId,
    detail = this.detail
)