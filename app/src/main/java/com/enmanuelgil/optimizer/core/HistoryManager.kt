package com.enmanuelgil.optimizer.core

import android.content.Context
import com.enmanuelgil.optimizer.model.OptimizationRecord
import org.json.JSONArray
import org.json.JSONObject

object HistoryManager {
    private const val PREFS_NAME = "optimizer_history"
    private const val KEY_RECORDS = "records"
    private const val MAX_RECORDS = 30

    fun save(context: Context, record: OptimizationRecord) {
        val existing = load(context).toMutableList()
        existing.add(0, record)
        if (existing.size > MAX_RECORDS) existing.removeAt(existing.size - 1)
        val array = JSONArray()
        existing.forEach { r ->
            array.put(JSONObject().apply {
                put("ts", r.timestamp)
                put("profile", r.profileName)
                put("ram", r.ramFreedMb)
                put("killed", r.appsKilled)
                put("tempBefore", r.temperatureBefore.toDouble())
                put("tempAfter", r.temperatureAfter.toDouble())
                put("actions", r.actionCount)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    fun load(context: Context): List<OptimizationRecord> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECORDS, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                OptimizationRecord(
                    timestamp = o.getLong("ts"),
                    profileName = o.getString("profile"),
                    ramFreedMb = o.getLong("ram"),
                    appsKilled = o.getInt("killed"),
                    temperatureBefore = o.getDouble("tempBefore").toFloat(),
                    temperatureAfter = o.getDouble("tempAfter").toFloat(),
                    actionCount = o.getInt("actions")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RECORDS).apply()
    }
}
