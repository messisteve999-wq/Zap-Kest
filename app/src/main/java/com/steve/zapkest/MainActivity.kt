package com.steve.zapkest

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val green = Color.rgb(37, 211, 102)
    private val dark = Color.rgb(16, 21, 18)
    private val card = Color.rgb(28, 36, 31)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); setPadding(dp(4), dp(5), dp(4), dp(5))
    }
    private fun btn(s: String, action: () -> Unit) = Button(this).apply {
        text = s; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(31, 117, 65)); setOnClickListener { action() }
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(dark); setPadding(dp(16), dp(20), dp(16), dp(16)) }
        root.addView(label("ZAP KEST V4", 28f, green))
        root.addView(label("Trois catalogues · Deux fenêtres flottantes maximum · Actualisation toutes les 12 heures.", 15f))
        root.addView(label("QUESTIONS ET MESSAGES", 18f, green))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        QuestionRepository.dailyQuestions().forEach { q ->
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(card); setPadding(dp(10), dp(7), dp(10), dp(7)) }
            item.addView(label(q.category.uppercase(), 11f, green))
            item.addView(label(q.text, 15f))
            item.addView(btn("Copier") { ClipboardHelper.copy(this@MainActivity, q.text) })
            list.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
        val scroll = ScrollView(this); scroll.addView(list); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(btn("Activer les fenêtres flottantes") { enableOverlay() })
        root.addView(btn("Configurer Google Search (API existante)") { configureGoogleSearch() })
        root.addView(btn("Choisir une catégorie") { chooseCategory() })
        setContentView(root)
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val intent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }

    private fun configureGoogleSearch() {
        val prefs = getSharedPreferences("zap_kest", MODE_PRIVATE)
        val key = EditText(this).apply { hint = "Google API key"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setText(prefs.getString("google_search_api_key", "")) }
        val cx = EditText(this).apply { hint = "Programmable Search Engine ID (cx)"; inputType = InputType.TYPE_CLASS_TEXT; setText(prefs.getString("google_search_cx", "")) }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(4)); addView(key); addView(cx) }
        AlertDialog.Builder(this).setTitle("Google Search")
            .setMessage("N'utilise que des identifiants API que tu possèdes déjà et qui ont accès à Custom Search JSON API. Google n'accepte plus de nouveaux clients pour cette API.")
            .setView(layout)
            .setPositiveButton("Enregistrer") { _, _ ->
                GoogleSearchSource.saveSettings(this, key.text.toString(), cx.text.toString())
                Toast.makeText(this, if (GoogleSearchSource.hasSettings(this)) "Identifiants enregistrés sur cet appareil." else "Sans identifiants, Zap Kest garde le catalogue local.", Toast.LENGTH_LONG).show()
            }.setNegativeButton("Annuler", null).show()
    }

    private fun chooseCategory() {
        val cats = QuestionRepository.categories.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choisir une catégorie").setItems(cats) { _, i ->
            GoogleSearchSource.refresh(this, cats[i]) { found ->
                val qs = found.ifEmpty { DailyQuestionEngine.daily(this).filter { it.category == cats[i] } }
                AlertDialog.Builder(this).setTitle(cats[i])
                    .setItems(qs.map { it.text }.toTypedArray()) { _, j -> ClipboardHelper.copy(this, qs[j].text) }
                    .setNegativeButton("Fermer", null).show()
            }
        }.setNegativeButton("Annuler", null).show()
    }
}
