# JARVIS - Voice-First Personal AI Assistant (Part 1 & Part 2A)

JARVIS is a production-ready, voice-first personal AI assistant for Android powered by Google Gemini API, Jetpack Compose, Material 3, and a clean, modular architecture.

This project delivers **Part 1 (The Core Foundation)** and **Part 2A (Real-Time Live Voice Engine)**. It establishes the assistant architecture, futuristic HUD interface with animated circular voice reactor, low-latency Gemini Live WebSocket bidirectional audio streaming, instant voice barge-in (interruption), speech recognition fallback, safety confirmation pipeline, action manager, and Room persistence.

---

## 🚀 Key Features in Part 2A (Real-Time Voice Engine)

1. **Dedicated Real-Time Live Voice Engine (`LiveVoiceEngine`):**
   - Direct bidirectional streaming connection over OkHttp WebSocket to Gemini Multimodal Live API (`gemini-2.5-flash-native-audio-preview-12-2025`).
   - Ultra-low latency voice interaction with native 24kHz audio playback via `AudioTrack`.
   - Continuous audio streaming input via `AudioRecord` (16kHz, 16-bit PCM Mono).

2. **Instant Interruption (Barge-in):**
   - The user can interrupt JARVIS at any time while it is speaking!
   - Instantaneous buffer flushing via `AudioOutputManager.flushAndStop()` triggered by local mic energy detection or server interruption signals.
   - Smooth transition back to active listening without delays or audio artifacts.

3. **Android Compliant Background Execution:**
   - `JarvisLiveAudioService`: Foreground Service with `foregroundServiceType="microphone"`, adhering strictly to Android 14+ background execution policies.
   - Ongoing system notification with one-tap disconnect capability.

4. **Multi-Language Persona:**
   - Native support for **Hindi**, **English**, and conversational **Hinglish**.
   - Choice of prebuilt Gemini voices (`Puck`, `Aoede`, `Charon`, `Fenrir`, `Kore`).

5. **Robust Fallback & Hybrid Support:**
   - If real-time streaming is disabled or offline, automatically falls back to Android `SpeechRecognizer` + `TextToSpeech` without crashing.
   - Text terminal conversation remains 100% accessible and synchronized.

---

## 🚀 Key Features in Part 1 (Foundation)

1. **Futuristic Cybernetic HUD & Reactor:**
   - Custom animated circular voice indicator (`JarvisArcReactor`) with dynamic state transitions:
     - **IDLE:** Ambient breathing pulse with slow orbital rotation.
     - **LISTENING:** Real-time audio waveform reactivity synchronized to microphone decibel levels.
     - **THINKING:** Accelerated cyber-arc rotation during Gemini API processing.
     - **SPEAKING:** Concentric sonic wave pulses matched to vocal synthesis.
     - **ERROR:** Cautionary diagnostic pulse with user-friendly recovery instructions.
   - Live telemetry status bar (model indicator, connection sync, battery & memory diagnostics).

2. **Google Gemini API Integration:**
   - Dedicated service layer (`GeminiService`) decoupled from the UI.
   - Robust networking with OkHttp (60s timeouts) and Retrofit.
   - Multi-turn conversation management with automatic token context budgeting.
   - Graceful offline detection, rate-limit handling, and clear error translation.

3. **Command & Safety Architecture:**
   - `Action` interface with built-in intent parsing.
   - Part 1 actions: Date & Time, Battery status, and System diagnostics.
   - **Safety First Principle:** Sensitive operations cannot run silently. Requires explicit user authorization via interactive confirmation cards or voice response ("yes / authorize" vs "no / abort").
   - Clear extension hooks and architecture for Parts 2 through 5.

4. **Local Persistence & Settings:**
   - Room Database (`JarvisDatabase`) caching conversation history locally.
   - In-app configuration screen for language, model (`gemini-3.5-flash` vs `gemini-3.1-pro-preview`), speech synthesis tuning, and custom personality instructions.

---

## 🛠️ Architecture Overview

The codebase is organized into modular packages under `com.example`:

```
com.example
├── MainActivity.kt                 # Application entry point & permission flow
├── core/
│   ├── actions/                    # Command & safety pipeline
│   │   ├── Action.kt               # Universal action interface & context
│   │   ├── ActionManager.kt        # Action registry & authorization broker
│   │   └── BuiltInActions.kt       # Part 1 actions + extension hooks for Parts 2-5
│   ├── conversation/               # Dialogue management & prompt orchestration
│   │   ├── ConversationManager.kt  # JARVIS persona builder & prompt injection
│   │   └── ConversationRepository.kt# Message persistence interface & Room repo
│   ├── engine/                     # Central assistant orchestrator
│   │   ├── AssistantEngine.kt      # Core engine contract
│   │   └── DefaultAssistantEngine.kt# Concurrency & turn-taking coordination
│   ├── model/                      # Domain models (AssistantState, Message, etc.)
│   ├── permissions/                # System permission verification
│   └── voice/                      # Audio I/O abstraction layer
│       ├── VoiceEngine.kt          # Pluggable audio contract
│       └── AndroidVoiceEngine.kt   # SpeechRecognizer + TTS implementation
├── data/
│   ├── api/                        # Gemini REST API data structures & endpoints
│   ├── db/                         # Room database, DAO & entities
│   ├── preferences/                # SharedPreferences store for user config
│   └── service/                    # GeminiServiceImpl (Retrofit + OkHttp)
└── ui/
    ├── components/                 # JarvisArcReactor, ActionConfirmationCard, CyberHeader
    ├── navigation/                 # Navigation bar & tab host
    ├── screens/                    # HUD, Conversation, Permissions, Settings
    ├── theme/                      # Cybernetic color palette & Typography
    └── viewmodel/                  # JarvisViewModel & Factory
```

---

## 🔑 Gemini API Configuration

API keys are managed securely and injected at build time via `BuildConfig.GEMINI_API_KEY`:

1. **AI Studio Secrets Panel (Recommended):**
   - In AI Studio, open the **Secrets panel**.
   - Add a secret named `GEMINI_API_KEY` with your Google Gemini API key.
   - The platform automatically injects this secret into `.env` at runtime.

2. **Local Development (.env file):**
   - Create a file named `.env` in the project root directory:
     ```env
     GEMINI_API_KEY=YOUR_ACTUAL_GEMINI_API_KEY
     ```
   - Never commit your `.env` file to version control (it is ignored by `.gitignore`).

> **Safety Notice:** If no API key is provided, the app continues to operate without crashing. System diagnostic actions and the UI remain fully interactive, and the Settings screen provides guidance on adding the key.

---

## 📱 Android Permissions

Declared in `AndroidManifest.xml`:
- `android.permission.INTERNET`: Required for communicating with Google Gemini API servers.
- `android.permission.RECORD_AUDIO`: Required for speech input via the microphone.
- `android.permission.ACCESS_NETWORK_STATE`: Monitors offline/online connection status.
- `android.permission.MODIFY_AUDIO_SETTINGS`: Optimizes audio routing for voice recording and playback.

---

## 🏗️ Build & Run Instructions

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 36 (compileSdk = 36, minSdk = 24)
- JDK 17 / 21

### Running via Gradle
To compile and assemble the debug APK:
```bash
gradle :app:assembleDebug
```

To run unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 🗺️ Roadmap: Future Parts (Parts 2 - 5)

The Part 1 codebase has been specifically designed so that future parts plug directly into existing interfaces without refactoring core logic:

| Part | Planned Capability | Architectural Touchpoints |
|---|---|---|
| **Part 2** | **Telephony & Real-Time Voice** | Implement `GeminiLiveVoiceEngine` (WebSocket/Bidi audio) replacing/supplementing `VoiceEngine`; Implement `CallActionExtension` for phone dialer & contact calls. |
| **Part 3** | **Messaging & WhatsApp Automation** | Implement `WhatsAppActionExtension` with accessibility automation; Notification dispatch service. |
| **Part 4** | **Media Controls & YouTube** | Implement `YouTubeActionExtension` with media session & video launcher controls. |
| **Part 5** | **Alarms, Reminders & System Automations** | Implement `ReminderActionExtension` backed by AlarmManager and Calendar Provider; automated recurring routines. |

---

## 🛡️ Assistant Persona

- **Name:** J.A.R.V.I.S.
- **Attributes:** Calm, intelligent, concise, respectful, helpful, futuristic.
- **Spoken Tone:** Natural and crisp sentences optimized for speech synthesis without verbose formatting.
- **Multilingual:** Seamlessly understands and responds in English, Hindi, or conversational Hinglish based on user preference.
