package com.cyc.yearlymemoir.domain.model

/**
 * 领域模型：每日余额记录
 */
data class BalanceRecord(
    val sourceType: String,
    val recordDate: String, // yyyy-MM-dd
    val balance: Double
)
