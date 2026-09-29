package com.example.core.actions

import android.Manifest
import android.content.Context
import android.util.Log
import com.example.core.contacts.ContactItem
import com.example.core.contacts.ContactSearchResult
import com.example.core.contacts.ContactsReader
import com.example.core.contacts.PhoneCallHelper
import com.example.core.model.ActionStatus
import com.example.core.model.PendingAction
import java.util.Locale

/**
 * JARVIS Part 3A: Outgoing Phone Call Action.
 * Supports commands such as:
 * - "Rahul ko call karo"
 * - "Mummy ko phone lagao"
 * - "Papa ko call karo"
 * - "Call Amit"
 * - "Dial 9876543210"
 *
 * Implements strict contact disambiguation and safety authorization before dialing.
 */
class PhoneCallAction(
    private val contactsReaderProvider: (Context) -> ContactsReader = { ContactsReader(it) }
) : Action {

    override val id: String = "action_phone_call"
    override val title: String = "Outgoing Phone Call"
    override val description: String = "Finds contacts and initiates outgoing voice phone calls with safety confirmation."
    override val category: ActionCategory = ActionCategory.PHONE
    override val requiresConfirmation: Boolean = true
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE
    )

    private val tag = "PhoneCallAction"

    override fun matches(input: String): Boolean {
        val lower = input.trim().lowercase(Locale.ROOT)

        // Hindi / Hinglish patterns: "X ko call karo", "X ko phone lagao", "phone lagao X ko"
        if (lower.contains("call karo") ||
            lower.contains("call lagao") ||
            lower.contains("phone lagao") ||
            lower.contains("phone karo") ||
            lower.contains("dial karo") ||
            lower.contains("call kijiye") ||
            lower.contains("phone kijiye")
        ) {
            return true
        }

        // English patterns: "call X", "dial X", "make a call to X", "phone X"
        if (lower.startsWith("call ") ||
            lower.startsWith("dial ") ||
            lower.startsWith("phone ") ||
            lower.contains("make a call to ")
        ) {
            return true
        }

        return false
    }

    override fun extractParameters(input: String): Map<String, String> {
        val target = extractTargetRecipient(input)
        return if (target.isNotBlank()) mapOf("target" to target) else emptyMap()
    }

    override suspend fun execute(context: ActionContext): ActionResult {
        // 1. If this is an execution of an already-confirmed action:
        if (context.parameters["confirmed"] == "true") {
            val number = context.parameters["number"] ?: ""
            val name = context.parameters["name"] ?: number

            if (number.isBlank()) {
                return ActionResult.Failure("Invalid or missing phone number.")
            }

            return when (val callResult = PhoneCallHelper.makeCall(context.context, number)) {
                is PhoneCallHelper.CallResult.CallInitiated -> {
                    ActionResult.Success(
                        responseMessage = "Calling $name ($number)...",
                        details = mapOf("name" to name, "number" to number, "status" to "initiated")
                    )
                }
                is PhoneCallHelper.CallResult.DialerOpened -> {
                    ActionResult.Success(
                        responseMessage = "Dialer opened for $name ($number).",
                        details = mapOf("name" to name, "number" to number, "status" to "dialer")
                    )
                }
                is PhoneCallHelper.CallResult.Failure -> {
                    ActionResult.Failure("Call failure: ${callResult.reason}")
                }
            }
        }

        // 2. Extract recipient target
        val target = context.parameters["target"] ?: extractTargetRecipient(context.rawQuery)
        if (target.isBlank()) {
            return ActionResult.Failure("Please specify whom you would like to call. (e.g. 'Rahul ko call karo')")
        }

        // 3. Direct Phone Number Detection (e.g. "Call 9876543210")
        val isDirectNumber = target.replace(Regex("[^0-9+]"), "").length >= 7
        if (isDirectNumber) {
            val cleanNum = target.replace(Regex("[^0-9+]"), "")
            return ActionResult.RequiresConfirmation(
                PendingAction(
                    actionId = id,
                    title = "Call $cleanNum",
                    description = "Outgoing call to $cleanNum",
                    confirmationPrompt = "क्या मैं $cleanNum को कॉल करूँ? (Would you like me to call $cleanNum?)",
                    parameters = mapOf("name" to cleanNum, "number" to cleanNum),
                    status = ActionStatus.PENDING_CONFIRMATION
                )
            )
        }

        // 4. Contact Resolution via ContactsReader
        val contactsReader = contactsReaderProvider(context.context)
        if (!contactsReader.hasPermission()) {
            return ActionResult.MissingPermission(
                permission = Manifest.permission.READ_CONTACTS,
                rationale = "Contacts access is required to find phone numbers in your address book."
            )
        }

        return when (val searchResult = contactsReader.findContact(target)) {
            is ContactSearchResult.PermissionRequired -> {
                ActionResult.MissingPermission(
                    permission = Manifest.permission.READ_CONTACTS,
                    rationale = searchResult.rationale
                )
            }

            is ContactSearchResult.NotFound -> {
                val isHindi = context.rawQuery.contains("karo") || context.rawQuery.contains("lagao")
                val message = if (isHindi) {
                    "मुझे आपकी कांटेक्ट लिस्ट में '$target' नाम का कोई नंबर नहीं मिला।"
                } else {
                    "I could not find any contact named '$target' in your address book."
                }
                ActionResult.Success(responseMessage = message)
            }

            is ContactSearchResult.AmbiguousMatches -> {
                // Rule 5: If ambiguous, ask: "Rahul के दो contacts हैं। किसे call करूँ?"
                val matches = searchResult.matches
                val isHindi = context.rawQuery.contains("karo") || context.rawQuery.contains("lagao")

                val countHindi = when (matches.size) {
                    2 -> "दो"
                    3 -> "तीन"
                    4 -> "चार"
                    else -> "${matches.size}"
                }

                val optionsPrompt = matches.take(3).mapIndexed { index, contact ->
                    "${index + 1}. ${contact.name} (${contact.label}: ${contact.phoneNumber})"
                }.joinToString(", ")

                val prompt = if (isHindi) {
                    "${searchResult.targetQuery} के $countHindi contacts हैं: $optionsPrompt। किसे call करूँ?"
                } else {
                    "There are ${matches.size} contacts matching '${searchResult.targetQuery}': $optionsPrompt. Which one should I call?"
                }

                // Create a pending action with the top match for easy user confirmation
                val first = matches.first()
                ActionResult.RequiresConfirmation(
                    PendingAction(
                        actionId = id,
                        title = "Select Contact: ${searchResult.targetQuery}",
                        description = prompt,
                        confirmationPrompt = prompt,
                        parameters = mapOf(
                            "name" to first.name,
                            "number" to first.phoneNumber,
                            "ambiguous" to "true"
                        ),
                        status = ActionStatus.PENDING_CONFIRMATION
                    )
                )
            }

            is ContactSearchResult.SingleMatch -> {
                val contact = searchResult.contact
                if (!contact.isValidNumber) {
                    return ActionResult.Failure("Contact '${contact.name}' does not have a valid telephone number.")
                }

                val isHindi = context.rawQuery.contains("karo") || context.rawQuery.contains("lagao")
                val prompt = if (isHindi) {
                    "क्या मैं ${contact.name} (${contact.phoneNumber}) को कॉल करूँ?"
                } else {
                    "Would you like me to call ${contact.name} at ${contact.phoneNumber}?"
                }

                ActionResult.RequiresConfirmation(
                    PendingAction(
                        actionId = id,
                        title = "Call ${contact.name}",
                        description = "Outgoing phone call to ${contact.name} (${contact.phoneNumber})",
                        confirmationPrompt = prompt,
                        parameters = mapOf(
                            "name" to contact.name,
                            "number" to contact.phoneNumber
                        ),
                        status = ActionStatus.PENDING_CONFIRMATION
                    )
                )
            }
        }
    }

    /**
     * Extracts the target contact name from natural Hindi, English, and Hinglish phrases.
     */
    private fun extractTargetRecipient(rawInput: String): String {
        var text = rawInput.trim()

        // Remove politeness markers
        val politeness = listOf("please", "kripya", "zara", "bhai", "jarvis")
        for (marker in politeness) {
            text = text.replace(Regex("(?i)\\b$marker\\b"), "").trim()
        }

        // Pattern 1: "Rahul ko call karo" -> "Rahul"
        val hindiSuffixRegex = Regex(
            """(?i)^(.+?)\s+ko\s+(?:call|phone|dial)\s+(?:karo|lagao|kijiye|kar\s+do|karwana).*$"""
        )
        hindiSuffixRegex.find(text)?.let {
            val candidate = it.groupValues[1].trim()
            if (candidate.isNotBlank()) return cleanName(candidate)
        }

        // Pattern 2: "Call karo Rahul ko" or "Phone lagao Mummy ko"
        val hindiPrefixRegex = Regex(
            """(?i)^(?:call|phone|dial)\s+(?:karo|lagao|kijiye)\s+(.+?)(?:\s+ko)?$"""
        )
        hindiPrefixRegex.find(text)?.let {
            val candidate = it.groupValues[1].trim()
            if (candidate.isNotBlank()) return cleanName(candidate)
        }

        // Pattern 3: "Call Rahul" or "Dial Mummy"
        val englishPrefixRegex = Regex(
            """(?i)^(?:call|dial|phone)\s+(?:to\s+)?(.+)$"""
        )
        englishPrefixRegex.find(text)?.let {
            val candidate = it.groupValues[1].trim()
            if (candidate.isNotBlank()) return cleanName(candidate)
        }

        // Pattern 4: "Rahul ko call" or "Mummy ko phone"
        val simpleSuffixRegex = Regex("""(?i)^(.+?)\s+ko\s+(?:call|phone|dial).*$""")
        simpleSuffixRegex.find(text)?.let {
            val candidate = it.groupValues[1].trim()
            if (candidate.isNotBlank()) return cleanName(candidate)
        }

        return cleanName(text)
    }

    private fun cleanName(raw: String): String {
        return raw.replace(Regex("[?.!,]"), "").trim()
    }
}
