package com.example.chat

class Message {
    var message: String? = null
    var senderId: String? = null
    var timestamp: Long? = null

    constructor(){}

    constructor(message: String?, senderId: String?, timestamp: Long?){
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
    }
}