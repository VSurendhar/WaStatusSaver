package com.voiddevelopers.wastatussaver.data.model

import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.presentation.ui.main.ui.Title

data class QuickSaveUserPref(val enable: Boolean?, val mediaType: List<MediaType>? , val app : Title?)
