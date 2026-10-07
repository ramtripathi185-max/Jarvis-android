package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.core.actions.ActionManager
import com.example.core.conversation.ConversationManager
import com.example.core.conversation.ConversationRepository
import com.example.core.engine.DefaultAssistantEngine
import com.example.core.permissions.PermissionManager
import com.example.core.voice.AndroidVoiceEngine
import com.example.data.db.JarvisDatabase
import com.example.data.preferences.JarvisPreferences
import com.example.data.service.GeminiServiceImpl
import com.example.ui.navigation.JarvisNavHost
import com.example.ui.theme.CyberDarkNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JarvisViewModel
import com.example.ui.viewmodel.JarvisViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: JarvisViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as JarvisApplication

        // Viewmodel factory ya initializations yahan se clean run hongi
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberDarkNavy
                ) {
                    JarvisNavHost()
                }
            }
        }
    }
}
