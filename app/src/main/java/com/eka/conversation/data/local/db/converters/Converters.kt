package com.eka.conversation.data.local.db.converters

import androidx.room.TypeConverter
import com.eka.conversation.data.local.db.entities.MessageFile
import com.eka.conversation.data.local.db.entities.models.MessageFileType
import com.eka.conversation.data.local.db.entities.models.MessageRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromList(value: List<Int>?): String? {
        return value?.joinToString(",") // Convert List<Int> to String
    }

    @TypeConverter
    fun toList(value: String?): List<Int>? {
        return value?.split(",")?.map { it.toInt() } // Convert String back to List<Int>
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromMessageFile(messageFile: MessageFile?): String? {
        return messageFile?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toMessageFile(messageFileJson: String?): MessageFile? {
        return messageFileJson?.let { gson.fromJson(it, MessageFile::class.java) }
    }

    @TypeConverter
    fun fromMessageFilesList(value: List<MessageFile>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMessageFilesList(value: String?): List<MessageFile>? {
        val listType = object : TypeToken<List<MessageFile>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromMessageRole(value: MessageRole): String {
        return value.name
    }

    @TypeConverter
    fun toMessageRole(value: String): MessageRole {
        return MessageRole.valueOf(value)
    }

    @TypeConverter
    fun fromMessageFileType(fileType: MessageFileType): String {
        return fileType.name
    }

    @TypeConverter
    fun toMessageFileType(name: String): MessageFileType {
        return MessageFileType.entries.firstOrNull { it.name == name } ?: MessageFileType.ANY
    }
}