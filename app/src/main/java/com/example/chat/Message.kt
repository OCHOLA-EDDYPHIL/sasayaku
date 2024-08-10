class Message {
    var message: String? = null
    var senderId: String? = null
    var timestamp: Long? = null
    var isDateSeparator: Boolean = false

    constructor() {}

    constructor(message: String?, senderId: String?, timestamp: Long?, isDateSeparator: Boolean = false) {
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
        this.isDateSeparator = isDateSeparator
    }
}