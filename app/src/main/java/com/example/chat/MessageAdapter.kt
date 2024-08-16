package com.example.chat

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.NetworkUtils.isNetworkAvailable
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
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.receive, parent, false)
                ReceiveViewHolder(view)
            }

            ITEM_SENT -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
                SentViewHolder(view)
            }

            ITEM_DATE -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.date_separator, parent, false)
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

    // Add a long-press listener to the message views in MessageAdapter
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = sdf.format(Date(currentMessage.timestamp ?: 0))

        when (holder) {
            is SentViewHolder -> {
                holder.sentMessage.text = currentMessage.message
                holder.sentTimestamp.text = time
                holder.sentStatus.text = when (currentMessage.status) {
                    MessageStatus.READ -> "Read"
                    MessageStatus.DELIVERED -> "Delivered"
                    MessageStatus.SENT -> "Sent"
                    MessageStatus.WAITING -> "Waiting"
                }
                holder.itemView.setOnLongClickListener {
                    showDeleteDialog(currentMessage)
                    true
                }
            }

            is ReceiveViewHolder -> {
                holder.receiveMessage.text = currentMessage.message
                holder.receiveTimestamp.text = time
                holder.itemView.setOnLongClickListener {
                    showDeleteDialog(currentMessage)
                    true
                }
            }

            is DateViewHolder -> {
                val dateSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val messageDate = Date(currentMessage.timestamp ?: 0)
                val today = Date()
                val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

                holder.dateText.text = when {
                    dateSdf.format(messageDate) == dateSdf.format(today) -> "Today"
                    dateSdf.format(messageDate) == dateSdf.format(yesterday) -> "Yesterday"
                    else -> dateSdf.format(messageDate)
                }
            }
        }
    }

    private fun showDeleteDialog(message: Message) {
        val currentTime = System.currentTimeMillis()
        val thirtyMinutesInMillis = 30 * 60 * 1000
        val canDeleteForEveryone = currentTime - (message.timestamp ?: 0) <= thirtyMinutesInMillis

        val dialogMessage = if (canDeleteForEveryone) {
            "Are you sure you want to delete this message for everyone?"
        } else {
            "Are you sure you want to delete this message for you?"
        }

        val positiveButtonText = if (canDeleteForEveryone) {
            "Delete for Everyone"
        } else {
            "Delete for You"
        }

        AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setMessage(dialogMessage)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (message.status == MessageStatus.WAITING) {
                    (context as ChatActivity).deleteWaitingMessage(message)
                } else {
                    (context as ChatActivity).deleteMessage(message)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage: TextView = itemView.findViewById(R.id.txt_sent_message)
        val sentTimestamp: TextView = itemView.findViewById(R.id.txt_sent_timestamp)
        val sentStatus: TextView = itemView.findViewById(R.id.txt_sent_status)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.txt_receive_message)
        val receiveTimestamp: TextView = itemView.findViewById(R.id.txt_receive_timestamp)
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.txt_date_separator)
    }
}