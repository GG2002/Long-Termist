package com.cyc.yearlymemoir.ui.personalbanlance

import java.time.LocalDate

fun getTodayString(): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.now().format(formatter)
}