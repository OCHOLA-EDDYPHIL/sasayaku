package com.example.chat

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var userDbHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the toolbar
//        val toolbar: Toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)
//        supportActionBar?.title = getString(R.string.app_name)

        auth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()
        userDbHelper = UserDatabaseHelper(this)

        userList = ArrayList()
        adapter = UserAdapter(this, userList)

        userRecyclerView = findViewById(R.id.userRecyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter
        userRecyclerView.edgeEffectFactory = SpringEdgeEffectFactory(this)

        loadUsersFromLocalDb()

        if (isNetworkAvailable()) {
            syncUsersWithFirebase()
        }
    }

    private fun saveUserToLocalDb(user: User) {
        val db = userDbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDatabaseHelper.COLUMN_UID, user.uid)
            put(UserDatabaseHelper.COLUMN_NAME, user.name)
            put(UserDatabaseHelper.COLUMN_EMAIL, user.email)
        }
        db.insertWithOnConflict(UserDatabaseHelper.TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun loadUsersFromLocalDb() {
        val db = userDbHelper.readableDatabase
        val cursor: Cursor = db.query(
            UserDatabaseHelper.TABLE_USERS,
            null, null, null, null, null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                val user = User(
                    getString(getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_NAME)),
                    getString(getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_EMAIL)),
                    getString(getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_UID))
                )
                userList.add(user)
            }
        }
        cursor.close()
        adapter.notifyDataSetChanged()
    }

    private fun syncUsersWithFirebase() {
        mDbRef.child("user").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (postSnapshot in snapshot.children) {
                    val currentUser = postSnapshot.getValue(User::class.java)
                    if (auth.currentUser?.uid != currentUser?.uid) {
                        userList.add(currentUser!!)
                        saveUserToLocalDb(currentUser)
                    }
                }
                adapter.notifyDataSetChanged()
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