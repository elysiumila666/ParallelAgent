package com.example.parallelagent

import org.json.JSONArray
import org.json.JSONObject

enum class AgentResource {
    NETWORK,
    OS_BACKGROUND,
    FOREGROUND_UI
}

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true
)

data class AgentTool(
    val name: String,
    val description: String,
    val resource: AgentResource,
    val hasExternalSideEffect: Boolean,
    val parameters: List<ToolParameter>
)

object ToolRegistry {

    private val tools = listOf(

        AgentTool(
            name = "exchange_rate",
            description =
                "Get the latest exchange rate between two currencies.",
            resource = AgentResource.NETWORK,
            hasExternalSideEffect = false,
            parameters = listOf(
                ToolParameter(
                    name = "from",
                    type = "string",
                    description = "Source currency ISO code, for example CNY"
                ),
                ToolParameter(
                    name = "to",
                    type = "string",
                    description = "Target currency ISO code, for example AUD"
                )
            )
        ),

        AgentTool(
            name = "xiaohongshu_gui",
            description =
                "Interact with Xiaohongshu when the task requires " +
                        "information from the user's Xiaohongshu account, " +
                        "browsing history, saved posts, searches, or posts.",
            resource = AgentResource.FOREGROUND_UI,
            hasExternalSideEffect = false,
            parameters = listOf(
                ToolParameter(
                    name = "task",
                    type = "string",
                    description = "Task to perform inside Xiaohongshu"
                )
            )
        ),

        AgentTool(
            name = "wechat_send_file",
            description =
                "Send a local file from the phone to a specific WeChat contact.",
            resource = AgentResource.FOREGROUND_UI,
            hasExternalSideEffect = true,
            parameters = listOf(
                ToolParameter(
                    name = "file",
                    type = "string",
                    description = "Name or description of the local file"
                ),
                ToolParameter(
                    name = "contact",
                    type = "string",
                    description = "WeChat contact name or remark"
                )
            )
        )
    )

    fun get(name: String): AgentTool? {
        return tools.find { it.name == name }
    }

    fun toDeepSeekTools(): JSONArray {

        val array = JSONArray()

        tools.forEach { tool ->

            val properties = JSONObject()
            val required = JSONArray()

            tool.parameters.forEach { parameter ->

                properties.put(
                    parameter.name,
                    JSONObject().apply {
                        put("type", parameter.type)
                        put(
                            "description",
                            parameter.description
                        )
                    }
                )

                if (parameter.required) {
                    required.put(parameter.name)
                }
            }

            val parametersSchema =
                JSONObject().apply {
                    put("type", "object")
                    put("properties", properties)
                    put("required", required)
                }

            val function =
                JSONObject().apply {
                    put("name", tool.name)
                    put(
                        "description",
                        tool.description
                    )
                    put(
                        "parameters",
                        parametersSchema
                    )
                }

            array.put(
                JSONObject().apply {
                    put("type", "function")
                    put("function", function)
                }
            )
        }

        return array
    }
}
