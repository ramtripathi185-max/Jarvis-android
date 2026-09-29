package com.example.core.actions

import android.content.Context
import com.example.core.model.PendingAction

enum class ActionCategory {
    SYSTEM,
    PHONE,
    COMMUNICATION,
    MEDIA,
    PRODUCTIVITY,
    CUSTOM
}

data class ActionContext(
    val context: Context,
    val parameters: Map<String, String> = emptyMap(),
    val rawQuery: String = ""
)

sealed interface ActionResult {
    data class Success(
        val responseMessage: String,
        val details: Map<String, Any> = emptyMap()
    ) : ActionResult

    data class RequiresConfirmation(
        val pendingAction: PendingAction
    ) : ActionResult

    data class MissingPermission(
        val permission: String,
        val rationale: String
    ) : ActionResult

    data class Failure(
        val errorMessage: String,
        val throwable: Throwable? = null
    ) : ActionResult
}

/**
 * Universal Action interface for JARVIS commands.
 * Modular design allows adding Phone, WhatsApp, YouTube, Reminders in Parts 2-5.
 */
interface Action {
    val id: String
    val title: String
    val description: String
    val category: ActionCategory
    val requiresConfirmation: Boolean
    val requiredPermissions: List<String>
        get() = emptyList()

    /**
     * Determines whether this action can handle the given text command.
     */
    fun matches(input: String): Boolean

    /**
     * Extracts parameters from the input command.
     */
    fun extractParameters(input: String): Map<String, String> = emptyMap()

    /**
     * Executes the action.
     */
    suspend fun execute(context: ActionContext): ActionResult
}
