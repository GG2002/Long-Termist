package com.cyc.yearlymemoir.domain.model


data class Field(
    val fieldId: Int, val fieldName: String, val fieldType: String, val groupId: Int
)

const val FIELD_TYPE_NUM = "num"
const val FIELD_TYPE_STR = "str"
const val FIELD_TYPE_TEXT = "text"