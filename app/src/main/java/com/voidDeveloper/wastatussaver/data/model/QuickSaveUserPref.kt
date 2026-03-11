package com.voidDeveloper.wastatussaver.data.model

import com.voidDeveloper.wastatussaver.domain.model.MediaType
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Title

data class QuickSaveUserPref(val enable: Boolean?, val mediaType: List<MediaType>? , val app : Title?)
