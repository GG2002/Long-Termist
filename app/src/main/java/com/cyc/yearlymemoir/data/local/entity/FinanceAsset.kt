package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资产记录：每天每个资产只允许一条记录
 */
@Entity(
    tableName = "finance_asset",
    indices = [Index(value = ["asset_name", "record_date"], unique = true)]
)
data class FinanceAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val asset_name: String,
    val asset_amount: Double,
    /** 记录日期，格式 yyyy-MM-dd */
    val record_date: String
)