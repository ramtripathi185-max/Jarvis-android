package com.example

import com.example.core.actions.ActionCategory
import com.example.core.actions.ActionResult
import com.example.core.actions.SecuritySensitiveAction
import com.example.core.actions.TimeDateAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun timeAction_matchesKeywords() {
    val action = TimeDateAction()
    assertTrue(action.matches("what is the time"))
    assertTrue(action.matches("aaj ka samay kya hai"))
    assertFalse(action.matches("play music"))
  }

  @Test
  fun securitySensitiveAction_requiresConfirmationByDefault() = runBlocking {
    val action = SecuritySensitiveAction()
    assertTrue(action.requiresConfirmation)
    assertEquals(ActionCategory.SYSTEM, action.category)
  }

  @Test
  fun liveSessionState_hasExpectedTransitions() {
    val states = com.example.core.voice.live.LiveSessionState.values()
    assertTrue(states.contains(com.example.core.voice.live.LiveSessionState.LISTENING))
    assertTrue(states.contains(com.example.core.voice.live.LiveSessionState.SPEAKING))
    assertTrue(states.contains(com.example.core.voice.live.LiveSessionState.INTERRUPTED))
  }

  @Test
  fun wakeWordState_hasExpectedTransitions() {
    val states = com.example.core.voice.wakeword.WakeWordState.values()
    assertTrue(states.contains(com.example.core.voice.wakeword.WakeWordState.DISABLED))
    assertTrue(states.contains(com.example.core.voice.wakeword.WakeWordState.ARMED_FOREGROUND))
    assertTrue(states.contains(com.example.core.voice.wakeword.WakeWordState.TRIGGERED))
  }
}

