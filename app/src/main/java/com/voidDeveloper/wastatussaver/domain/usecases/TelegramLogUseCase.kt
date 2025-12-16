package com.voidDeveloper.wastatussaver.domain.usecases

import com.voidDeveloper.wastatussaver.domain.repo.main.MainRepo
import javax.inject.Inject


class TelegramLogUseCase @Inject constructor(private val repo: MainRepo) {

    suspend fun sendLogs(logs: String) {
        repo.sendLogsTelegram(logs)
    }

}