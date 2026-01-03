package com.cyc.yearlymemoir.data.tidbcloud.dto

import com.cyc.yearlymemoir.domain.model.Group
import kotlinx.serialization.Serializable

@Serializable
data class GroupRequest(
    val groupId: Int = 0, // 设为 0 让 Room 自动生成 ID
    val groupName: String,
    val groupParentId: Int
)

fun GroupRequest.toDomainModel(): Group {
    return Group(
        groupId = this.groupId,
        groupName = this.groupName,
        groupParentId = this.groupParentId
    )
}

fun Group.toEntity(): GroupRequest {
    return GroupRequest(
        groupId = this.groupId,
        groupName = this.groupName,
        groupParentId = this.groupParentId
    )
}