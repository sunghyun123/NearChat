package com.example.nearchat

import android.app.NotificationChannel
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 메시지 데이터 처리
        remoteMessage.data.isNotEmpty().let {
            val message = remoteMessage.data["message"]
            val senderName = remoteMessage.data["senderName"]
            sendNotification(message, senderName)
        }
    }

    private fun sendNotification(message: String?, senderName: String?) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channelId = "chat_notifications"

        // Notification Channel 설정 (API 26 이상)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // 알림을 클릭하면 앱의 MainActivity로 이동
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE 플래그 추가
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("New Message from $senderName")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification) // 아이콘 설정
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(0, notification)
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM 새 토큰 처리, 서버에 토큰을 보내서 저장 가능
        Log.d("FCM", "New token: $token")
    }
}
