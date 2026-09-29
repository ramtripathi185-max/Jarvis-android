package com.example.core.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Securely reads and matches device contacts using Android ContactsContract.
 */
class ContactsReader(private val context: Context) {

    private val tag = "ContactsReader"

    /**
     * Common Hindi-English honorific / relational aliases
     */
    private val relationalAliases = mapOf(
        "मम्मी" to listOf("mummy", "mom", "mother", "maa", "mataji"),
        "माँ" to listOf("mummy", "mom", "mother", "maa"),
        "मां" to listOf("mummy", "mom", "mother", "maa"),
        "mummy" to listOf("mom", "mother", "maa"),
        "mom" to listOf("mummy", "mother", "maa"),
        "पापा" to listOf("papa", "dad", "father", "pitaji"),
        "पिताजी" to listOf("papa", "dad", "father"),
        "papa" to listOf("dad", "father"),
        "dad" to listOf("papa", "father"),
        "भाई" to listOf("bhai", "brother", "bhaiya"),
        "भैया" to listOf("bhai", "bhaiya", "brother"),
        "bhai" to listOf("brother", "bhaiya"),
        "दीदी" to listOf("didi", "sister"),
        "बहन" to listOf("didi", "sister", "behen")
    )

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads all contacts with phone numbers from Android ContactsContract.
     */
    fun readAllContacts(): List<ContactItem> {
        if (!hasPermission()) {
            Log.w(tag, "Attempted to read contacts without READ_CONTACTS permission.")
            return emptyList()
        }

        val contactsList = mutableListOf<ContactItem>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) ?: "" else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val type = if (typeIndex != -1) it.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val customLabel = if (labelIndex != -1) it.getString(labelIndex) else null
                    val photo = if (photoIndex != -1) it.getString(photoIndex) else null

                    val label = when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel ?: "Custom"
                        else -> "Phone"
                    }

                    if (name.isNotBlank() && number.isNotBlank()) {
                        contactsList.add(
                            ContactItem(
                                id = id,
                                name = name.trim(),
                                phoneNumber = number.trim(),
                                label = label,
                                photoUri = photo
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading contacts from device", e)
        }

        return contactsList
    }

    /**
     * Natural-language contact matching.
     */
    fun findContact(query: String): ContactSearchResult {
        if (!hasPermission()) {
            return ContactSearchResult.PermissionRequired(
                "Contacts access is required to look up phone numbers."
            )
        }

        val trimmedQuery = query.trim().lowercase(Locale.ROOT)
        if (trimmedQuery.isEmpty()) {
            return ContactSearchResult.NotFound(query)
        }

        val allContacts = readAllContacts()
        if (allContacts.isEmpty()) {
            return ContactSearchResult.NotFound(query)
        }

        // Build list of target candidate aliases
        val targetAliases = mutableSetOf(trimmedQuery)
        relationalAliases[trimmedQuery]?.let { targetAliases.addAll(it) }

        // Tier 1: Exact Match (e.g. "Rahul" == "Rahul")
        val exactMatches = allContacts.filter { contact ->
            val contactNameLower = contact.name.lowercase(Locale.ROOT)
            targetAliases.any { alias -> contactNameLower == alias }
        }

        if (exactMatches.size == 1) {
            return ContactSearchResult.SingleMatch(exactMatches.first())
        } else if (exactMatches.size > 1) {
            return disambiguateOrGroup(query, exactMatches)
        }

        // Tier 2: Word Token / Boundary Match (e.g. "Rahul" in "Rahul Sharma", "Mummy" in "Mummy Jio")
        val wordMatches = allContacts.filter { contact ->
            val contactWords = contact.name.lowercase(Locale.ROOT)
                .split(Regex("[\\s._-]+"))
            targetAliases.any { alias ->
                contactWords.contains(alias)
            }
        }

        if (wordMatches.size == 1) {
            return ContactSearchResult.SingleMatch(wordMatches.first())
        } else if (wordMatches.size > 1) {
            return disambiguateOrGroup(query, wordMatches)
        }

        // Tier 3: Prefix / StartsWith Match (e.g. "Dr. Rahul")
        val prefixMatches = allContacts.filter { contact ->
            val contactNameLower = contact.name.lowercase(Locale.ROOT)
            targetAliases.any { alias ->
                contactNameLower.startsWith(alias) || contactNameLower.contains(" $alias")
            }
        }

        if (prefixMatches.size == 1) {
            return ContactSearchResult.SingleMatch(prefixMatches.first())
        } else if (prefixMatches.size > 1) {
            return disambiguateOrGroup(query, prefixMatches)
        }

        // Tier 4: Substring / Contains Match
        val substringMatches = allContacts.filter { contact ->
            val contactNameLower = contact.name.lowercase(Locale.ROOT)
            targetAliases.any { alias ->
                contactNameLower.contains(alias)
            }
        }

        return when {
            substringMatches.size == 1 -> ContactSearchResult.SingleMatch(substringMatches.first())
            substringMatches.size > 1 -> disambiguateOrGroup(query, substringMatches)
            else -> ContactSearchResult.NotFound(query)
        }
    }

    private fun disambiguateOrGroup(
        targetQuery: String,
        matches: List<ContactItem>
    ): ContactSearchResult {
        // Deduplicate contacts with the exact same name and normalized number
        val distinct = matches.distinctBy { "${it.name}|${it.dialableNumber}" }
        return if (distinct.size == 1) {
            ContactSearchResult.SingleMatch(distinct.first())
        } else {
            ContactSearchResult.AmbiguousMatches(targetQuery, distinct)
        }
    }
}
