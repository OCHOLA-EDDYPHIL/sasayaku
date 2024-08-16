package com.example.chat

import android.app.IntentService
import android.content.Intent
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
                // Handle new messages efficiently
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}