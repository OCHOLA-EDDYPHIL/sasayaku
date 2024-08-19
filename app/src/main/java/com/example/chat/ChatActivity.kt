package com.example.chat

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var fabScrollToBottom: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference

    companion object {
        var receiverRoom: String? = null
        var senderRoom: String? = null
        var isInChat: Boolean = false
        const val DATE_FORMAT = "yyyyMMdd"
        const val TIME_FORMAT = "hh:mm a"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        val chatId = intent.getStringExtra("chatId")
        val senderName = intent.getStringExtra("name")
        val uid = intent.getStringExtra("uid")
        if (chatId != null) {
            NotificationUtils.cancelNotification(this, chatId)
        }

        NotificationUtils.createNotificationChannel(this)

        initializeFirebaseDatabaseReference(uid)
        setupToolbar(senderName)
        setupRecyclerView()
        setupFloatingActionButton()
        loadMessagesFromFirebase()
//        listenForNewMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun initializeFirebaseDatabaseReference(receiverUid: String?) {
        val senderUid = TubongeDb.getAuth().currentUser?.uid
        mDbRef = TubongeDb.getDatabase().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid
    }

    private fun setupToolbar(senderName: String?) {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = senderName

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
                    if (message.status != MessageStatus.READ && message.senderId != TubongeDb.getAuth().currentUser?.uid) {
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

    private fun setupFloatingActionButton() {
        fabScrollToBottom = findViewById(R.id.fabScrollToBottom)
        fabScrollToBottom.setOnClickListener {
            chatRecyclerView.scrollToPosition(messageList.size - 1)
        }
    }

    private fun sendMessage() {
        val message = messageBox.text.toString().trim()
        if (message.isNotEmpty()) {
            val senderUid = TubongeDb.getAuth().currentUser?.uid
            getCurrentUserName { senderName ->
                Log.d("sendMessage", "Sender UID: $senderUid, Sender Name: $senderName")
                val timestamp = System.currentTimeMillis()
                val messageId = mDbRef.push().key
                val messageObject = Message(
                    message,
                    senderUid,
                    senderName,
                    timestamp,
                    status = if (NetworkUtils.isNetworkAvailable(this)) {
                        MessageStatus.SENT
                    } else {
                        MessageStatus.WAITING
                    }
                )
                Log.d("sendMessage", "Message Object Sender Name: ${messageObject.senderName}")

                val updates = hashMapOf(
                    "/chats/$senderRoom/messages/$messageId" to messageObject,
                    "/chats/$receiverRoom/messages/$messageId" to messageObject,
                    "/user/$senderUid/lastMessageTimestamp" to timestamp
                )

                mDbRef.updateChildren(updates).addOnSuccessListener {
                    messageBox.setText(getString(R.string.nothing))
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Message cannot be blank", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentUserName(callback: (String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            val mDbRef = TubongeDb.getDatabase().getReference()
            mDbRef.child("user").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userName = snapshot.child("name").getValue(String::class.java)
                        Log.d("getCurrentUserName", "User Name: $userName")
                        callback(userName)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "getCurrentUserName",
                            "Failed to fetch user name",
                            error.toException()
                        )
                        callback(null)
                    }
                })
        } else {
            callback(null)
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

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@ChatActivity,
                            "Failed to update messages",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        if (NetworkUtils.isNetworkAvailable(this)) {
            updateWaitingMessages()
        }
        isInChat = true
    }

    override fun onPause() {
        super.onPause()
        isInChat = false
    }

    private fun loadMessagesFromFirebase() {
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    var lastDate: String? = null
                    var hasUnreadMessages = false
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        message?.id = postSnapshot.key
                        message?.status = message?.status ?: MessageStatus.SENT

                        val messageDate = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                            .format(Date(message?.timestamp ?: 0))
                        if (lastDate != messageDate) {
                            if (message?.timestamp != null) {
                                messageList.add(Message(null, null, null, message.timestamp, true))
                                lastDate = messageDate
                            }
                        }
                        if (!message?.message.isNullOrEmpty()) {
                            message?.let { messageList.add(it) }
                            if (message?.status != MessageStatus.READ) {
                                hasUnreadMessages = true
                            }
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    chatRecyclerView.scrollToPosition(messageList.size - 1)

                    // Update last message timestamp for receiver
                    val receiverUid = intent.getStringExtra("uid")
                    val lastMessageTimestamp = messageList.lastOrNull()?.timestamp
                    if (lastMessageTimestamp != null) {
                        mDbRef.child("user").child(receiverUid!!).child("lastMessageTimestamp")
                            .setValue(lastMessageTimestamp)
                    }

                    // Trigger notification
                    val senderName = intent.getStringExtra("name") ?: "Unknown"
                    val uid = intent.getStringExtra("uid")
                    val actualMessageCount = messageList.count { it.message != null }
                    NotificationUtils.showNotification(
                        this@ChatActivity,
                        senderName,
                        actualMessageCount,
                        senderRoom!!,
                        uid ?: return,
                        hasUnreadMessages
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT)
                        .show()
                }
            })

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
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    fun deleteMessage(message: Message) {
        val currentTime = System.currentTimeMillis()
        val thirtyMinutesInMillis = 30 * 60 * 1000

        if (currentTime - (message.timestamp ?: 0) <= thirtyMinutesInMillis) {
            // Delete for both sender and receiver
            mDbRef.child("chats").child(senderRoom!!).child("messages").child(message.id!!)
                .removeValue()
            mDbRef.child("chats").child(receiverRoom!!).child("messages").child(message.id!!)
                .removeValue()
        } else {
            // Delete only for sender
            mDbRef.child("chats").child(senderRoom!!).child("messages").child(message.id!!)
                .removeValue()
        }

        messageList.remove(message)
        messageAdapter.notifyDataSetChanged()
    }

    fun deleteWaitingMessage(message: Message) {
        if (message.status == MessageStatus.WAITING) {
            mDbRef.child("chats").child(senderRoom!!).child("messages").child(message.id!!)
                .removeValue()
            mDbRef.child("chats").child(receiverRoom!!).child("messages").child(message.id!!)
                .removeValue()
            messageList.remove(message)
            messageAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}