package com.example

import android.app.Application
import com.example.core.actions.ActionManager
import com.example.core.conversation.ConversationManager
import com.example.core.conversation.RoomConversationRepository
import com.example.core.engine.AssistantEngine
import com.example.core.engine.DefaultAssistantEngine
import com.example.core.permissions.PermissionManager
import com.example.core.voice.AndroidVoiceEngine
import com.example.data.db.JarvisDatabase
import com.example.data.preferences.JarvisPreferences
import com.example.data.service.GeminiServiceImpl

class JarvisApplication : Application() {

    lateinit var database: JarvisDatabase
        private set

    lateinit var conversationRepository: RoomConversationRepository
        private set

    lateinit var preferences: JarvisPreferences
        private set

    lateinit var permissionManager: PermissionManager
        private set

    lateinit var actionManager: ActionManager
        private set

    lateinit var voiceEngine: AndroidVoiceEngine
        private set

    lateinit var geminiService: GeminiServiceImpl
        private set

    lateinit var conversationManager: ConversationManager
        private set

    lateinit var assistantEngine: AssistantEngine
        private set

    override fun onCreate() {
        super.onCreate()

        database = JarvisDatabase.getDatabase(this)
        conversationRepository = RoomConversationRepository(database)
        preferences = JarvisPreferences(this)
        permissionManager = PermissionManager(this)
        actionManager = ActionManager(this)
        voiceEngine = AndroidVoiceEngine(this, preferences.language.value)
        geminiService = GeminiServiceImpl()
        conversationManager = ConversationManager(conversationRepository)

        assistantEngine = DefaultAssistantEngine(
            voiceEngine = voiceEngine,
            actionManager = actionManager,
            geminiService = geminiService,
            conversationManager = conversationManager,
            conversationRepository = conversationRepository,
            preferences = preferences
        )
    }
}
