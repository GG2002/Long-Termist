package com.cyc.yearlymemoir.data.tidbcloud.dao

import com.cyc.yearlymemoir.data.tidbcloud.db.ApiResponse
import com.cyc.yearlymemoir.data.tidbcloud.db.DbClient
import com.cyc.yearlymemoir.data.tidbcloud.dto.DetailRequest

class DetailDao(client: DbClient) : BaseDao(client) {

    fun getAllDetails(): List<DetailRequest> {
        val resp = client.post(
            endpoint = "details/getAllDetails",
            requestBody = "",
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(DetailRequest.serializer()),
        )

        return resp.data?.rows ?: listOf()
    }

    // `upsert` 完美对应你的 insertOrUpdate
    fun upsertDetail(detail: DetailRequest) {
        client.post(
            endpoint = "details/upsertDetail",
            requestBody = detail,
            requestSerializer = DetailRequest.serializer(),
            responseSerializer = ApiResponse.serializer(DetailRequest.serializer()),
        )
    }

    // 查询每天的数据
    fun getDayFieldDataById(fieldId: Int): List<DetailRequest> {
        val request = mapOf("field_id" to fieldId)

        val resp = client.post(
            endpoint = "details/getEveryDayData",
            requestBody = request,
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(DetailRequest.serializer()),
        )
        return resp.data?.rows ?: listOf()
    }

    // 获取每月的第一天的数据。
    fun getFirstDayOfMonthDataById(fieldId: Int): List<DetailRequest> {
        val request = mapOf("field_id" to fieldId)

        val resp = client.post(
            endpoint = "details/getFirstDayOfMonthData",
            requestBody = request,
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(DetailRequest.serializer()),
        )
        return resp.data?.rows ?: listOf()
    }

    // 获取每年的第一天的数据。
    fun getFirstDayOfYearDataById(fieldId: Int): List<DetailRequest> {
        val request = mapOf("field_id" to fieldId)

        val resp = client.post(
            endpoint = "details/getFirstDayOfYearData",
            requestBody = request,
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(DetailRequest.serializer()),
        )
        return resp.data?.rows ?: listOf()
    }

    fun deleteAll() {
        client.post(
            endpoint = "details/deleteAll",
            requestBody = {},
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(DetailRequest.serializer()),
        )
    }
}