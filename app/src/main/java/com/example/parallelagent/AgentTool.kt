package com.example.parallelagent

enum class AgentResource {
    NETWORK,
    OS_BACKGROUND,
    FOREGROUND_UI
}

data class AgentTool(
    val name: String,
    val resource: AgentResource,
    val hasExternalSideEffect: Boolean,
    val description: String
)

object ToolRegistry {

    private val tools = mapOf(

        "exchange_rate" to AgentTool(
            name = "exchange_rate",
            resource = AgentResource.NETWORK,
            hasExternalSideEffect = false,
            description = "Get the latest exchange rate between two currencies"
        ),

        "xiaohongshu_gui" to AgentTool(
            name = "xiaohongshu_gui",
            resource = AgentResource.FOREGROUND_UI,
            hasExternalSideEffect = false,
            description = "Interact with Xiaohongshu UI to find information"
        ),

        "wechat_send_file" to AgentTool(
            name = "wechat_send_file",
            resource = AgentResource.FOREGROUND_UI,
            hasExternalSideEffect = true,
            description = "Send a local file to a WeChat contact"
        )
    )

    fun get(name: String): AgentTool? {
        return tools[name]
    }
}
