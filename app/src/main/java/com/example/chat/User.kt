package com.example.chat

class User {
    var name : String? = null
    var email : String? = null
    var uid : String? = null
    var lastMessageTimestamp: Long? = null

    constructor(){}

    constructor(name: String?, email: String?, uid: String?, lastMessageTimestamp: Long?){
        this.name = name
        this.email = email
        this.uid = uid
        this.lastMessageTimestamp = lastMessageTimestamp
    }
}