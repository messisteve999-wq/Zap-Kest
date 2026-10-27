package com.steve.zapkest

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

object DailyQuestionEngine {
    private fun dayKey(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    fun daily(context: Context): List<Question> {
        val today = dayKey()
        val prefs = context.getSharedPreferences("zap_kest", Context.MODE_PRIVATE)
        if (prefs.getLong("daily_date", Long.MIN_VALUE) != today) {
            prefs.edit().putLong("daily_date", today).apply()
        }
        return QuestionRepository.categories.flatMap { category ->
            GoogleSearchSource.cached(context, category).ifEmpty { QuestionRepository.questionsFor(category, today) }
        }
    }

    fun next(context: Context, category: String? = null): Question {
        val questions = if (category != null) {
            GoogleSearchSource.cached(context, category).ifEmpty { QuestionRepository.questionsFor(category, dayKey()) }
        } else daily(context)
        val safeQuestions = questions.ifEmpty { QuestionRepository.dailyQuestions(dayKey()) }
        val prefs = context.getSharedPreferences("zap_kest", Context.MODE_PRIVATE)
        val key = "cursor_${dayKey()}_${category ?: "all"}"
        val index = prefs.getInt(key, 0).coerceAtLeast(0) % safeQuestions.size
        prefs.edit().putInt(key, index + 1).apply()
        return safeQuestions[index]
    }
}
