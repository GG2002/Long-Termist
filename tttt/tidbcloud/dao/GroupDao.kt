package com.cyc.yearlymemoir.data.tidbcloud.dao

import com.cyc.yearlymemoir.data.tidbcloud.db.DbClient
import com.cyc.yearlymemoir.domain.model.Group

class GroupDao(client: DbClient) : BaseDao(client) {

    fun insertGroup(group: Group) {}

    fun updateGroup(group: Group) {}

    fun deleteGroupById(groupId: Int) {}

    fun getAllGroups(): List<Group> {
        return listOf()
    }

    fun getGroupById(groupId: Int): Group? {
        return null
    }

    fun deleteAll() {}
}