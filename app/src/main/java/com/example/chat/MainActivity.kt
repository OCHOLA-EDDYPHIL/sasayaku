package com.example.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request FCM token manually
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token: $token")
            } else {
                Log.e("FCM", "Fetching FCM token failed", task.exception)
            }
        }

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        auth = TubongeDb.getAuth()
        mDbRef = TubongeDb.getDatabase().getReference()

        userList = ArrayList()
        adapter = UserAdapter(this, userList)

        userRecyclerView = findViewById(R.id.userRecyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        loadUsersFromFirebase()
        listenForNewMessages()
    }

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshUserData()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL)
        }
    }

    companion object {
        private const val REFRESH_INTERVAL = 300000L // 5 minutes
    }

    private fun loadUsersFromFirebase() {
        progressBar.visibility = View.VISIBLE

        if (!NetworkUtils.isNetworkAvailable(this)) {
            AlertUtils.showAlert(this, "Error", "No internet connection.")
            return
        }
        mDbRef.child("user").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    userList.clear()
                    val totalUsers = snapshot.childrenCount
                    var processedUsers = 0

                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(User::class.java)
                        if (auth.currentUser?.uid != currentUser?.uid) {
                            userList.add(currentUser!!)
                        }
                        processedUsers++
                        if (processedUsers == totalUsers.toInt()) {
                            userList.sortByDescending { it.lastMessageTimestamp }
                            adapter.notifyDataSetChanged()
                            progressBar.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading users", e)
                    AlertUtils.showAlert(this@MainActivity, "Error", "Failed to load users.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Database error: ${error.message}", error.toException())
                AlertUtils.showAlert(this@MainActivity, "Error", "Failed to load users.")
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun refreshUserData() {
        progressBar.visibility = View.VISIBLE

        if (!NetworkUtils.isNetworkAvailable(this)) {
            AlertUtils.showAlert(this, "Error", "No internet connection.")
            return
        }
        mDbRef.child("user").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    userList.clear()
                    val totalUsers = snapshot.childrenCount
                    var processedUsers = 0

                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(User::class.java)
                        if (auth.currentUser?.uid != currentUser?.uid) {
                            userList.add(currentUser!!)
                        }
                        processedUsers++
                        if (processedUsers == totalUsers.toInt()) {
                            userList.sortByDescending { it.lastMessageTimestamp }
                            adapter.notifyDataSetChanged()
                            progressBar.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading users", e)
                    AlertUtils.showAlert(this@MainActivity, "Error", "Failed to load users.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Database error: ${error.message}", error.toException())
                AlertUtils.showAlert(this@MainActivity, "Error", "Failed to load users.")
                progressBar.visibility = View.GONE
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun logout() {
        AlertDialog.Builder(this).apply {
            setTitle("Confirm")
            setMessage("Are you sure you want to logout?")
            setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                val sharedPreferences = getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("isLoggedIn", false)
                editor.apply()

                auth.signOut()
                val intent = Intent(this@MainActivity, Login::class.java)
                startActivity(intent)
                finish()
            }
            setNegativeButton("No") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            create()
            show()
        }
    }

    private fun listenForNewMessages() {
        val currentUserUid = TubongeDb.getAuth().currentUser?.uid ?: return

        mDbRef.child("chats").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                mDbRef.child("chats").child(snapshot.key!!).child("messages")
                    .addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            val message = snapshot.getValue(Message::class.java)
                            message?.id = snapshot.key
                            if (message?.status == MessageStatus.SENT) {
                                message.status = MessageStatus.DELIVERED
                                mDbRef.child("chats").child(snapshot.key!!).child("messages")
                                    .child(message.id!!).child("status")
                                    .setValue(MessageStatus.DELIVERED)

                                // Trigger notification only if the message is not from the current user
                                if (message.senderId != currentUserUid) {
                                    message.senderId?.let { resetMessageCountForDifferentUser(it) }
                                    triggerNotification(message)
                                }
                            }
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {}
                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to load messages",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    })
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load chat rooms", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private val messageCountMap = mutableMapOf<String, Int>()

    private val processedMessageIds = mutableSetOf<String>()

    private fun triggerNotification(message: Message) {
        val senderId = message.senderId ?: return
        val senderName = message.senderName
        val messageId = message.id ?: return

        // Skip processing if this message has already been handled
        if (processedMessageIds.contains(messageId)) {
            return
        }

        // Mark this message as processed
        processedMessageIds.add(messageId)

        // Check if the user is currently in the chat with the sender
        val currentUserUid = TubongeDb.getAuth().currentUser?.uid
        val senderRoom = senderId + currentUserUid
        if (ChatActivity.isInChat && ChatActivity.senderRoom == senderRoom) {
            return
        }

        // Increment the message count for the sender
        val currentCount = messageCountMap.getOrDefault(senderId, 0) + 1
        messageCountMap[senderId] = currentCount

        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("uid", senderId)
            putExtra("name", senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationContent = if (currentCount > 1) {
            "You have $currentCount new messages from $senderName"
        } else {
            message.message
        }

        val notificationBuilder = NotificationCompat.Builder(this, "chat_notifications")
            .setSmallIcon(R.drawable.message_foreground)
            .setContentTitle(senderName)
            .setContentText(notificationContent)
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

        // Use the sender's UID as the unique notification ID
        val notificationId = senderId.hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun resetMessageCountForDifferentUser(newSenderId: String) {
        messageCountMap.keys.filter { it != newSenderId }.forEach { messageCountMap[it] = 0 }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}