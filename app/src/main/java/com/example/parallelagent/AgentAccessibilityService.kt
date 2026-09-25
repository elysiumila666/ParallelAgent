package com.example.parallelagent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class AgentAccessibilityService : AccessibilityService() {

    private var lastVolumeUpTime = 0L
    private var lastVolumeDownTime = 0L

    companion object {
        const val ACTION_TRIGGER = "com.example.parallelagent.TRIGGER"
        const val ACTION_ABORT = "com.example.parallelagent.ABORT"
        private const val DOUBLE_CLICK_MS = 500L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val now = System.currentTimeMillis()

        when (event.keyCode) {

            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (now - lastVolumeUpTime < DOUBLE_CLICK_MS) {
                    lastVolumeUpTime = 0

                    sendBroadcast(
                        Intent(ACTION_TRIGGER).setPackage(packageName)
                    )

                    // PoC阶段消费第二次按键
                    return true
                }

                lastVolumeUpTime = now
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (now - lastVolumeDownTime < DOUBLE_CLICK_MS) {
                    lastVolumeDownTime = 0

                    sendBroadcast(
                        Intent(ACTION_ABORT).setPackage(packageName)
                    )

                    return true
                }

                lastVolumeDownTime = now
            }
        }

        // 单击仍交还给系统处理音量
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}