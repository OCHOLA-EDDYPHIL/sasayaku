package com.example.chat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdapter(val context: Context, val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        holder.textName.text = currentUser.name

        val lastMessageTime = currentUser.lastMessageTimestamp
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val messageDate = Date(lastMessageTime ?: 0)
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

        holder.textLastMessageTime.text = when {
            lastMessageTime == null -> "Never"
            dateSdf.format(messageDate) == dateSdf.format(today) -> sdf.format(messageDate)
            dateSdf.format(messageDate) == dateSdf.format(yesterday) -> "Yesterday"
            else -> dateSdf.format(messageDate)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)
            context.startActivity(intent)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName = itemView.findViewById<TextView>(R.id.txt_name)
        val textLastMessageTime = itemView.findViewById<TextView>(R.id.txt_last_message_time)
    }
}