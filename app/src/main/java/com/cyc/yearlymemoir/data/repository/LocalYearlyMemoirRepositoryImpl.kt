package com.cyc.yearlymemoir.data.repository

import com.cyc.yearlymemoir.data.local.dao.DetailDao
import com.cyc.yearlymemoir.data.local.dao.FieldDao
import com.cyc.yearlymemoir.data.local.entity.toDomainModel
import com.cyc.yearlymemoir.data.local.entity.toEntity
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.Group
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import com.cyc.yearlymemoir.domain.repository.YearlyMemoirRepository

class LocalYearlyMemoirRepository(
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
        val year = universalDate.getRawYear()
        val mdDate = universalDate.getRawMDDate()

        return detailDao.getDetailByFieldAndUniversalDateAndYearly(
            fieldId, year, mdDate.toString(), yearly
        )?.toDomainModel()
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

        // Room 不直接支持 DESC/ASC 参数，你可以在查询后手动反转列表
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
        val fieldId = fieldDao.getFieldByName(fieldName)!!.fieldId
        val detailEntity = Detail(
            detailId = 0,
            year = year,
            mdDate = mdDate.toString(),
            yearly = yearly,
            fieldId = fieldId,
            detail = detail,
        ).toEntity()
        detailDao.upsertDetail(detailEntity)
    }

    override suspend fun deleteDetail(detail: Detail) {
        TODO("Not yet implemented")
    }

    override suspend fun insertOrUpdateField(field: Field) {
        fieldDao.insertField(field.toEntity())
    }

    override suspend fun getAllFields(): List<Field> {
        return fieldDao.getAllFields().map { it.toDomainModel() }
    }


    override suspend fun getFieldByName(name: String): Field? {
        return fieldDao.getFieldByName(name)?.toDomainModel()
    }

    override suspend fun getAllGroupsWithFields(): Map<Group, List<Field>> {
        TODO("Not yet implemented")
    }
}