/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2020  Karl Schreiner, c4akarl@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.androidheads.vienna.escalero

import android.annotation.SuppressLint
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

//        Log.d(TAG, "1 onMessageReceived(), remoteMessage.from: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {

//            Log.d(TAG, "2 onMessageReceived(), remoteMessage.data: ${remoteMessage.data}")

            notifyMainActivity(remoteMessage)

        }

        // Check if message contains a notification payload.
//        if (remoteMessage.notification != null) {
//
////            Log.d(TAG, "3 onMessageReceived(), remoteMessage.notification!!.body!!: ${remoteMessage.notification!!.body!!}")
//
//        }

        remoteMessage.notification?.let {

//            Log.d(TAG, "4 onMessageReceived(), sendNotification(), it.body!!: ${it.body!!}")

//            sendNotification(it.body!!)

        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//
//       sendRegistrationToServer(token)
//
//    }

    private fun notifyMainActivity(remoteMessage: RemoteMessage) {
        val intent = Intent()
        intent.action = MainActivity.BROADCAST_PUSH_NOTIFICATION
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("actionId", remoteMessage.data["actionId"])
        intent.putExtra("matchId", remoteMessage.data["matchId"])
        intent.putExtra("fromUsername", remoteMessage.data["fromUsername"])
        intent.putExtra("fromUserId", remoteMessage.data["fromUserId"])
        intent.putExtra("fromGpsId", remoteMessage.data["fromGpsId"])
        intent.putExtra("toUsername", remoteMessage.data["toUsername"])
        intent.putExtra("toUserId", remoteMessage.data["toUserId"])
        intent.putExtra("toGpsId", remoteMessage.data["toGpsId"])
        intent.putExtra("timestamp", remoteMessage.data["timestamp"])
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

//    private fun sendRegistrationToServer(token: String?) {
//
//        Log.d(TAG, "sendRegistrationToServer(), token: $token")
//
//        to users/$userId/token  ???
//
//    }

    companion object {

        const val TAG = "FCMService"

    }
}