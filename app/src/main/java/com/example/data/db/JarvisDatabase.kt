package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.db.entity.ChatMessageEntity // Aapki entity ka correct import path

@Database(
    entities = [ChatMessageEntity::class], // Yahan apni entity class specify karein
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    // Aapke DAOs yahan define honge
}
