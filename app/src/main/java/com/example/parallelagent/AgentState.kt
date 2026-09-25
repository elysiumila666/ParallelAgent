package com.example.parallelagent

import org.json.JSONObject

enum class AgentRuntimeState {
    IDLE,
    WAITING_APPROVAL
}

data class PendingAction(
    val toolName: String,
    val arguments: JSONObject
)

object AgentState {

    @Volatile
    var state: AgentRuntimeState = AgentRuntimeState.IDLE
        private set

    @Volatile
    var pendingAction: PendingAction? = null
        private set

    fun waitForApproval(
        toolName: String,
        arguments: JSONObject
    ) {
        pendingAction = PendingAction(
            toolName = toolName,
            arguments = JSONObject(arguments.toString())
        )

        state = AgentRuntimeState.WAITING_APPROVAL
    }

    fun clearApproval() {
        pendingAction = null
        state = AgentRuntimeState.IDLE
    }
}