package com.gaarx.tvplayer.domain.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        /*if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start your app's main activity
            val i = Intent(context, PlayerActivity::class.java)
            //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //context.startActivity(i)
        }*/
    }
}
