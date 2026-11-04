package com.example.godsmino.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object GameStatsManager {
    private const val PREFS_NAME = "godsmino_stats"
    private const val KEY_WINS = "wins"
    private const val KEY_LOSSES = "losses"
    private const val KEY_HIGH_SCORE = "high_score"
    private const val KEY_MATCHES = "total_matches"
    private const val KEY_HISTORY = "match_history"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun recordMatch(context: Context, isWin: Boolean, score: Int, opponentName: String = "Bot (IA)", rounds: Int = 1) {
        val prefs = getPrefs(context)
        val wins = prefs.getInt(KEY_WINS, 0)
        val losses = prefs.getInt(KEY_LOSSES, 0)
        val matches = prefs.getInt(KEY_MATCHES, 0)
        val highScore = prefs.getInt(KEY_HIGH_SCORE, 0)

        val historyJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val jsonArray = try { JSONArray(historyJson) } catch (_: Exception) { JSONArray() }

        val newMatch = JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("opponentName", opponentName)
            put("isWin", isWin)
            put("score", score)
            put("rounds", rounds)
            put("timestamp", System.currentTimeMillis())
        }

        // Add to front of array (most recent first)
        val newArray = JSONArray()
        newArray.put(newMatch)
        for (i in 0 until minOf(jsonArray.length(), 19)) { // keep last 20 matches
            newArray.put(jsonArray.get(i))
        }

        prefs.edit().apply {
            if (isWin) putInt(KEY_WINS, wins + 1) else putInt(KEY_LOSSES, losses + 1)
            putInt(KEY_MATCHES, matches + 1)
            if (score > highScore) putInt(KEY_HIGH_SCORE, score)
            putString(KEY_HISTORY, newArray.toString())
            apply()
        }
    }

    fun getStats(context: Context): Stats {
        val prefs = getPrefs(context)
        val historyJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val jsonArray = try { JSONArray(historyJson) } catch (_: Exception) { JSONArray() }

        val history = mutableListOf<MatchRecord>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            history.add(
                MatchRecord(
                    id = obj.optString("id", i.toString()),
                    opponentName = obj.optString("opponentName", "Oponente"),
                    isWin = obj.optBoolean("isWin", false),
                    score = obj.optInt("score", 0),
                    rounds = obj.optInt("rounds", 1),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        return Stats(
            wins = prefs.getInt(KEY_WINS, 0),
            losses = prefs.getInt(KEY_LOSSES, 0),
            totalMatches = prefs.getInt(KEY_MATCHES, 0),
            highScore = prefs.getInt(KEY_HIGH_SCORE, 0),
            history = history
        )
    }

    data class MatchRecord(
        val id: String,
        val opponentName: String,
        val isWin: Boolean,
        val score: Int,
        val rounds: Int,
        val timestamp: Long
    ) {
        fun getFormattedTime(): String {
            val diffMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
            return when {
                diffMinutes < 1 -> "Hace un momento"
                diffMinutes < 60 -> "Hace $diffMinutes min"
                diffMinutes < 1440 -> "Hace ${diffMinutes / 60} h"
                else -> "Hace ${diffMinutes / 1440} días"
            }
        }
    }

    data class Stats(
        val wins: Int,
        val losses: Int,
        val totalMatches: Int,
        val highScore: Int,
        val history: List<MatchRecord>
    ) {
        val winrate: Int
            get() = if (totalMatches > 0) (wins * 100) / totalMatches else 0
    }
}
