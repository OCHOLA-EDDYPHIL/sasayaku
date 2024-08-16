package com.example.chat

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.firebase.database.*

class MessageService : IntentService("MessageService") {

    private lateinit var mDbRef: DatabaseReference

    override fun onHandleIntent(intent: Intent?) {
        mDbRef = TubongeDb.getDatabase().getReference()
        loadMessagesInBackground()
    }

    private fun loadMessagesInBackground() {
        mDbRef.child("chats").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                message?.id = snapshot.key
                Log.d("MessageService", "New message added: ${message?.message}")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                val message = snapshot.getValue(Message::class.java)
//                message?.id = snapshot.key
//                Log.d("MessageService", "Message changed: ${message?.message}")
            }

            //
            override fun onChildRemoved(snapshot: DataSnapshot) {
//                val message = snapshot.getValue(Message::class.java)
//                message?.id = snapshot.key
//                Log.d("MessageService", "Message removed: ${message?.message}")
            }

            //
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                val message = snapshot.getValue(Message::class.java)
//                message?.id = snapshot.key
//                Log.d("MessageService", "Message moved: ${message?.message}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MessageService", "Failed to load messages: ${error.message}")
            }
        })
    }
}