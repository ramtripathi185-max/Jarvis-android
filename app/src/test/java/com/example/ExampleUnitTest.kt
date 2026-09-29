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
}

