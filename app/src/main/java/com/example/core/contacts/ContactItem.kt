package com.example.core.contacts

/**
 * Clean data model representing a resolved contact entry.
 */
data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val label: String = "Mobile",
    val photoUri: String? = null
) {
    /**
     * Sanitized telephone number for dialing (digits and optional leading +).
     */
    val dialableNumber: String
        get() = phoneNumber.replace(Regex("[^0-9+]"), "")

    val isValidNumber: Boolean
        get() = dialableNumber.length >= 3
}

sealed interface ContactSearchResult {
    data class SingleMatch(val contact: ContactItem) : ContactSearchResult
    data class AmbiguousMatches(val targetQuery: String, val matches: List<ContactItem>) : ContactSearchResult
    data class NotFound(val targetQuery: String) : ContactSearchResult
    data class PermissionRequired(val rationale: String) : ContactSearchResult
}
