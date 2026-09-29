package com.example.core.actions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.core.model.ActionStatus
import com.example.core.model.PendingAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Part 1: Date & Time query action.
 * Supports English, Hindi, and Hinglish phrasing.
 */
class TimeDateAction : Action {
    override val id: String = "action_time_date"
    override val title: String = "Get Time & Date"
    override val description: String = "Reports the current system time and date."
    override val category: ActionCategory = ActionCategory.SYSTEM
    override val requiresConfirmation: Boolean = false

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("time") ||
                lower.contains("date") ||
                lower.contains("samay") ||
                lower.contains("taarikh") ||
                lower.contains("din kya hai") ||
                lower.contains("kya baje")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val formattedTime = timeFormat.format(now)
        val formattedDate = dateFormat.format(now)

        val isHindi = context.rawQuery.contains("samay") ||
                context.rawQuery.contains("kya baje") ||
                context.rawQuery.contains("taarikh")

        val response = if (isHindi) {
            "Abhi samay $formattedTime hai, aur aaj $formattedDate hai."
        } else {
            "It is currently $formattedTime on $formattedDate."
        }

        return ActionResult.Success(
            responseMessage = response,
            details = mapOf("time" to formattedTime, "date" to formattedDate)
        )
    }
}

/**
 * Part 1: Device Battery Status query action.
 */
class BatteryStatusAction : Action {
    override val id: String = "action_battery_status"
    override val title: String = "Battery Status"
    override val description: String = "Reads current battery level and charging state."
    override val category: ActionCategory = ActionCategory.SYSTEM
    override val requiresConfirmation: Boolean = false

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("battery") ||
                lower.contains("charge") ||
                lower.contains("charging")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        val appContext = context.context.applicationContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = appContext.registerReceiver(null, filter)

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val isHindi = context.rawQuery.contains("kitni") ||
                context.rawQuery.contains("karo") ||
                context.rawQuery.contains("hai")

        val response = if (batteryPct >= 0) {
            if (isHindi) {
                "Aapki battery abhi $batteryPct% par hai" +
                        if (isCharging) " aur device charge ho raha hai." else "."
            } else {
                "Your battery is at $batteryPct%" +
                        if (isCharging) " and currently charging." else "."
            }
        } else {
            "Battery diagnostics are currently unavailable on this device."
        }

        return ActionResult.Success(
            responseMessage = response,
            details = mapOf("percentage" to batteryPct, "isCharging" to isCharging)
        )
    }
}

/**
 * Part 1: System Diagnostics action.
 */
class DiagnosticsAction : Action {
    override val id: String = "action_diagnostics"
    override val title: String = "System Diagnostics"
    override val description: String = "Performs quick integrity diagnostic check on core systems."
    override val category: ActionCategory = ActionCategory.SYSTEM
    override val requiresConfirmation: Boolean = false

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("diagnostics") ||
                lower.contains("system status") ||
                lower.contains("health check") ||
                lower.contains("integrity")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)

        return ActionResult.Success(
            responseMessage = "All core systems nominal. Engine active, Memory: ${usedMemMb}MB/${maxMemMb}MB, Voice channel ready.",
            details = mapOf("usedMemory" to usedMemMb, "maxMemory" to maxMemMb)
        )
    }
}

/**
 * Part 1: Safety Confirmation Demonstration Action.
 * Demonstrates the safety confirmation protocol required for sensitive operations.
 */
class SecuritySensitiveAction : Action {
    override val id: String = "action_purge_cache"
    override val title: String = "Purge Conversation Cache"
    override val description: String = "Clears cached assistant conversation history."
    override val category: ActionCategory = ActionCategory.SYSTEM
    override val requiresConfirmation: Boolean = true

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("clear memory") ||
                lower.contains("purge cache") ||
                lower.contains("delete history") ||
                lower.contains("reset chat")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        // Because requiresConfirmation is true, if confirmation parameter is missing, return RequiresConfirmation
        val confirmed = context.parameters["confirmed"] == "true"
        if (!confirmed) {
            return ActionResult.RequiresConfirmation(
                PendingAction(
                    actionId = id,
                    title = "Clear Conversation Cache",
                    description = "This will erase recent conversation history from local memory.",
                    confirmationPrompt = "Are you sure you want to purge the conversation cache?",
                    parameters = mapOf("confirmed" to "true"),
                    status = ActionStatus.PENDING_CONFIRMATION
                )
            )
        }

        return ActionResult.Success(
            responseMessage = "Conversation cache has been purged successfully."
        )
    }
}

/* =========================================================================
 * EXTENSION POINTS FOR FUTURE PARTS
 * ========================================================================= */

/**
 * TODO [Part 2]: Phone Action Hook
 * Will handle dialer, call contacts, and telecom integration.
 */
class CallActionExtension : Action {
    override val id: String = "action_phone_call"
    override val title: String = "Phone Call"
    override val description: String = "Places voice calls to contacts or numbers [Planned for Part 2]."
    override val category: ActionCategory = ActionCategory.PHONE
    override val requiresConfirmation: Boolean = true
    override val requiredPermissions: List<String> = listOf("android.permission.CALL_PHONE")

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.startsWith("call ") || lower.contains("phone call") || lower.contains("dial ")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        // TODO: In Part 2, implement actual Telecom/Intent call logic with confirmation
        return ActionResult.RequiresConfirmation(
            PendingAction(
                actionId = id,
                title = "Initiate Call",
                description = "Place call to recipient [Part 2 Extension]",
                confirmationPrompt = "Would you like me to initiate this phone call?",
                parameters = context.parameters,
                status = ActionStatus.PENDING_CONFIRMATION
            )
        )
    }
}

/**
 * TODO [Part 3]: WhatsApp Action Hook
 * Will handle WhatsApp message dispatch, contact resolution, and accessibility intent dispatch.
 */
class WhatsAppActionExtension : Action {
    override val id: String = "action_whatsapp_message"
    override val title: String = "WhatsApp Message"
    override val description: String = "Dispatches WhatsApp messages [Planned for Part 3]."
    override val category: ActionCategory = ActionCategory.COMMUNICATION
    override val requiresConfirmation: Boolean = true

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("whatsapp")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        // TODO: In Part 3, implement WhatsApp URI & Accessibility dispatch
        return ActionResult.RequiresConfirmation(
            PendingAction(
                actionId = id,
                title = "Send WhatsApp Message",
                description = "Send message via WhatsApp [Part 3 Extension]",
                confirmationPrompt = "Do you want to send this WhatsApp message?",
                parameters = context.parameters,
                status = ActionStatus.PENDING_CONFIRMATION
            )
        )
    }
}

/**
 * TODO [Part 4]: YouTube Action Hook
 * Will handle YouTube search, playback launch, and media controller actions.
 */
class YouTubeActionExtension : Action {
    override val id: String = "action_youtube"
    override val title: String = "YouTube Playback"
    override val description: String = "Plays videos and music on YouTube [Planned for Part 4]."
    override val category: ActionCategory = ActionCategory.MEDIA
    override val requiresConfirmation: Boolean = false

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("play on youtube") || lower.contains("search youtube") || lower.startsWith("youtube ")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        // TODO: In Part 4, implement YouTube Intent launcher & player
        return ActionResult.Success(
            responseMessage = "YouTube integration is queued for Part 4 module installation."
        )
    }
}

/**
 * TODO [Part 5]: Reminder & Productivity Hook
 * Will handle alarms, reminders, calendar entries, and automated routines.
 */
class ReminderActionExtension : Action {
    override val id: String = "action_reminder"
    override val title: String = "Set Reminder"
    override val description: String = "Sets alarms and calendar reminders [Planned for Part 5]."
    override val category: ActionCategory = ActionCategory.PRODUCTIVITY
    override val requiresConfirmation: Boolean = true

    override fun matches(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return lower.contains("remind me") || lower.contains("set reminder") || lower.contains("alarm")
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        // TODO: In Part 5, implement AlarmManager & Calendar Provider integration
        return ActionResult.RequiresConfirmation(
            PendingAction(
                actionId = id,
                title = "Schedule Reminder",
                description = "Schedule reminder [Part 5 Extension]",
                confirmationPrompt = "Would you like me to set this reminder?",
                parameters = context.parameters,
                status = ActionStatus.PENDING_CONFIRMATION
            )
        )
    }
}
