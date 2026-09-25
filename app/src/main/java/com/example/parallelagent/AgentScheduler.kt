package com.example.parallelagent

enum class ScheduleDecision {
    EXECUTE,
    DEFER,
    ASK_APPROVAL
}

data class ScheduleResult(
    val decision: ScheduleDecision,
    val reason: String
)

object AgentScheduler {

    fun schedule(
        tool: AgentTool,
        foregroundOccupiedByHuman: Boolean
    ): ScheduleResult {

        // 第一优先级：外部副作用必须得到用户确认
        if (tool.hasExternalSideEffect) {
            return ScheduleResult(
                decision = ScheduleDecision.ASK_APPROVAL,
                reason = "External side effect requires approval"
            )
        }

        // 第二优先级：工具需要前台，但前台正在被用户使用
        if (
            tool.resource == AgentResource.FOREGROUND_UI &&
            foregroundOccupiedByHuman
        ) {
            return ScheduleResult(
                decision = ScheduleDecision.DEFER,
                reason = "Foreground is currently owned by the user"
            )
        }

        // 没有资源冲突，也没有高风险副作用
        return ScheduleResult(
            decision = ScheduleDecision.EXECUTE,
            reason = "Safe to execute now"
        )
    }
}