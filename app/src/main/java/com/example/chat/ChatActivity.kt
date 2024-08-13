package com.example.chat

import android.os.Bundle
import android.util.Log
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
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference

    var receiverRoom: String? = null
    var senderRoom: String? = null

    companion object {
        const val DATE_FORMAT = "yyyyMMdd"
        const val TIME_FORMAT = "hh:mm a"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        initializeFirebaseDatabaseReference()
        setupToolbar()
        setupRecyclerView()
        loadMessagesFromFirebase()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun initializeFirebaseDatabaseReference() {
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid
    }

    private fun setupToolbar() {
        val receiverName = intent.getStringExtra("name")
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = receiverName

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sendButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
                    val message = messageList.getOrNull(i) ?: continue
                    if (message.status != MessageStatus.READ && message.senderId != FirebaseAuth.getInstance().currentUser?.uid) {
                        message.id?.let { messageId ->
                            message.status = MessageStatus.READ
                            mDbRef.child("chats").child(senderRoom!!).child("messages")
                                .child(messageId).child("status").setValue(MessageStatus.READ)
                            mDbRef.child("chats").child(receiverRoom!!).child("messages")
                                .child(messageId).child("status").setValue(MessageStatus.READ)
                        }
                    }
                }
            }
        })
    }

    private fun sendMessage() {
        val message = messageBox.text.toString().trim()
        if (message.isNotEmpty()) {
            val senderUid = FirebaseAuth.getInstance().currentUser?.uid
            val messageObject = Message(
                message,
                senderUid,
                System.currentTimeMillis(),
                status = if (NetworkUtils.isNetworkAvailable(this)) {
                    MessageStatus.SENT
                } else {
                    MessageStatus.WAITING
                }
            )

            mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                .setValue(messageObject).addOnSuccessListener {
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }
                }
            messageBox.setText("")
        }
    }

    private fun updateWaitingMessages() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            mDbRef.child("chats").child(senderRoom!!).child("messages")
                .orderByChild("status").equalTo(MessageStatus.WAITING.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (postSnapshot in snapshot.children) {
                            val message = postSnapshot.getValue(Message::class.java)
                            message?.id = postSnapshot.key
                            message?.status = MessageStatus.SENT
                            mDbRef.child("chats").child(senderRoom!!).child("messages")
                                .child(message?.id!!).child("status").setValue(MessageStatus.SENT)
                            mDbRef.child("chats").child(receiverRoom!!).child("messages")
                                .child(message.id!!).setValue(message)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    override fun onResume() {
        super.onResume()
        updateWaitingMessages()
    }

    private fun loadMessagesFromFirebase() {
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    var lastDate: String? = null
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        message?.id = postSnapshot.key
                        message?.status = message?.status ?: MessageStatus.SENT

                        // Log the message object to debug
                        Log.d("ChatActivity", "Message: $message.toString()")

                        val messageDate = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                            .format(Date(message?.timestamp ?: 0))
                        if (lastDate != messageDate) {
                            if (message?.timestamp != null) {
                                messageList.add(Message(null, null, message.timestamp, true))
                                lastDate = messageDate
                            }
                        }
                        if (!message?.message.isNullOrEmpty()) {
                            message?.let { messageList.add(it) }
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    // Scroll to the last message
                    chatRecyclerView.scrollToPosition(messageList.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        // Update message status to DELIVERED when received
        mDbRef.child("chats").child(receiverRoom!!).child("messages")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.id = snapshot.key
                    if (message?.status == MessageStatus.SENT) {
                        message.status = MessageStatus.DELIVERED
                        mDbRef.child("chats").child(receiverRoom!!).child("messages")
                            .child(message.id!!).child("status").setValue(MessageStatus.DELIVERED)
                        mDbRef.child("chats").child(senderRoom!!).child("messages")
                            .child(message.id!!).child("status").setValue(MessageStatus.DELIVERED)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}