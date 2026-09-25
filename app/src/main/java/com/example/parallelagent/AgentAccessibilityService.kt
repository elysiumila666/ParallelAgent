package com.example.parallelagent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class AgentAccessibilityService : AccessibilityService() {

    private var lastVolumeUpTime = 0L
    private var lastVolumeDownTime = 0L

    companion object {
        const val ACTION_APPROVE =
            "com.example.parallelagent.APPROVE"

        const val ACTION_REJECT =
            "com.example.parallelagent.REJECT"
        const val ACTION_TRIGGER = "com.example.parallelagent.TRIGGER"
        const val ACTION_ABORT = "com.example.parallelagent.ABORT"
        private const val DOUBLE_CLICK_MS = 500L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {

        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        /*
         * Approval mode has priority.
         *
         * While an action is waiting for approval:
         * Volume+ = approve
         * Volume- = reject
         *
         * These are single clicks and will NOT be interpreted
         * as the normal double-click wake/abort gestures.
         */
        if (AgentState.state == AgentRuntimeState.WAITING_APPROVAL) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_VOLUME_UP -> {

                    val pendingAction = AgentState.pendingAction

                    if (pendingAction != null) {

                        sendBroadcast(
                            Intent(ACTION_APPROVE)
                                .setPackage(packageName)
                        )
                    }

                    return true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {

                    AgentState.clearApproval()

                    sendBroadcast(
                        Intent(ACTION_REJECT)
                            .setPackage(packageName)
                    )

                    return true
                }
            }
        }


        /*
         * Normal mode:
         * Volume++ = wake agent
         * Volume-- = abort
         */
        val now = System.currentTimeMillis()

        when (event.keyCode) {

            KeyEvent.KEYCODE_VOLUME_UP -> {

                if (now - lastVolumeUpTime < DOUBLE_CLICK_MS) {

                    lastVolumeUpTime = 0

                    AgentTaskContext.capture()

                    sendBroadcast(
                        Intent(ACTION_TRIGGER)
                            .setPackage(packageName)
                            .putExtra(
                                "foreground_package",
                                AgentTaskContext.foregroundPackageAtTrigger
                                    ?: "unknown"
                            )
                    )

                    return true
                }

                lastVolumeUpTime = now
            }


            KeyEvent.KEYCODE_VOLUME_DOWN -> {

                if (now - lastVolumeDownTime < DOUBLE_CLICK_MS) {

                    lastVolumeDownTime = 0

                    sendBroadcast(
                        Intent(ACTION_ABORT)
                            .setPackage(packageName)
                    )

                    return true
                }

                lastVolumeDownTime = now
            }
        }

        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val observedPackage =
            event.packageName?.toString() ?: return

        // System surfaces are not human-owned app foreground.
        val ignoredPackages = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.coloros.launcher",
            "com.oplus.launcher",
            packageName
        )

        if (observedPackage in ignoredPackages) {
            return
        }

        when (event.eventType) {

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {

                ForegroundState.update(observedPackage)
            }
        }
    }
    override fun onInterrupt() {}
}