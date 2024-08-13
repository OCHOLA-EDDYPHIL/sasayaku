package com.example.chat

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

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

        loadMessagesFromFirebase()

        sendButton.setOnClickListener {
            val message = messageBox.text.toString().trim()
            if (message.isNotEmpty()) {
                val messageObject = Message(
                    message, senderUid, System.currentTimeMillis(), status = MessageStatus.SENT
                )

                if (NetworkUtils.isNetworkAvailable(this)) {
                    mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                        .setValue(messageObject).addOnSuccessListener {
                            mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                                .setValue(messageObject)
                        }
                }
                messageBox.setText("")
            }
        }

        chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
                    val message = messageList.getOrNull(i) ?: continue
                    if (message.status != MessageStatus.READ && message.senderId != senderUid) {
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
                        val messageDate = SimpleDateFormat(
                            "yyyyMMdd", Locale.getDefault()
                        ).format(Date(message?.timestamp ?: 0))
                        if (lastDate != messageDate) {
                            if (message?.timestamp != null) {
                                messageList.add(Message(null, null, message.timestamp, true))
                            }
                            lastDate = messageDate
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