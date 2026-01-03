package com.cyc.yearlymemoir.data.tidbcloud.dao

import com.cyc.yearlymemoir.data.local.entity.FieldEntity
import com.cyc.yearlymemoir.data.tidbcloud.db.ApiResponse
import com.cyc.yearlymemoir.data.tidbcloud.db.DbClient
import com.cyc.yearlymemoir.data.tidbcloud.dto.FieldRequest

class FieldDao(client: DbClient) : BaseDao(client) {

    fun insertField(field: FieldEntity) {}

    fun getFieldByName(name: String): FieldRequest? {
        val request = mapOf("field_name" to name)

        val resp = client.post(
            endpoint = "fields/getFieldByName",
            requestBody = request,
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(FieldRequest.serializer()),
        )
        val field = resp.data?.rows?.get(0)
        println(field)
        return field
    }

    fun getFieldsByGroupId(groupId: Int): List<FieldRequest> {
        return listOf()
    }

    fun getAllFields(): List<FieldRequest> {
        val resp = client.post(
            endpoint = "fields/getAllFields",
            requestBody = "",
            requestSerializer = kotlinx.serialization.serializer(),
            responseSerializer = ApiResponse.serializer(FieldRequest.serializer()),
        )
        return resp.data?.rows ?: listOf()
    }

    fun deleteAll() {}
}