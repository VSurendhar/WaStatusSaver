package com.voiddevelopers.wastatussaver.data.utils.extentions

import com.voiddevelopers.wastatussaver.R

fun Int?.valueOrEmptyString(): Int = this ?: R.string.empty_string
