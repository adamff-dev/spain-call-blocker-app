package com.addev.listaspam.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.addev.listaspam.util.SpamUtils

/**
 * BroadcastReceiver for handling incoming call events and checking for spam numbers.
 * Requires Android P (API level 28) or higher.
 *
 * Note: This class will not work on Android Q (API level 29) or higher due to changes in privacy permissions.
 * For Android Q or higher, use MyCallScreeningService instead.
 */
class MyCallReceiver : BroadcastReceiver() {

    companion object {
        private const val SPAM_PREFS = "SPAM_PREFS"
        private const val BLOCK_NUMBERS_KEY = "BLOCK_NUMBERS"
    }

    private val spamUtils = SpamUtils()

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast.
     * @param context Context for accessing resources.
     * @param intent The received Intent.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.PHONE_STATE") {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber =
                intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                handleIncomingCall(context, incomingNumber)
            }
        }
    }

    /**
     * Handles the incoming call by checking if the number is spam.
     * @param context Context for accessing resources.
     * @param incomingNumber The incoming phone number.
     */
    private fun handleIncomingCall(context: Context, incomingNumber: String) {
        val sharedPreferences = context.getSharedPreferences(SPAM_PREFS, Context.MODE_PRIVATE)
        val blockedNumbers = sharedPreferences.getStringSet(BLOCK_NUMBERS_KEY, null)

        // End call if the number is already blocked
        if (blockedNumbers?.contains(incomingNumber) == true) {
            endCall(context)
            return
        }

        spamUtils.checkSpamNumber(context, incomingNumber) { isSpam ->
            if (isSpam) {
                endCall(context)
            }
        }
    }

    /**
     * Ends the incoming call.
     * @param context Context for accessing resources.
     */
    @SuppressLint("MissingPermission")
    private fun endCall(context: Context) {
        val telMgr = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telMgr.endCall()
    }
}
