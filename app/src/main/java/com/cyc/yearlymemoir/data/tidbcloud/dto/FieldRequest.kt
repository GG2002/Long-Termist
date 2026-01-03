package com.cyc.yearlymemoir.data.tidbcloud.dto

import com.cyc.yearlymemoir.domain.model.Field
import kotlinx.serialization.Serializable

@Serializable
data class FieldRequest(
    val field_id: Int = 0,
    val field_name: String,
    val field_type: String,
    val group_id: Int
)

fun FieldRequest.toDomainModel(): Field {
    return Field(
        fieldId = this.field_id,
        fieldName = this.field_name,
        fieldType = this.field_type,
        groupId = this.group_id
    )
}

fun Field.toRequest(groupId: Int): FieldRequest {
    return FieldRequest(
        field_id = this.fieldId,
        field_name = this.fieldName,
        field_type = this.fieldType,
        group_id = groupId
    )
}