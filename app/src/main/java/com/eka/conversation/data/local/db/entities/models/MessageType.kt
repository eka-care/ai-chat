package com.eka.conversation.data.local.db.entities.models

enum class Type(val stringValue: String) {
    TEXT("text"),
    IMAGE("image"),
    PDF("pdf"),
    CUSTOM("custom")
}