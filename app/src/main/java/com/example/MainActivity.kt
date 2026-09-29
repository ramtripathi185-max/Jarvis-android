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
import com.example.core.conversation.RoomConversationRepository
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

        // 1. Retrieve Singletons from Application
        val app = application as JarvisApplication

        // 2. ViewModel Initialization
        val factory = JarvisViewModelFactory(
            app.assistantEngine,
            app.permissionManager,
            app.preferences
        )
        viewModel = ViewModelProvider(this, factory)[JarvisViewModel::class.java]

        // 3. Compose UI Content
        setContent {
            MyApplicationTheme {
                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    viewModel.refreshPermissions()
                    if (isGranted) {
                        Toast.makeText(this, "Microphone access granted for JARVIS", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Microphone access required for voice commands", Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberDarkNavy
                ) {
                    JarvisNavHost(
                        viewModel = viewModel,
                        onRequestPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshPermissions()
        }
    }
}
