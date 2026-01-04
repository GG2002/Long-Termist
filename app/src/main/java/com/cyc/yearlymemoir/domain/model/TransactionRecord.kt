package com.cyc.yearlymemoir.domain.model

/**
 * 领域模型：支出/收入记录
 */
data class TransactionRecord(
    val id: Int = 0,
    val amount: Double,
    val tag: String,
    val remark: String,
    val recordDate: String // yyyy-MM-dd
)
