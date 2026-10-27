package com.steve.zapkest

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object GeminiAssistant {
    private const val PREFS = "zap_kest"
    private const val KEY = "gemini_api_key"
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun saveKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, key.trim()).apply()
    }

    fun hasKey(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "").isNullOrBlank()

    fun generate(
        context: Context,
        conversation: String,
        callback: (String) -> Unit
    ) {
        val key = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty()

        if (key.isBlank()) {
            main.post {
                callback("Configure d'abord ta clé API Gemini dans Zap Kest.")
            }
            return
        }

        executor.execute {
            var result = "Gemini n'a pas pu répondre. Vérifie Internet."
            var connection: HttpURLConnection? = null

            try {
                val url = URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
                )

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty(
                        "Content-Type",
                        "application/json; charset=utf-8"
                    )
                }

                val prompt = """
                    Tu es AutoZap, un assistant de messagerie.
                    Propose une réponse courte, naturelle, humaine,
                    respectueuse et adaptée au contexte en français.
                    Retourne uniquement le texte de la réponse.
                    Conversation récente :
                    $conversation
                """.trimIndent()

                val body = JSONObject()
                    .put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", prompt)
                                )
                            )
                        )
                    )
                    .put(
                        "generationConfig",
                        JSONObject()
                            .put("temperature", 0.7)
                            .put("maxOutputTokens", 180)
                    )
                    .put(
                        "tools",
                        JSONArray().put(
                            JSONObject().put("google_search", JSONObject())
                        )
                    )

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                }.orEmpty()

                if (code in 200..299) {
                    result = JSONObject(response)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                        .trim()
                        .ifBlank { result }
                } else {
                    result = when (code) {
                        400, 403 -> "Clé Gemini refusée. Vérifie ta clé API."
                        429 -> "Limite Gemini atteinte. Réessaie plus tard."
                        else -> "Erreur Gemini ($code)."
                    }
                }
            } catch (_: Exception) {
                result = "Connexion à Gemini impossible. Vérifie Internet."
            } finally {
                connection?.disconnect()
            }

            main.post { callback(result) }
        }
    }
}
