package com.cyc.yearlymemoir.data.repository

import com.cyc.yearlymemoir.data.tidbcloud.dao.DetailDao
import com.cyc.yearlymemoir.data.tidbcloud.dao.FieldDao
import com.cyc.yearlymemoir.data.tidbcloud.dto.toDomainModel
import com.cyc.yearlymemoir.data.tidbcloud.dto.toRequest
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.Group
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import com.cyc.yearlymemoir.domain.repository.YearlyMemoirRepository

class TidbCloudYearlyMemoirRepositoryImpl(
    private val detailDao: DetailDao,
    private val fieldDao: FieldDao
) : YearlyMemoirRepository {
    override suspend fun getAllDetails(): List<Detail> {
        return detailDao.getAllDetails().map { it.toDomainModel() }
    }

    override suspend fun getDetailByFieldAndUniversalDateAndYearly(
        fieldId: Int,
        universalDate: UniversalDate,
        yearly: Boolean
    ): Detail? {
        TODO("Not yet implemented")
    }

    override suspend fun getFirstDayFieldDataByName(
        name: String,
        period: TimePeriod,
        isAsc: Boolean
    ): Pair<List<Detail>, String> {
        val field = fieldDao.getFieldByName(name)?.toDomainModel()
            ?: throw Exception("Field not found: $name")

        val detailsEntity = when (period) {
            TimePeriod.DAY -> detailDao.getDayFieldDataById(field.fieldId)

            TimePeriod.MONTH -> detailDao.getFirstDayOfMonthDataById(field.fieldId)
            TimePeriod.YEAR -> detailDao.getFirstDayOfYearDataById(field.fieldId)
        }

        val detailsDomain = detailsEntity.map { it.toDomainModel() }

        val sortedDetails = if (isAsc) detailsDomain else detailsDomain.reversed()

        return Pair(sortedDetails, field.fieldType)
    }

    override suspend fun insertOrUpdateDetail(
        fieldName: String,
        detail: String,
        date: UniversalDate,
        yearly: Boolean
    ) {
        val (year, mdDate) = Pair(date.getRawYear(), date.getRawMDDate())
        println("1111111111111111111111111" + fieldName)
        val fieldId = fieldDao.getFieldByName(fieldName)!!.field_id
        println("222222222222222222222222222")
        val detailEntity = Detail(
            detailId = 0,
            year = year,
            mdDate = mdDate.toString(),
            yearly = yearly,
            fieldId = fieldId,
            detail = detail,
        ).toRequest()
        detailDao.upsertDetail(detailEntity)
        println("33333333333333333333333333333")
    }

    override suspend fun deleteDetail(detail: Detail) {
        TODO("Not yet implemented")
    }

    override suspend fun insertOrUpdateField(field: Field) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllFields(): List<Field> {
        return fieldDao.getAllFields().map { it.toDomainModel() }
    }

    override suspend fun getFieldByName(name: String): Field? {
        TODO("Not yet implemented")
    }

    override suspend fun getAllGroupsWithFields(): Map<Group, List<Field>> {
        TODO("Not yet implemented")
    }
}