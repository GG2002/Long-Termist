package com.cyc.yearlymemoir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cyc.yearlymemoir.domain.model.Field

/**
 * 代表数据库中 'fields' 表的实体。
 * 这里定义了与 'groups' 表的外键关系。
 */
@Entity(
    tableName = "fields",
    indices = [Index(value = ["group_id"])] // 为外键创建索引以提高查询性能
)
data class FieldEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "field_id")
    val fieldId: Int = 0,

    @ColumnInfo(name = "field_name")
    val fieldName: String,

    @ColumnInfo(name = "field_type")
    val fieldType: String,

    @ColumnInfo(name = "group_id")
    val groupId: Int
)

fun FieldEntity.toDomainModel(): Field {
    return Field(
        fieldId = this.fieldId,
        fieldName = this.fieldName,
        fieldType = this.fieldType,
        groupId = this.groupId
    )
}

fun Field.toEntity(): FieldEntity {
    return FieldEntity(
        fieldId = this.fieldId,
        fieldName = this.fieldName,
        fieldType = this.fieldType,
        groupId = this.groupId
    )
}