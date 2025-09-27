package com.voidDeveloper.wastatussaver.data.utils.extentions

import com.voidDeveloper.wastatussaver.R

fun Int?.valueOrEmptyString(): Int {
    return this ?: R.string.empty_string
}