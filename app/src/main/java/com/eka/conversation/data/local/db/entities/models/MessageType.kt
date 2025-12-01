package com.eka.conversation.data.local.db.entities.models

enum class MessageType(val stringValue: String) {
    TEXT("text"),
    SINGLE_SELECT("single_select"),
    MULTI_SELECT("multi_select"),
}