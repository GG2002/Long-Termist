package com.cyc.yearlymemoir.utils

import com.cyc.yearlymemoir.domain.model.UniversalDateType
import com.cyc.yearlymemoir.domain.model.UniversalMDDateType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class FieldType {
    TEXT, NUMBER
}

@Serializable
data class Field(
    val name: String,
    val type: FieldType
)

@Serializable
data class UserDefinedStruct(
    val name: String,
    val fields: List<Field>
) {
    // 生成建表语句
    fun genCreateStatement(): String {
        val columnDefs = fields.joinToString(",\n") { field ->
            val sqlType = when (field.type) {
                FieldType.TEXT -> "TEXT"
                FieldType.NUMBER -> "REAL"
            }
            "`${field.name}` $sqlType"
        }

        return "CREATE TABLE IF NOT EXISTS `${name}` (" +
                "$columnDefs,\n" +
                "`date_type` CHAR(2),\n" +
                "`month` INTEGER,\n" +
                "`day` INTEGER,\n" +
                "`week_order` INTEGER,\n" +
                "`weekday` INTEGER\n" +
                ");"
    }

    fun genDeleteStatement(): String {
        return "DROP TABLE IF EXISTS $name;"
    }

    // 生成插入语句
    fun genInsertStatement(values: Map<String, Any?>, udate: UniversalMDDateType): String {
        // 1. 检查每个字段是否存在，并做类型校验
        val validEntries = values.map { (fieldName, value) ->
            val field = fields.find { it.name == fieldName }
                ?: throw IllegalArgumentException("字段 '$fieldName' 未在结构体中定义")

            // 2. 类型检查
            when (field.type) {
                FieldType.TEXT -> if (value !is String? && value != null) throw IllegalArgumentException(
                    "字段 '${field.name}' 是 TEXT 类型，但提供了非字符串值"
                )

                FieldType.NUMBER -> if (value !is Number? && value != null) throw IllegalArgumentException(
                    "字段 '${field.name}' 是 NUMBER 类型，但提供了非数值类型"
                )
            }

            fieldName to value
        }

        // 3. 构建 SQL 语句
        val columns = validEntries.map { it.first }
            .joinToString(
                prefix = "(",
                separator = ", ", postfix = ", date_type, month, day, week_order, weekday)"
            )
        val placeholders =
            validEntries.map { it.second }.joinToString(
                prefix = "(", separator = ", ", postfix = when (udate) {
                    is UniversalMDDateType.MonthDay -> ", '${UniversalDateType.MonthDay}', ${udate.month}, ${udate.day}, 0, 0)"
                    is UniversalMDDateType.MonthWeekday -> ", '${UniversalDateType.MonthWeekday}', ${udate.month}, 0, ${udate.weekOrder}, ${udate.weekday})"
                    is UniversalMDDateType.LunarDate -> ", '${UniversalDateType.LunarDate}', ${udate.month}, ${udate.day}, 0, 0)"
                }
            )


        return "INSERT INTO $name $columns VALUES $placeholders;"
    }

    // 序列化自己为 JSON 字符串
    fun serialize(): String {
        return Json.encodeToString(this)
    }

    companion object {
        // 从 JSON 字符串反序列化
        fun deserialize(json: String): UserDefinedStruct {
            return Json.decodeFromString(json)
        }
    }
}
