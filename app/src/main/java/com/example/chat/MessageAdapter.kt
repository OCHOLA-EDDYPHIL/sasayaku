package com.example.chat

import Message
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val ITEM_RECEIVE = 1
    val ITEM_SENT = 2
    val ITEM_DATE = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_RECEIVE -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.receive, parent, false)
                ReceiveViewHolder(view)
            }
            ITEM_SENT -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
                SentViewHolder(view)
            }
            ITEM_DATE -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.date_separator, parent, false)
                DateViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        return if (currentMessage.isDateSeparator) {
            ITEM_DATE
        } else if (FirebaseAuth.getInstance().currentUser?.uid == currentMessage.senderId) {
            ITEM_SENT
        } else {
            ITEM_RECEIVE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = sdf.format(Date(currentMessage.timestamp ?: 0))

        when (holder) {
            is SentViewHolder -> {
                holder.sentMessage.text = currentMessage.message
                holder.sentTimestamp.text = time
            }
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = currentMessage.message
                holder.receiveTimestamp.text = time
            }
            is DateViewHolder -> {
                holder.dateText.text = currentMessage.message
            }
        }
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage: TextView = itemView.findViewById(R.id.txt_sent_message)
        val sentTimestamp: TextView = itemView.findViewById(R.id.txt_sent_timestamp)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.txt_receive_message)
        val receiveTimestamp: TextView = itemView.findViewById(R.id.txt_receive_timestamp)
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.txt_date_separator)
    }
}