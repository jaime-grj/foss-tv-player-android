package com.gaarj.iptvplayer.domain.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gaarj.iptvplayer.ui.view.HomeActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start your app's main activity
            val i = Intent(context, HomeActivity::class.java)
            //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //context.startActivity(i)
        }
    }
}
