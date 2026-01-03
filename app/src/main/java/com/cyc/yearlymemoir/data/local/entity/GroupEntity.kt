package com.cyc.yearlymemoir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyc.yearlymemoir.domain.model.Group

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "group_id")
    val groupId: Int = 0, // 设为0让Room自动生成ID

    @ColumnInfo(name = "group_name")
    val groupName: String,

    @ColumnInfo(name = "group_parent_id")
    val groupParentId: Int
)

fun GroupEntity.toDomainModel(): Group {
    return Group(
        groupId = this.groupId,
        groupName = this.groupName,
        groupParentId = this.groupParentId
    )
}

fun Group.toEntity(): GroupEntity {
    return GroupEntity(
        groupId = this.groupId,
        groupName = this.groupName,
        groupParentId = this.groupParentId
    )
}