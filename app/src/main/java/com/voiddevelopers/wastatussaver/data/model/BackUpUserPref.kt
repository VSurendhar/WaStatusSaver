package com.voiddevelopers.wastatussaver.data.model

import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaType

data class BackUpUserPref(
    val interval: BackupInterval? = null,
    val selectedMediaTypes: List<MediaType>? = null,
    val allowBackUp: Boolean? = null,
    val canUseCellular: Boolean? = null,
)
