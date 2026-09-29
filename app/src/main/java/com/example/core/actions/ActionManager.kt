package com.example.core.actions

import android.content.Context
import com.example.core.model.PendingAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages all registered actions, command evaluation, and the safety confirmation pipeline.
 */
class ActionManager(private val context: Context) {

    private val _registeredActions = mutableListOf<Action>()
    val registeredActions: List<Action> get() = _registeredActions.toList()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    init {
        // Register Part 1 foundational actions
        registerAction(TimeDateAction())
        registerAction(BatteryStatusAction())
        registerAction(DiagnosticsAction())
        registerAction(SecuritySensitiveAction())

        // Register Part 3A Phone Call Action
        registerAction(PhoneCallAction())

        // Register modular extension hooks for future parts
        registerAction(WhatsAppActionExtension())
        registerAction(YouTubeActionExtension())
        registerAction(ReminderActionExtension())
    }

    fun registerAction(action: Action) {
        if (_registeredActions.none { it.id == action.id }) {
            _registeredActions.add(action)
        }
    }

    /**
     * Attempts to find an action matching the user's input command.
     */
    fun findMatchingAction(input: String): Action? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return _registeredActions.firstOrNull { it.matches(trimmed) }
    }

    /**
     * Executes the matched action with full safety verification.
     */
    suspend fun executeAction(
        action: Action,
        rawQuery: String,
        parameters: Map<String, String> = emptyMap()
    ): ActionResult {
        val extractedParams = action.extractParameters(rawQuery)
        val combinedParams = extractedParams + parameters
        val actionContext = ActionContext(
            context = context,
            parameters = combinedParams,
            rawQuery = rawQuery
        )

        val result = action.execute(actionContext)

        if (result is ActionResult.RequiresConfirmation) {
            _pendingAction.value = result.pendingAction
        } else {
            _pendingAction.value = null
        }

        return result
    }

    /**
     * Confirms or rejects a pending action.
     */
    suspend fun resolvePendingAction(approved: Boolean): ActionResult {
        val current = _pendingAction.value
        if (current == null) {
            return ActionResult.Failure("No pending action waiting for confirmation.")
        }

        _pendingAction.value = null

        if (!approved) {
            return ActionResult.Success("Action '${current.title}' was aborted as requested.")
        }

        val targetAction = _registeredActions.firstOrNull { it.id == current.actionId }
            ?: return ActionResult.Failure("Target action definition not found.")

        // Mark parameters as confirmed and execute
        val approvedParams = current.parameters.toMutableMap().apply {
            put("confirmed", "true")
        }

        val actionContext = ActionContext(
            context = context,
            parameters = approvedParams,
            rawQuery = current.confirmationPrompt
        )

        return targetAction.execute(actionContext)
    }

    fun clearPendingAction() {
        _pendingAction.value = null
    }
}
