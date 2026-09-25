package com.example.parallelagent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.io.File

class RecordingService : Service() {
    private lateinit var outputFile: File

    companion object {
        const val ACTION_RECORDING_STARTED =
            "com.example.parallelagent.RECORDING_STARTED"

        const val ACTION_RECORDING_FINISHED =
            "com.example.parallelagent.RECORDING_FINISHED"

        const val ACTION_RECORDING_FAILED =
            "com.example.parallelagent.RECORDING_FAILED"
        const val ACTION_TRANSCRIBING =
            "com.example.parallelagent.TRANSCRIBING"

        const val ACTION_TRANSCRIPTION_RESULT =
            "com.example.parallelagent.TRANSCRIPTION_RESULT"

        const val ACTION_TRANSCRIPTION_ERROR =
            "com.example.parallelagent.TRANSCRIPTION_ERROR"
        const val ACTION_PLANNING =
            "com.example.parallelagent.PLANNING"

        const val ACTION_PLAN_RESULT =
            "com.example.parallelagent.PLAN_RESULT"

        const val ACTION_PLAN_ERROR =
            "com.example.parallelagent.PLAN_ERROR"
        const val ACTION_TOOL_RUNNING =
            "com.example.parallelagent.TOOL_RUNNING"

        const val ACTION_TOOL_RESULT =
            "com.example.parallelagent.TOOL_RESULT"
        private const val CHANNEL_ID = "agent_microphone"
    }

    private var recorder: MediaRecorder? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Parallel Agent")
            .setContentText("Listening…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1001, notification)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        startRecording()

        return START_NOT_STICKY
    }

    private fun startRecording() {

        try {

            outputFile = File(
                cacheDir,
                "agent_recording.m4a"
            )

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {

                setAudioSource(MediaRecorder.AudioSource.MIC)

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
                )

                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }

            sendBroadcast(
                Intent(ACTION_RECORDING_STARTED)
                    .setPackage(packageName)
            )

            handler.postDelayed({
                stopRecording()
            }, 5000)

        } catch (e: Exception) {

            sendBroadcast(
                Intent(ACTION_RECORDING_FAILED)
                    .setPackage(packageName)
            )

            stopSelf()
        }
    }

    private fun stopRecording() {

        try {
            recorder?.stop()
        } catch (e: Exception) {

            sendBroadcast(
                Intent(ACTION_RECORDING_FAILED)
                    .setPackage(packageName)
            )

            recorder?.release()
            recorder = null

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        recorder?.release()
        recorder = null

        // 告诉 Island：录音结束，开始理解
        sendBroadcast(
            Intent(ACTION_TRANSCRIBING)
                .setPackage(packageName)
        )

        AsrClient().transcribe(
            outputFile,

            onSuccess = { result ->

                // 先让 Island 显示用户刚才说了什么
                sendBroadcast(
                    Intent(ACTION_TRANSCRIPTION_RESULT)
                        .setPackage(packageName)
                        .putExtra("text", result)
                )

                // 再交给 DeepSeek Planner
                sendBroadcast(
                    Intent(ACTION_PLANNING)
                        .setPackage(packageName)
                )

                DeepSeekClient().plan(
                    result,

                    onSuccess = { plan ->

                        try {

                            val planJson = org.json.JSONObject(plan)

                            val type = planJson.optString("type")

                            val tool = planJson.optString("tool")

                            if (
                                type == "tool_call" &&
                                tool == "exchange_rate"
                            ) {

                                val arguments =
                                    planJson.getJSONObject("arguments")

                                val from =
                                    arguments.getString("from")

                                val to =
                                    arguments.getString("to")


                                // 告诉 Island：Agent 正在执行工具
                                sendBroadcast(
                                    Intent(ACTION_TOOL_RUNNING)
                                        .setPackage(packageName)
                                        .putExtra(
                                            "tool",
                                            "$from → $to"
                                        )
                                )


                                // 真正执行 ExchangeRateTool
                                ExchangeRateTool().execute(

                                    from = from,
                                    to = to,

                                    onSuccess = { result ->

                                        val displayText =
                                            "💱 1 ${result.base} = " +
                                                    "%.4f".format(result.rate) +
                                                    " ${result.quote}"

                                        sendBroadcast(
                                            Intent(ACTION_TOOL_RESULT)
                                                .setPackage(packageName)
                                                .putExtra(
                                                    "result",
                                                    displayText
                                                )
                                        )

                                        stopForeground(
                                            STOP_FOREGROUND_REMOVE
                                        )

                                        stopSelf()
                                    },

                                    onError = { error ->

                                        sendBroadcast(
                                            Intent(ACTION_PLAN_ERROR)
                                                .setPackage(packageName)
                                                .putExtra(
                                                    "error",
                                                    error
                                                )
                                        )

                                        stopForeground(
                                            STOP_FOREGROUND_REMOVE
                                        )

                                        stopSelf()
                                    }
                                )

                            } else {

                                // 没有匹配到可执行 Tool
                                sendBroadcast(
                                    Intent(ACTION_PLAN_RESULT)
                                        .setPackage(packageName)
                                        .putExtra(
                                            "plan",
                                            plan
                                        )
                                )

                                stopForeground(
                                    STOP_FOREGROUND_REMOVE
                                )

                                stopSelf()
                            }

                        } catch (e: Exception) {

                            sendBroadcast(
                                Intent(ACTION_PLAN_ERROR)
                                    .setPackage(packageName)
                                    .putExtra(
                                        "error",
                                        e.message ?: "Tool execution error"
                                    )
                            )

                            stopForeground(
                                STOP_FOREGROUND_REMOVE
                            )

                            stopSelf()
                        }
                    },
                    onError = { error ->

                        sendBroadcast(
                            Intent(ACTION_PLAN_ERROR)
                                .setPackage(packageName)
                                .putExtra("error", error)
                        )

                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                )
            },

            onError = { error ->

                sendBroadcast(
                    Intent(ACTION_TRANSCRIPTION_ERROR)
                        .setPackage(packageName)
                        .putExtra("error", error)
                )

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)

        recorder?.release()
        recorder = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Agent microphone",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
