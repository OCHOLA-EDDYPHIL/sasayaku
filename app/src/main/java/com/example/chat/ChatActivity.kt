package com.example.chat

import Message
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
        chatRecyclerView.edgeEffectFactory = SpringEdgeEffectFactory(this)

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

    private fun saveMessageToLocalDb(message: Message) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ChatDatabaseHelper.COLUMN_MESSAGE, message.message)
            put(ChatDatabaseHelper.COLUMN_SENDER_ID, message.senderId)
            put(ChatDatabaseHelper.COLUMN_TIMESTAMP, message.timestamp)
        }
        db.insert(ChatDatabaseHelper.TABLE_MESSAGES, null, values)
    }

    private fun loadMessagesFromLocalDb() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            ChatDatabaseHelper.TABLE_MESSAGES,
            null, null, null, null, null,
            "${ChatDatabaseHelper.COLUMN_TIMESTAMP} ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val message = Message(
                    getString(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_MESSAGE)),
                    getString(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_SENDER_ID)),
                    getLong(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_TIMESTAMP))
                )
                messageList.add(message)
            }
        }
        cursor.close()
        messageAdapter.notifyDataSetChanged()
    }

    private fun syncMessagesWithFirebase() {
        mDbRef.child("chats").child(senderRoom!!).child("messages").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (postSnapshot in snapshot.children) {
                    val message = postSnapshot.getValue(Message::class.java)
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
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}