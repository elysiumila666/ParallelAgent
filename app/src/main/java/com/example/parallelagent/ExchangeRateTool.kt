package com.example.parallelagent

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ExchangeRateTool {

    fun execute(
        from: String,
        to: String,
        onSuccess: (ExchangeRateResult) -> Unit,
        onError: (String) -> Unit
    ) {

        Thread {
            try {

                val base = from.lowercase()
                val quote = to.lowercase()

                val url = URL(
                    "https://api.frankfurter.dev/v2/rate/$base/$quote"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode

                val responseText =
                    if (responseCode in 200..299) {
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
                        "Exchange rate $responseCode: $responseText"
                    )
                    return@Thread
                }

                val json = JSONObject(responseText)

                val result = ExchangeRateResult(
                    base = json.getString("base"),
                    quote = json.getString("quote"),
                    rate = json.getDouble("rate"),
                    date = json.getString("date")
                )

                onSuccess(result)

            } catch (e: Exception) {

                onError(
                    e.message ?: "Exchange rate error"
                )
            }
        }.start()
    }
}


data class ExchangeRateResult(
    val base: String,
    val quote: String,
    val rate: Double,
    val date: String
)