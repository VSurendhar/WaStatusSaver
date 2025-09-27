package com.voidDeveloper.wastatussaver.data.utils.extentions

fun String?.valueOrDefault(): String {
    return this ?: ""
}