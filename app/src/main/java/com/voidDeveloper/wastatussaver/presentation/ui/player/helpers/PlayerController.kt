package com.voidDeveloper.wastatussaver.presentation.ui.player.helpers

import androidx.media3.common.Player
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class PlayerController @Inject constructor(val player: Player)