fun deleteMessage(messageId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
        // delete locally
        repo.deleteMessage(messageId)

        // refresh current thread UI (do not change read state)
        currentThreadId?.let { id ->
            withContext(Dispatchers.Main) {
                refreshThread(id, refreshRead = false)
            }
        }
    }
}

fun deleteMessageForEveryone(messageId: Long) {
    val message = rawMessages.value.firstOrNull { it.id == messageId } ?: return

    viewModelScope.launch(Dispatchers.IO) {
        // delete locally
        repo.deleteMessage(messageId)

        // build delete command SMS
        val snippet = message.body.take(30)
        val deleteCommand = SotextProtocol.encodeDeleteCommand(
            messageId = messageId,
            timestamp = message.timestamp,
            bodySnippet = snippet
        )

        // send control SMS to recipient
        repo.sendSms(message.address, deleteCommand)

        // refresh current thread UI
        currentThreadId?.let { id ->
            withContext(Dispatchers.Main) {
                refreshThread(id, refreshRead = false)
            }
        }
    }
}
