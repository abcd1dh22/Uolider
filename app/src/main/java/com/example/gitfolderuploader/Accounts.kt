package com.example.gitfolderuploader

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/** একাধিক GitHub অ্যাকাউন্টের টোকেন (এনক্রিপ্টেড স্টোরেজে JSON তালিকা হিসেবে) */
object Accounts {
    private const val KEY = "accounts"

    fun securePrefs(ctx: Context): SharedPreferences? = try {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        null
    }

    fun list(sp: SharedPreferences?): List<Pair<String, String>> {
        val s = sp?.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                o.getString("name") to o.getString("token")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun write(sp: SharedPreferences?, items: List<Pair<String, String>>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("name", it.first).put("token", it.second)) }
        sp?.edit()?.putString(KEY, arr.toString())?.apply()
    }

    fun save(sp: SharedPreferences?, name: String, token: String) {
        write(sp, list(sp).filter { it.first != name } + (name to token))
    }

    fun remove(sp: SharedPreferences?, name: String) {
        write(sp, list(sp).filter { it.first != name })
    }

    fun removeByToken(sp: SharedPreferences?, token: String) {
        write(sp, list(sp).filter { it.second != token })
    }

    fun clear(sp: SharedPreferences?) {
        sp?.edit()?.remove(KEY)?.apply()
    }
}
