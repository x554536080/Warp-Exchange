package com.kuma.warpexchange.util

import java.util.Calendar
import java.util.Date

object CookieUtil {

    fun getCookieExpires(cookie: String): Date? {
        val parts = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (part in parts) {
            val c = Calendar.getInstance().time
            if (part.trim { it <= ' ' }.startsWith("Expires")) {
                val expiresParts = part.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val expires = expiresParts[1].trim { it <= ' ' }
                return Date(expires)
            }
        }
        return null
    }
}