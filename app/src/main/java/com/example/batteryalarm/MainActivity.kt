package com.example.batteryalarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var targetPercentage = 100

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Izin notifikasi diperlukan agar pemantau berjalan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        val tvTarget = TextView(this).apply {
            text = "Batas Peringatan Baterai: $targetPercentage%"
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val seekBar = SeekBar(this).apply {
            max = 100
            progress = targetPercentage
            setPadding(0, 40, 0, 40)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    targetPercentage = if (progress < 10) 10 else progress
                    tvTarget.text = "Batas Peringatan Baterai: $targetPercentage%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val btnStart = Button(this).apply {
            text = "Aktifkan Pemantau Baterai"
            setOnClickListener {
                if (checkAndRequestPermissions()) {
                    startBatteryService()
                }
            }
        }

        val btnStop = Button(this).apply {
            text = "Matikan Pemantau"
            setOnClickListener {
                stopService(Intent(this@MainActivity, BatteryMonitorService::class.java))
                Toast.makeText(this@MainActivity, "Pemantau baterai dihentikan", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(tvTarget)
        layout.addView(seekBar)
        layout.addView(btnStart)
        layout.addView(btnStop)
        setContentView(layout)
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return false
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Aktifkan izin 'Muncul di atas aplikasi lain'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return false
        }

        return true
    }

    private fun startBatteryService() {
        val intent = Intent(this, BatteryMonitorService::class.java).apply {
            putExtra("TARGET_PERCENTAGE", targetPercentage)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Pemantau aktif untuk batas $targetPercentage%", Toast.LENGTH_SHORT).show()
    }
}
