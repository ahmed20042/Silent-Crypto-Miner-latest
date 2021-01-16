package com.amov.geoshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast

class SMSReceiver : BroadcastReceiver() {

    private val TAG = "GeoShape"

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "$TAG: SMS received", Toast.LENGTH_LONG).show()
        Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
    }
}