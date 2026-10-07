package com.example.data.db

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "dummy_table")
data class DummyEntity(
    @PrimaryKey val id: Int = 1
)

@Database(
    entities = [DummyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    // Zero dependencies on external missing classes/DAOs
}
