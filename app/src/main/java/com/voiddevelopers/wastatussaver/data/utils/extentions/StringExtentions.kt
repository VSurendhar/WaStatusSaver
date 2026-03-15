package com.voiddevelopers.wastatussaver.data.utils.extentions

fun String?.valueOrDefault(): String {
    return this ?: ""
}