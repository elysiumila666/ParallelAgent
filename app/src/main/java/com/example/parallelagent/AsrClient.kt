package com.example.parallelagent

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AsrClient {

    fun transcribe(
        audioFile: File,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        Thread {

            try {

                val audioBytes = audioFile.readBytes()

                val base64Audio = Base64.encodeToString(
                    audioBytes,
                    Base64.NO_WRAP
                )

                val dataUri =
                    "data:audio/m4a;base64,$base64Audio"

                val contentItem = JSONObject().apply {
                    put("type", "input_audio")

                    put(
                        "input_audio",
                        JSONObject().apply {
                            put("data", dataUri)
                        }
                    )
                }

                val message = JSONObject().apply {
                    put("role", "user")

                    put(
                        "content",
                        JSONArray().apply {
                            put(contentItem)
                        }
                    )
                }

                val body = JSONObject().apply {

                    put(
                        "model",
                        "qwen-audio-3.1-asr-flash"
                    )

                    put(
                        "input",
                        JSONObject().apply {

                            put(
                                "messages",
                                JSONArray().apply {
                                    put(message)
                                }
                            )
                        }
                    )

                    // 关键：format 要放在 parameters 里面
                    put(
                        "parameters",
                        JSONObject().apply {
                            put("format", "m4a")
                        }
                    )
                }

                val url = URL(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer ${BuildConfig.DASHSCOPE_API_KEY}"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "X-DashScope-SSE",
                    "disable"
                )

                connection.doOutput = true

                connection.outputStream.use {
                    it.write(
                        body.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                val responseCode =
                    connection.responseCode

                val responseText = if (
                    responseCode in 200..299
                ) {

                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                } else {

                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "HTTP $responseCode"
                }

                if (responseCode !in 200..299) {

                    onError(
                        "ASR $responseCode: $responseText"
                    )

                    return@Thread
                }

                val json = JSONObject(responseText)

                val text = json
                    .getJSONObject("output")
                    .getJSONObject("output")
                    .getJSONObject("sentence")
                    .getString("text")

                onSuccess(text)

            } catch (e: Exception) {

                onError(
                    e.message ?: "Unknown ASR error"
                )
            }
        }.start()
    }
}