package com.example

import android.app.Application
import com.example.core.actions.ActionManager
import com.example.core.conversation.ConversationRepository
import com.example.core.engine.AssistantEngine
import com.example.core.engine.DefaultAssistantEngine
import com.example.core.permissions.PermissionManager
import com.example.core.voice.AndroidVoiceEngine
import com.example.core.voice.live.LiveVoiceEngine
import com.example.core.voice.VoiceEngine
import com.example.data.db.JarvisDatabase
import com.example.data.preferences.JarvisPreferences
import com.example.data.service.GeminiServiceImpl

class JarvisApplication : Application() {

    lateinit var database: JarvisDatabase
        private set

    lateinit var conversationRepository: ConversationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = androidx.room.Room.databaseBuilder(
            applicationContext,
            JarvisDatabase::class.java,
            "jarvis_db"
        ).build()

        conversationRepository = ConversationRepository()
    }
}
