package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import com.cyc.yearlymemoir.domain.model.BalanceRecord

/**
 * 记录每天余额数字的表。
 * 使用字符串保存来源类型（建议与 BalanceChannelType 的 name 保持一致），日期使用 yyyy-MM-dd。
 */
@Entity(tableName = "balances", primaryKeys = ["source_type", "record_date"])
data class BalanceEntity(
    val source_type: String, // e.g. "ALIPAY", "WECHAT", "CUP"
    val record_date: String, // yyyy-MM-dd
    val balance: Double
)

fun BalanceEntity.toDomainModel(): BalanceRecord = BalanceRecord(
    sourceType = this.source_type,
    recordDate = this.record_date,
    balance = this.balance
)

fun BalanceRecord.toEntity(): BalanceEntity = BalanceEntity(
    source_type = this.sourceType,
    record_date = this.recordDate,
    balance = this.balance
)
