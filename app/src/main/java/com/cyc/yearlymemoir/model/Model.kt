package com.cyc.yearlymemoir.model

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class Model {
    companion object {
        lateinit var db: Database
    }

    init {
        // sqlite 连接
//        val db_path = "./yearlmemoir.db"
//        val jdbcUrl = "jdbc:sqlite:$db_path"
//        db = Database.connect(
//            url = jdbcUrl,
//            driver = "org.sqlite.JDBC"
//        )

        // mysql 连接
        val password = "u4TyIMn85Cr5b0VV"
        val jdbcUrl = "jdbc:mysql://3DfwRjLLHiPLUSo.root:" +
                password +
                "@gateway01.eu-central-1.prod.aws.tidbcloud.com:4000/personal?sslMode=VERIFY_IDENTITY&useInformationSchema=false"
        db = Database.connect(
            url = jdbcUrl,
            driver = "com.mysql.cj.jdbc.Driver"
        )
    }

    data class Detail(
        val detail_id: Int, val year: Int, val mdDate: String,
        val yearly: Boolean, val field_id: Int, val detail: String
    )

    object Details : Table("details") {
        val detail_id = integer("detail_id").autoIncrement()
        val year = integer("year")
        val mdDate = varchar("md_date", 10)
        val yearly = bool("yearly")
        val field_id = integer("field_id")
        val detail = text("detail")

        override val primaryKey = PrimaryKey(year, mdDate, field_id)
    }

    object Groups : Table("groups") {
        val groupId = integer("group_id").autoIncrement()
        val groupName = varchar("group_name", 255)
        val groupParentId = integer("group_parent_id")

        override val primaryKey = PrimaryKey(groupId)
    }

    object Fields : Table("fields") {
        val fieldId = integer("field_id").autoIncrement()
        val fieldName = varchar("field_name", 255)
        val fieldType = varchar("field_type", 50)

        override val primaryKey = PrimaryKey(fieldId)
    }
    
    fun test(): List<Detail> {
        val details = mutableListOf<Detail>()
        transaction {
            addLogger(StdOutSqlLogger)
            Details.selectAll().map { row ->
                details.add(
                    Detail(
                        detail_id = row[Details.detail_id],
                        year = row[Details.year],
                        mdDate = row[Details.mdDate],
                        yearly = row[Details.yearly],
                        field_id = row[Details.field_id],
                        detail = row[Details.detail],
                    )
                )
            }
        }
        return details
    }
}