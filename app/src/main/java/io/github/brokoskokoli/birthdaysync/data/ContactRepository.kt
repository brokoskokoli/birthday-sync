package io.github.brokoskokoli.birthdaysync.data

import android.accounts.AccountManager
import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts

/** Eine Kontaktquelle (Konto) mit ihren Gruppen/Labels. */
data class ContactSource(
    val id: String,
    val label: String,
    val accountName: String,
    val contactCount: Int,
    val birthdayCount: Int,
    val groups: List<ContactGroup>,
)

data class ContactGroup(
    val id: String,
    val title: String,
    val contactCount: Int,
    val birthdayCount: Int,
)

class ContactRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    private data class Account(val type: String, val name: String)

    /** Alle Konten mit Kontakten und deren Gruppen, inkl. Zähler für Kontakte und Geburtstage. */
    fun loadSources(): List<ContactSource> {
        val contactsByAccount = contactIdsByAccount()
        val birthdayContacts = readBirthdayRows().map { it.contactId }.toSet()
        val members = groupMembers()

        val groupsByAccount = mutableMapOf<Account, MutableList<ContactGroup>>()
        resolver.query(
            Groups.CONTENT_URI,
            arrayOf(Groups._ID, Groups.TITLE, Groups.ACCOUNT_TYPE, Groups.ACCOUNT_NAME),
            "${Groups.DELETED}=0", null, Groups.TITLE,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val ids = members[id].orEmpty()
                if (ids.isEmpty()) continue
                val account = Account(c.getString(2).orEmpty(), c.getString(3).orEmpty())
                groupsByAccount.getOrPut(account) { mutableListOf() } += ContactGroup(
                    id = groupSourceId(id),
                    title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "Gruppe $id",
                    contactCount = ids.size,
                    birthdayCount = ids.count { it in birthdayContacts },
                )
            }
        }

        val labels = authenticatorLabels()
        return contactsByAccount.map { (account, ids) ->
            ContactSource(
                id = accountSourceId(account.type, account.name),
                label = accountLabel(account, labels),
                accountName = account.name.ifEmpty { "Lokal auf dem Gerät" },
                contactCount = ids.size,
                birthdayCount = ids.count { it in birthdayContacts },
                groups = groupsByAccount[account].orEmpty(),
            )
        }.sortedWith(compareBy({ it.label }, { it.accountName }))
    }

    /**
     * Geburtstage aller Kontakte, die in einem ausgewählten Konto liegen oder Mitglied einer
     * ausgewählten Gruppe sind. Pro (zusammengeführtem) Kontakt genau ein Geburtstag.
     */
    fun loadBirthdays(selection: Set<String>): List<Birthday> {
        if (selection.isEmpty()) return emptyList()
        val selectedContacts = mutableSetOf<Long>()
        contactIdsByAccount().forEach { (account, ids) ->
            if (accountSourceId(account.type, account.name) in selection) selectedContacts += ids
        }
        groupMembers().forEach { (groupId, ids) ->
            if (groupSourceId(groupId) in selection) selectedContacts += ids
        }

        return readBirthdayRows()
            .filter { it.contactId in selectedContacts }
            .groupBy { it.contactId }
            .mapNotNull { (_, rows) ->
                // Mehrere Rohkontakte (z. B. Google + Gerät) können je einen Geburtstag haben: einer mit Jahr gewinnt.
                rows.maxByOrNull { if (it.date.year != null) 1 else 0 }
            }
            .map { Birthday(it.contactId, it.lookupKey, it.name, it.date) }
            .sortedBy { it.name }
    }

    private data class BirthdayRow(val contactId: Long, val lookupKey: String, val name: String, val date: BirthDate)

    private fun readBirthdayRows(): List<BirthdayRow> {
        val rows = mutableListOf<BirthdayRow>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data.CONTACT_ID, Data.LOOKUP_KEY, Data.DISPLAY_NAME, Event.START_DATE),
            "${Data.MIMETYPE}=? AND ${Event.TYPE}=?",
            arrayOf(Event.CONTENT_ITEM_TYPE, Event.TYPE_BIRTHDAY.toString()),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val date = BirthDate.parse(c.getString(3)) ?: continue
                val contactId = c.getLong(0)
                rows += BirthdayRow(
                    contactId = contactId,
                    lookupKey = c.getString(1) ?: "id:$contactId",
                    name = c.getString(2)?.takeIf { it.isNotBlank() } ?: "Unbenannt",
                    date = date,
                )
            }
        }
        return rows
    }

    private fun contactIdsByAccount(): Map<Account, Set<Long>> {
        val map = mutableMapOf<Account, MutableSet<Long>>()
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts.CONTACT_ID, RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME),
            "${RawContacts.DELETED}=0", null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.isNull(0)) continue
                map.getOrPut(Account(c.getString(1).orEmpty(), c.getString(2).orEmpty())) { mutableSetOf() } += c.getLong(0)
            }
        }
        return map
    }

    private fun groupMembers(): Map<Long, Set<Long>> {
        val map = mutableMapOf<Long, MutableSet<Long>>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(GroupMembership.GROUP_ROW_ID, Data.CONTACT_ID),
            "${Data.MIMETYPE}=?", arrayOf(GroupMembership.CONTENT_ITEM_TYPE), null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.isNull(0) || c.isNull(1)) continue
                map.getOrPut(c.getLong(0)) { mutableSetOf() } += c.getLong(1)
            }
        }
        return map
    }

    /** Anzeigenamen der Kontotypen, so wie sie das System in den Einstellungen nennt. */
    private fun authenticatorLabels(): Map<String, String> {
        val pm = context.packageManager
        return AccountManager.get(context).authenticatorTypes.mapNotNull { desc ->
            runCatching {
                desc.type to pm.getResourcesForApplication(desc.packageName).getString(desc.labelId)
            }.getOrNull()
        }.toMap()
    }

    private fun accountLabel(account: Account, labels: Map<String, String>): String = when {
        account.type.isEmpty() || account.type in LOCAL_TYPES -> "Gerät"
        account.type == "com.google" -> "Google"
        else -> labels[account.type] ?: account.type
    }

    companion object {
        private const val SEP = '\u001F'
        private val LOCAL_TYPES = setOf(
            "com.android.contacts", "vnd.sec.contact.phone", "com.android.localphone",
            "Local Phone Account", "com.local.contacts", "com.oneplus.contacts",
        )

        fun accountSourceId(type: String, name: String) = "acc:$type$SEP$name"
        fun groupSourceId(groupId: Long) = "grp:$groupId"
    }
}
