package com.example.chat

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

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
        refreshUserData()
    }

    private fun logout() {
        AlertDialog.Builder(this).apply {
            setTitle("Confirm")
            setMessage("Are you sure you want to logout?")
            setPositiveButton("Yes") { dialog: DialogInterface, _: Int ->
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