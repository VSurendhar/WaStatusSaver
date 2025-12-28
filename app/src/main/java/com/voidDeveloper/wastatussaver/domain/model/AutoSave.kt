package com.voidDeveloper.wastatussaver.domain.model

data class AutoSave(
    val autoSaveMediaMimeType: List<String>? = null,
    var isAutoSaveEnable: Boolean? = null,
    var interval: Int? = null,
)