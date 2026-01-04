package com.voidDeveloper.wastatussaver.domain.usecases

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject


class TelegramLogUseCase @Inject constructor(private val client: OkHttpClient) {

    private val BASE_URL = "https://api.telegram.org/bot"

    suspend fun sendLogs(logs: String) {
        val token = com.voidDeveloper.wastatussaver.BuildConfig.BOT_TOKEN
        val chatId = com.voidDeveloper.wastatussaver.BuildConfig.CHAT_ID
        try {

            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", logs)
                put("parse_mode", "MarkdownV2")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL$token/sendMessage")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                } else {
                    val errorBody = response.body?.string()
                }
            }

        } catch (e: IOException) {

        } catch (e: Exception) {

        }
    }

}