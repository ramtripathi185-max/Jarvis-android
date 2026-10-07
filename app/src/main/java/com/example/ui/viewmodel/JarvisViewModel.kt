package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.conversation.ConversationRepository
import kotlinx.coroutines.launch

class JarvisViewModel(
    private val repository: ConversationRepository
) : ViewModel() {

    fun seedWelcomeIfNeeded() {
        // Welcome message logic
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllMessages()
        }
    }
}
