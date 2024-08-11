package com.example.chat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "chat.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_MESSAGES = "messages"
        const val COLUMN_ID = "id"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_SENDER_ID = "sender_id"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_MESSAGES ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_MESSAGE TEXT, "
                + "$COLUMN_SENDER_ID TEXT, "
                + "$COLUMN_TIMESTAMP LONG)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }
}