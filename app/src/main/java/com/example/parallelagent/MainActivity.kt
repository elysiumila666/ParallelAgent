package com.example.parallelagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this).apply {
            text = "Start Agent"

            setOnClickListener {

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        100
                    )
                    return@setOnClickListener
                }

                startAgent()
            }
        }

        setContentView(button)
    }

    private fun startAgent() {

        if (!Settings.canDrawOverlays(this)) {

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )

            startActivity(intent)

        } else {

            startService(
                Intent(this, OverlayService::class.java)
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (
            Settings.canDrawOverlays(this) &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startService(Intent(this, OverlayService::class.java))
        }
    }
}