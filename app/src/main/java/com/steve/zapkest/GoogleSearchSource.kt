package com.steve.zapkest

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

/**
 * Uses Google's Custom Search JSON API only when the user already has authorized API access.
 * The API is closed to new customers, so the app always falls back to its local catalog.
 */
object GoogleSearchSource {
    private const val PREFS = "zap_kest"
    private const val KEY_API = "google_search_api_key"
    private const val KEY_CX = "google_search_cx"
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun saveSettings(context: Context, apiKey: String, cx: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_API, apiKey.trim())
            .putString(KEY_CX, cx.trim())
            .apply()
    }

    fun hasSettings(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return !p.getString(KEY_API, "").isNullOrBlank() && !p.getString(KEY_CX, "").isNullOrBlank()
    }

    fun refresh(context: Context, category: String, callback: (List<Question>) -> Unit) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(KEY_API, "").orEmpty()
        val cx = prefs.getString(KEY_CX, "").orEmpty()
        if (apiKey.isBlank() || cx.isBlank()) {
            main.post { callback(emptyList()) }
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null
            val found = mutableListOf<Question>()
            try {
                val searchTerms = when (category) {
                    QuestionRepository.ROMANCE -> "compliments respectueux romantisme conversation"
                    QuestionRepository.CONNAISSANCE -> "questions ouvertes pour faire connaissance conversation"
                    else -> "questions conversation couple relation communication"
                }
                val query = URLEncoder.encode(searchTerms, "UTF-8")
                val url = URL("https://www.googleapis.com/customsearch/v1?key=${URLEncoder.encode(apiKey, "UTF-8")}&cx=${URLEncoder.encode(cx, "UTF-8")}&q=$query&num=10&safe=active")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                if (connection.responseCode == 200) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val title = item.optString("title").trim()
                        if (title.isBlank()) continue
                        val text = when (category) {
                            QuestionRepository.ROMANCE -> "J'ai trouvé ce sujet intéressant : « $title ». Quel est ton avis ?"
                            QuestionRepository.CONNAISSANCE -> "J'aimerais connaître ton point de vue sur « $title ». Qu'en penses-tu ?"
                            else -> "Ça te dirait qu'on discute ensemble de « $title » ?"
                        }
                        found.add(Question("web_${category.hashCode()}_$i", category, 1, text))
                    }
                    if (found.isNotEmpty()) {
                        val arr = JSONArray()
                        found.forEach { q ->
                            arr.put(JSONObject().put("id", q.id).put("category", q.category).put("level", q.level).put("text", q.text))
                        }
                        prefs.edit().putString("google_cache_${category.hashCode()}", arr.toString()).apply()
                    }
                }
            } catch (_: Exception) {
                // Network/API errors leave the previous cache and local catalog intact.
            } finally {
                connection?.disconnect()
            }
            main.post { callback(found) }
        }
    }

    fun cached(context: Context, category: String): List<Question> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("google_cache_${category.hashCode()}", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Question(o.optString("id"), o.optString("category"), o.optInt("level", 1), o.optString("text"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
