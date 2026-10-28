package com.steve.zapkest

import android.content.Context

object AutoZapSettings {
    private const val PREFS = "zap_kest"
    private const val ALLOWED = "autozap_allowed_contacts_v2"
    private const val AUTOMATIC = "autozap_automatic_contacts_v2"
    private const val SEP = "\u001f"
    private const val MAX_PER_APP = 4

    private fun key(packageName: String, sender: String) =
        "$packageName$SEP$sender"

    private fun entries(context: Context, name: String): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (prefs.getStringSet(name, emptySet()) ?: emptySet()).toMutableSet()
    }

    @Synchronized
    fun isAllowed(context: Context, packageName: String, sender: String): Boolean =
        entries(context, ALLOWED).contains(key(packageName, sender))

    @Synchronized
    fun isAutomatic(context: Context, packageName: String, sender: String): Boolean =
        isAllowed(context, packageName, sender) &&
            entries(context, AUTOMATIC).contains(key(packageName, sender))

    @Synchronized
    fun allowedCount(context: Context, packageName: String): Int {
        val prefix = "$packageName$SEP"
        return entries(context, ALLOWED).count { it.startsWith(prefix) }
    }

    @Synchronized
    fun setAllowed(
        context: Context,
        packageName: String,
        sender: String,
        allowed: Boolean
    ): Boolean {
        val contactKey = key(packageName, sender)
        val allowedSet = entries(context, ALLOWED)
        val automaticSet = entries(context, AUTOMATIC)

        if (allowed) {
            if (!allowedSet.contains(contactKey) &&
                allowedCount(context, packageName) >= MAX_PER_APP
            ) return false

            allowedSet.add(contactKey)
        } else {
            allowedSet.remove(contactKey)
            automaticSet.remove(contactKey)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(ALLOWED, allowedSet)
            .putStringSet(AUTOMATIC, automaticSet)
            .apply()
        return true
    }

    @Synchronized
    fun setAutomatic(
        context: Context,
        packageName: String,
        sender: String,
        enabled: Boolean
    ): Boolean {
        if (!isAllowed(context, packageName, sender)) return false

        val automaticSet = entries(context, AUTOMATIC)
        val contactKey = key(packageName, sender)

        if (enabled) automaticSet.add(contactKey)
        else automaticSet.remove(contactKey)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(AUTOMATIC, automaticSet)
            .apply()
        return true
    }
}
