package com.cyc.yearlymemoir.domain.repository

import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.Group
import com.cyc.yearlymemoir.domain.model.UniversalDate

enum class TimePeriod {
    DAY,
    MONTH,
    YEAR
}

interface YearlyMemoirRepository {

    suspend fun getAllDetails(): List<Detail>

    suspend fun getDetailByFieldAndUniversalDateAndYearly(
        fieldId: Int,
        universalDate: UniversalDate,
        yearly: Boolean
    ): Detail?

    suspend fun getFirstDayFieldDataByName(
        name: String,
        period: TimePeriod,
        isAsc: Boolean = true
    ): Pair<List<Detail>, String> // 返回值可以封装成一个更明确的 Result 类

    suspend fun insertOrUpdateDetail(
        fieldName: String,
        detail: String,
        date: UniversalDate,
        yearly: Boolean
    )

    suspend fun deleteDetail(detail: Detail)

    suspend fun insertOrUpdateField(field: Field)

    suspend fun getAllFields(): List<Field>

    suspend fun getFieldByName(name: String): Field?

    suspend fun getAllGroupsWithFields(): Map<Group, List<Field>>
}