package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyc.yearlymemoir.domain.model.TransactionRecord

/**
 * 记录具体支出/收入的表。
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val tag: String,
    val remark: String,
    val record_date: String // yyyy-MM-dd
)

fun TransactionEntity.toDomainModel(): TransactionRecord = TransactionRecord(
    id = this.id,
    amount = this.amount,
    tag = this.tag,
    remark = this.remark,
    recordDate = this.record_date
)

fun TransactionRecord.toEntity(): TransactionEntity = TransactionEntity(
    id = this.id,
    amount = this.amount,
    tag = this.tag,
    remark = this.remark,
    record_date = this.recordDate
)
