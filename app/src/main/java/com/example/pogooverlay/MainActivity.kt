package com.example.pogooverlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.pm.PackageManager
import android.Manifest
import android.text.TextUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPermission = findViewById<Button>(R.id.btn_permission)
        val btnEnableService = findViewById<Button>(R.id.btn_enable_service)
        val btnOpenSettings = findViewById<Button>(R.id.btn_open_settings)
        val btnDisableService = findViewById<Button>(R.id.btn_disable_service)

        btnPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnEnableService.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                enableAccessibilityService()
            } else {
                Toast.makeText(this, "Permission required! Run: adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS", Toast.LENGTH_LONG).show()
            }
        }

        btnOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnDisableService.setOnClickListener {
            val intent = Intent("com.example.pogooverlay.ACTION_DISABLE_SERVICE")
            sendBroadcast(intent)
            
            // Update UI immediately
            btnDisableService.visibility = View.GONE
            btnEnableService.visibility = View.VISIBLE
            Toast.makeText(this, "Service Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableAccessibilityService() {
        val componentName = "$packageName/${OverlayAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        
        if (!enabledServices.contains(componentName)) {
            val newEnabledServices = if (enabledServices.isEmpty()) componentName else "$enabledServices:$componentName"
            Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newEnabledServices)
            Settings.Secure.putString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
            Toast.makeText(this, "Service Enabled", Toast.LENGTH_SHORT).show()
            
            // Update UI
            findViewById<Button>(R.id.btn_enable_service).visibility = View.GONE
            findViewById<Button>(R.id.btn_disable_service).visibility = View.VISIBLE
        } else {
             Toast.makeText(this, "Service already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val btnEnableService = findViewById<Button>(R.id.btn_enable_service)
        val btnDisableService = findViewById<Button>(R.id.btn_disable_service)

        // Check if service is running using the static flag we added
        // Check if service is enabled in system settings
        val componentName = "$packageName/${OverlayAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val isEnabled = enabledServices.contains(componentName)

        if (isEnabled) {
            btnEnableService.visibility = View.GONE
            btnDisableService.visibility = View.VISIBLE
        } else {
            btnEnableService.visibility = View.VISIBLE
            btnDisableService.visibility = View.GONE
        }
    }
}
