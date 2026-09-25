package com.example.parallelagent

import android.app.Service
import android.content.Intent
import android.graphics.Color

import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build

class OverlayService : Service() {
    private lateinit var speechController: SpeechController
    private val agentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RecordingService.ACTION_RECORDING_STARTED -> {
                    island.text = "🎙 Recording..."
                }

                RecordingService.ACTION_RECORDING_FINISHED -> {
                    island.text = "✓ Audio captured"
                }

                RecordingService.ACTION_RECORDING_FAILED -> {
                    island.text = "⚠ Recording failed"
                }
                RecordingService.ACTION_TRANSCRIBING -> {
                    island.text = "◌ Understanding..."
                }

                RecordingService.ACTION_TRANSCRIPTION_RESULT -> {

                    val text =
                        intent.getStringExtra("text")
                            ?: "No result"

                    island.text = "✓ $text"
                }

                RecordingService.ACTION_TRANSCRIPTION_ERROR -> {

                    val error =
                        intent.getStringExtra("error")
                            ?: "Unknown error"

                    island.text = "⚠ $error"
                }
                RecordingService.ACTION_PLANNING -> {
                    island.text = "◌ Planning..."
                }

                RecordingService.ACTION_PLAN_RESULT -> {

                    val plan =
                        intent.getStringExtra("plan")
                            ?: "No plan"

                    island.text = "◆ $plan"
                }

                RecordingService.ACTION_PLAN_ERROR -> {

                    val error =
                        intent.getStringExtra("error")
                            ?: "Planner error"

                    island.text = "⚠ $error"
                }
                RecordingService.ACTION_TOOL_RUNNING -> {

                    val tool =
                        intent.getStringExtra("tool")
                            ?: ""

                    island.text =
                        "⚙ Checking $tool..."
                }


                RecordingService.ACTION_TOOL_RESULT -> {

                    val result =
                        intent.getStringExtra("result")
                            ?: "Done"

                    island.text =
                        "✓ $result"
                }

                RecordingService.ACTION_TASK_DEFERRED -> {

                    val foreground =
                        intent.getStringExtra("foreground")
                            ?: "unknown"

                    val appName =
                        when (foreground) {
                            "com.xingin.xhs" -> "Xiaohongshu"
                            "com.tencent.mm" -> "WeChat"
                            else -> foreground
                        }

                    island.text =
                        "⏸ Waiting · $appName is in use"
                }


                RecordingService.ACTION_APPROVAL_REQUIRED -> {

                    island.text =
                        "⚠ Approval required"
                }

                RecordingService.ACTION_NO_TOOL -> {
                    island.text = "◇ Agent · Capability unavailable"
                }

                AgentAccessibilityService.ACTION_TRIGGER -> {

                    val foregroundPackage =
                        intent.getStringExtra("foreground_package")
                            ?: "unknown"

                    island.text = "📱 $foregroundPackage"

                    val recordingIntent =
                        Intent(this@OverlayService, RecordingService::class.java)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(recordingIntent)
                    } else {
                        startService(recordingIntent)
                    }
                }

                AgentAccessibilityService.ACTION_ABORT -> {
                    speechController.stop()
                    island.text = "■ Agent · Aborted"
                }
                AgentAccessibilityService.ACTION_APPROVE -> {

                    val pendingAction = AgentState.pendingAction

                    if (pendingAction != null) {

                        // Prototype: approval is recorded,
                        // but the real WeChat GUI executor is not implemented yet.
                        island.text = "✓ Approved · Queued"

                        AgentState.clearApproval()

                    } else {

                        island.text = "⚠ No pending action"
                    }
                }


                AgentAccessibilityService.ACTION_REJECT -> {

                    // State has already been cleared by AccessibilityService.
                    island.text = "✕ Rejected"
                }
            }
        }
    }
    private lateinit var windowManager: WindowManager
    private lateinit var island: TextView

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        island = TextView(this).apply {
            text = "● Agent · Working"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(40, 18, 40, 18)

            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = 60f
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        windowManager.addView(island, params)
        speechController = SpeechController(
            this,
            onStatus = { status ->
                island.text = status
            },
            onResult = { result ->
                island.text = "✓ $result"
            }
        )
        val filter = IntentFilter().apply {

            addAction(AgentAccessibilityService.ACTION_TRIGGER)
            addAction(AgentAccessibilityService.ACTION_ABORT)
            addAction(AgentAccessibilityService.ACTION_APPROVE)
            addAction(AgentAccessibilityService.ACTION_REJECT)
            addAction(RecordingService.ACTION_RECORDING_STARTED)
            addAction(RecordingService.ACTION_RECORDING_FINISHED)
            addAction(RecordingService.ACTION_RECORDING_FAILED)
            addAction(RecordingService.ACTION_TRANSCRIBING)
            addAction(RecordingService.ACTION_TRANSCRIPTION_RESULT)
            addAction(RecordingService.ACTION_TRANSCRIPTION_ERROR)
            addAction(RecordingService.ACTION_PLANNING)
            addAction(RecordingService.ACTION_PLAN_RESULT)
            addAction(RecordingService.ACTION_PLAN_ERROR)
            addAction(RecordingService.ACTION_TOOL_RUNNING)
            addAction(RecordingService.ACTION_TOOL_RESULT)
            addAction(RecordingService.ACTION_TASK_DEFERRED)
            addAction(RecordingService.ACTION_APPROVAL_REQUIRED)
            addAction(RecordingService.ACTION_NO_TOOL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                agentReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(agentReceiver, filter)
        }

        //Handler(Looper.getMainLooper()).postDelayed({
        //    island.text = "✓ Agent · Done"
        //}, 5000)
    }

    override fun onDestroy() {
        if (::speechController.isInitialized) {
            speechController.stop()
        }
        unregisterReceiver(agentReceiver)
        super.onDestroy()
        if (::island.isInitialized) {
            windowManager.removeView(island)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}