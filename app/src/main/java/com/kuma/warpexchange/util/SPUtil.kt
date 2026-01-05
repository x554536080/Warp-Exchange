package com.kuma.warpexchange.util

import android.content.Context
import com.kuma.warpexchange.MainActivity.Companion.APP_SP_KEY

object SPUtil {

    fun putString(context: Context, key: String, value: String) {
        context.getSharedPreferences(APP_SP_KEY, Context.MODE_PRIVATE).edit()
            .putString(key, value).apply()

    }

    fun getString(context: Context, key: String): String {
        return context.getSharedPreferences(APP_SP_KEY, Context.MODE_PRIVATE).getString(key, "")
            ?: let { "" }
    }

    fun removeString(context: Context, key: String) {
        context.getSharedPreferences(APP_SP_KEY, Context.MODE_PRIVATE).edit().remove(key).apply()
    }

}