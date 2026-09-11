package com.hoctin.gymtracker.data.wearable

import java.security.MessageDigest

internal object WearableHashing {
    fun sha256(vararg fields: Any?): String {
        val canonical = fields.joinToString(separator = "") { field ->
            if (field == null) {
                "-1:"
            } else {
                val value = field.toString()
                "${value.length}:$value"
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
