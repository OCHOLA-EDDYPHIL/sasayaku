package com.example.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationService : Service() {

    private lateinit var mDbRef: DatabaseReference
    private var currentUserUid: String? = null

    override fun onCreate() {
        super.onCreate()
        mDbRef = TubongeDb.getDatabase().getReference()
        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        startForegroundService()
        listenForNewMessages()
    }

    private fun startForegroundService() {
        val channelId = "notification_service_channel"
        val channelName = "Notification Service Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notification Service")
            .setContentText("Listening for new messages...")
            .setSmallIcon(R.drawable.message_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun listenForNewMessages() {
        currentUserUid?.let { uid ->
            mDbRef.child("chats").orderByChild("receiverId").equalTo(uid)
                .addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val message = snapshot.getValue(Message::class.java)
                        message?.id = snapshot.key
                        if (message?.status == MessageStatus.SENT) {
                            message.status = MessageStatus.DELIVERED
                            mDbRef.child("chats").child(message.id!!).child("status").setValue(MessageStatus.DELIVERED)
                            triggerNotification(message)
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NotificationService", "Failed to listen for new messages", error.toException())
                    }
                })
        }
    }

    private fun triggerNotification(message: Message) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("uid", message.senderId)
            putExtra("name", message.senderName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "chat_notifications")
            .setSmallIcon(R.drawable.message_foreground)
            .setContentTitle("New Message from ${message.senderName}")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chat_notifications",
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}