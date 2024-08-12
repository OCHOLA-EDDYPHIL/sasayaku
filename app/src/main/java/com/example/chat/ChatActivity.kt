package com.example.chat

import android.util.Log
import Message
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference
    private lateinit var dbHelper: ChatDatabaseHelper

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        dbHelper = ChatDatabaseHelper(this)

        val receiverName = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = receiverName

        toolbar.setNavigationOnClickListener {
            finish()
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sendButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        loadMessagesFromLocalDb()

        if (isNetworkAvailable()) {
            syncMessagesWithFirebase()
        }

        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            val messageObject = Message(message, senderUid, System.currentTimeMillis())

            if (isNetworkAvailable()) {
                mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }
            }
            saveMessageToLocalDb(messageObject)
            messageBox.setText("")
        }
    }

    private fun clearLocalDb() {
        val db = dbHelper.writableDatabase
        db.delete(ChatDatabaseHelper.TABLE_MESSAGES, null, null)
    }

    private fun saveMessageToLocalDb(message: Message) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ChatDatabaseHelper.COLUMN_MESSAGE, message.message)
            put(ChatDatabaseHelper.COLUMN_SENDER_ID, message.senderId)
            put(ChatDatabaseHelper.COLUMN_TIMESTAMP, message.timestamp)
            put(ChatDatabaseHelper.COLUMN_SENDER_ROOM, senderRoom)
            put(ChatDatabaseHelper.COLUMN_RECEIVER_ROOM, receiverRoom)
        }
        val result = db.insert(ChatDatabaseHelper.TABLE_MESSAGES, null, values)
        Log.d("ChatActivity", "Message saved to local DB: $result")
    }

    private fun loadMessagesFromLocalDb() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            ChatDatabaseHelper.TABLE_MESSAGES,
            null,
            "${ChatDatabaseHelper.COLUMN_SENDER_ROOM} = ? OR ${ChatDatabaseHelper.COLUMN_RECEIVER_ROOM} = ?",
            arrayOf(senderRoom, receiverRoom),
            null,
            null,
            "${ChatDatabaseHelper.COLUMN_TIMESTAMP} ASC"
        )

        val messageSet = mutableSetOf<String>()
        var lastDate: String? = null

        with(cursor) {
            while (moveToNext()) {
                val message = Message(
                    getString(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_MESSAGE)),
                    getString(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_SENDER_ID)),
                    getLong(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_TIMESTAMP))
                )
                val messageDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(
                    Date(
                        message.timestamp ?: 0
                    )
                )
                if (lastDate != messageDate) {
                    messageList.add(Message(messageDate, null, message.timestamp, true))
                    lastDate = messageDate
                }
                if (!messageSet.contains(message.timestamp.toString())) {
                    messageList.add(message)
                    messageSet.add(message.timestamp.toString())
                }
                Log.d("ChatActivity", "Message loaded from local DB: ${message.message}")
            }
        }
        cursor.close()
        messageAdapter.notifyDataSetChanged()
    }

    private fun syncMessagesWithFirebase() {
        clearLocalDb()
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    var lastDate: String? = null
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        val messageDate = SimpleDateFormat(
                            "yyyyMMdd",
                            Locale.getDefault()
                        ).format(Date(message?.timestamp ?: 0))
                        if (lastDate != messageDate) {
                            messageList.add(Message(messageDate, null, message?.timestamp, true))
                            lastDate = messageDate
                        }
                        messageList.add(message!!)
                        saveMessageToLocalDb(message)
                    }
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}