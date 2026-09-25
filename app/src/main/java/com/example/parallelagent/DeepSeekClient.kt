package com.example.parallelagent

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DeepSeekClient {

    fun plan(
        userText: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {

                // ① 定义 Agent 当前拥有的工具
                val exchangeRateTool = JSONObject().apply {
                    put("type", "function")

                    put(
                        "function",
                        JSONObject().apply {

                            put("name", "exchange_rate")

                            put(
                                "description",
                                "Get the latest exchange rate between two currencies."
                            )

                            put(
                                "parameters",
                                JSONObject().apply {

                                    put("type", "object")

                                    put(
                                        "properties",
                                        JSONObject().apply {

                                            put(
                                                "from",
                                                JSONObject().apply {
                                                    put("type", "string")
                                                    put(
                                                        "description",
                                                        "Base currency ISO code, e.g. CNY"
                                                    )
                                                }
                                            )

                                            put(
                                                "to",
                                                JSONObject().apply {
                                                    put("type", "string")
                                                    put(
                                                        "description",
                                                        "Target currency ISO code, e.g. AUD"
                                                    )
                                                }
                                            )
                                        }
                                    )

                                    put(
                                        "required",
                                        JSONArray().apply {
                                            put("from")
                                            put("to")
                                        }
                                    )
                                }
                            )
                        }
                    )
                }


                // ② 以后所有工具都放进这个 Tool Registry
                val tools = JSONArray().apply {
                    put(exchangeRateTool)
                }


                // ③ Planner 的系统规则
                val systemPrompt = """
                    You are the planner of a parallel Android phone agent.

                    The human always owns the foreground screen.

                    You may use the provided tools when needed.

                    If a user's request can be completed using an available
                    background tool, call that tool.

                    Do not invent tool results.

                    For now, use exchange_rate whenever the user asks for
                    a currency exchange rate.
                """.trimIndent()


                val messages = JSONArray().apply {

                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )

                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", userText)
                        }
                    )
                }


                // ④ 注意：现在真正把 tools 交给 DeepSeek
                val body = JSONObject().apply {

                    put("model", "deepseek-chat")
                    put("messages", messages)

                    put("tools", tools)

                    // 让模型自己决定是否调用工具
                    put("tool_choice", "auto")

                    put("temperature", 0)
                    put("stream", false)
                }


                val connection =
                    URL("https://api.deepseek.com/chat/completions")
                        .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer ${BuildConfig.DEEPSEEK_API_KEY}"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.doOutput = true

                connection.outputStream.use {
                    it.write(
                        body.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }


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
                        "DeepSeek $responseCode: $responseText"
                    )

                    return@Thread
                }


                // ⑤ 读取 DeepSeek 的原生 tool_calls
                val json = JSONObject(responseText)

                val message = json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")


                if (
                    message.has("tool_calls") &&
                    message.getJSONArray("tool_calls").length() > 0
                ) {

                    val toolCall =
                        message
                            .getJSONArray("tool_calls")
                            .getJSONObject(0)

                    val function =
                        toolCall.getJSONObject("function")

                    val toolName =
                        function.getString("name")

                    val arguments =
                        function.getString("arguments")


                    // 暂时把真正的 Tool Call 传给下一层
                    val result = JSONObject().apply {

                        put("type", "tool_call")

                        put(
                            "tool_call_id",
                            toolCall.getString("id")
                        )

                        put(
                            "tool",
                            toolName
                        )

                        put(
                            "arguments",
                            JSONObject(arguments)
                        )
                    }

                    onSuccess(result.toString())

                } else {

                    // DeepSeek 判断不需要工具
                    val content =
                        message.optString(
                            "content",
                            "No tool selected"
                        )

                    onSuccess(content)
                }


            } catch (e: Exception) {

                onError(
                    e.message ?: "DeepSeek error"
                )
            }

        }.start()
    }
}
