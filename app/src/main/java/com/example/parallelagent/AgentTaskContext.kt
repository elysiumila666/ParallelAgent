package com.example.parallelagent

object AgentTaskContext {

    @Volatile
    var foregroundPackageAtTrigger: String? = null
        private set

    @Volatile
    var triggeredAt: Long = 0L
        private set

    fun capture() {
        foregroundPackageAtTrigger =
            ForegroundState.currentPackage

        triggeredAt =
            System.currentTimeMillis()
    }

    fun clear() {
        foregroundPackageAtTrigger = null
        triggeredAt = 0L
    }
}