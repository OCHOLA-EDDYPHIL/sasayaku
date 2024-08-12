package com.example.chat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "chat.db"
        private const val DATABASE_VERSION = 2 // Incremented version
        const val TABLE_MESSAGES = "messages"
        const val COLUMN_ID = "id"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_SENDER_ID = "sender_id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SENDER_ROOM = "sender_room"
        const val COLUMN_RECEIVER_ROOM = "receiver_room"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_MESSAGES ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_MESSAGE TEXT, "
                + "$COLUMN_SENDER_ID TEXT, "
                + "$COLUMN_TIMESTAMP LONG, "
                + "$COLUMN_SENDER_ROOM TEXT, "
                + "$COLUMN_RECEIVER_ROOM TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COLUMN_SENDER_ROOM TEXT")
            db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COLUMN_RECEIVER_ROOM TEXT")
        }
    }
}