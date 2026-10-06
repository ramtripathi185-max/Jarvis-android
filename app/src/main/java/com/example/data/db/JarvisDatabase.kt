package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.db.dao.ChatMessageDao
import com.example.data.db.entity.ChatMessageEntity

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}
