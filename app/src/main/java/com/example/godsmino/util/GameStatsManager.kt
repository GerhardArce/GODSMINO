package com.example.godsmino.util

import android.content.Context
import android.content.SharedPreferences

object GameStatsManager {
    private const val PREFS_NAME = "godsmino_stats"
    private const val KEY_WINS = "wins"
    private const val KEY_LOSSES = "losses"
    private const val KEY_HIGH_SCORE = "high_score"
    private const val KEY_MATCHES = "total_matches"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun recordMatch(context: Context, isWin: Boolean, score: Int) {
        val prefs = getPrefs(context)
        val wins = prefs.getInt(KEY_WINS, 0)
        val losses = prefs.getInt(KEY_LOSSES, 0)
        val matches = prefs.getInt(KEY_MATCHES, 0)
        val highScore = prefs.getInt(KEY_HIGH_SCORE, 0)

        prefs.edit().apply {
            if (isWin) putInt(KEY_WINS, wins + 1) else putInt(KEY_LOSSES, losses + 1)
            putInt(KEY_MATCHES, matches + 1)
            if (score > highScore) putInt(KEY_HIGH_SCORE, score)
            apply()
        }
    }

    fun getStats(context: Context): Stats {
        val prefs = getPrefs(context)
        return Stats(
            wins = prefs.getInt(KEY_WINS, 0),
            losses = prefs.getInt(KEY_LOSSES, 0),
            totalMatches = prefs.getInt(KEY_MATCHES, 0),
            highScore = prefs.getInt(KEY_HIGH_SCORE, 0)
        )
    }

    data class Stats(val wins: Int, val losses: Int, val totalMatches: Int, val highScore: Int)
}
