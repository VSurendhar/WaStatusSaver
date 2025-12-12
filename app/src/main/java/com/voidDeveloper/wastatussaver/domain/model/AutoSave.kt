package com.voidDeveloper.wastatussaver.domain.model

data class AutoSave(
    val autoSaveMediaMimeType: List<String>,
    val isAutoSaveEnable: Boolean,
    val interval: Int,
)