package com.eka.conversation.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eka.conversation.common.Constants
import com.eka.conversation.data.local.daos.MessageDao
import com.eka.conversation.data.local.daos.MessageFileDao
import com.eka.conversation.data.local.db.converters.Converters
import com.eka.conversation.data.local.db.entities.MessageEntity
import com.eka.conversation.data.local.db.entities.MessageFTSEntity
import com.eka.conversation.data.local.db.entities.MessageFile

@Database(
    entities = [
        MessageEntity::class,
        MessageFile::class,
        MessageFTSEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun messageFileDao() : MessageFileDao

    companion object {
        @Volatile
        private var INSTANCE : ChatDatabase? = null

        fun getDatabase(context: Context) : ChatDatabase {
            return (INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    Constants.CHAT_DB_NAME
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            })
        }
    }
}