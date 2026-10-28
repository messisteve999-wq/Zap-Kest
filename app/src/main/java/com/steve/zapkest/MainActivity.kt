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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 12, 20))
            setPadding(dp(18), dp(18), dp(18), dp(12))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun space(height: Int) {
            content.addView(
                TextView(this).apply { text = "" },
                LinearLayout.LayoutParams(-1, dp(height))
            )
        }

        fun section(text: String) {
            content.addView(TextView(this).apply {
                this.text = text
                textSize = 13f
                setTextColor(Color.rgb(166, 174, 204))
                setPadding(dp(2), dp(8), dp(2), dp(8))
                letterSpacing = 0.08f
            })
        }

        fun panel(title: String, subtitle: String, symbol: String) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.rgb(25, 29, 45))
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.rgb(47, 53, 77))
                }
            }
            box.addView(TextView(this).apply {
                text = "$symbol   $title"
                textSize = 17f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            box.addView(TextView(this).apply {
                text = subtitle
                textSize = 13f
                setTextColor(Color.rgb(180, 187, 210))
                setPadding(0, dp(6), 0, 0)
            })
            content.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            })
        }

        content.addView(TextView(this).apply {
            text = "♛"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
            }
        }, LinearLayout.LayoutParams(dp(62), dp(62)))

        space(12)

        content.addView(TextView(this).apply {
            text = "ZAP KEST"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.04f
        })

        content.addView(TextView(this).apply {
            text = "V7  •  STEVE AI • GEMINI"
            textSize = 12f
            setTextColor(Color.rgb(165, 133, 255))
            setPadding(0, dp(5), 0, dp(4))
        })

        content.addView(TextView(this).apply {
            text = "Tes idées, tes messages et ton assistant au même endroit."
            textSize = 14f
            setTextColor(Color.rgb(184, 190, 210))
        })

        space(18)
        panel("AutoZap", "Réponses automatiques pour les contacts autorisés et configurés.", "⚡")
        panel("SugesZap", "Suggestions Gemini modifiables avant envoi.", "✦")
        panel("Fenêtre flottante", "Accède à ton assistant pendant que tu utilises d'autres applications.", "▣")

        section("ACCÈS RAPIDE")

        fun actionButton(
            title: String,
            subtitle: String,
            color: Int = Color.rgb(124, 92, 255),
            action: () -> Unit
        ) {
            val b = Button(this).apply {
                text = "$title\n$subtitle"
                textSize = 13f
                isAllCaps = false
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = dp(14).toFloat()
                }
                setOnClickListener { action() }
            }
            content.addView(b, LinearLayout.LayoutParams(-1, dp(62)).apply {
                bottomMargin = dp(9)
            })
        }

        actionButton("Ouvrir les fenêtres flottantes", "Lancer l'assistant") {
            enableOverlay()
        }

        actionButton(
            "Configurer Gemini et la recherche IA",
            "Suggestions de messages et recherche assistée",
            Color.rgb(43, 49, 70)
        ) {
            configureGemini()
        }

        actionButton(
            "Explorer les catégories",
            "Découvrir les questions du jour",
            Color.rgb(43, 49, 70)
        ) {
            chooseCategory()
        }

        section("MESSAGERIES • AUTOZAP / SUGESZAP")

        actionButton(
            "Autoriser l'accès aux notifications",
            "Détecter les messages compatibles",
            Color.rgb(43, 49, 70)
        ) {
            AutoZapNotificationListener.openNotificationAccess(this)
        }

        actionButton(
            "Autoriser la fenêtre flottante",
            "Afficher Steve au-dessus des applications",
            Color.rgb(43, 49, 70)
        ) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                Toast.makeText(this, "Autorisation déjà active", Toast.LENGTH_SHORT).show()
            }
        }

        panel(
            "Contacts autorisés",
            "Dans la fenêtre AutoZap, autorise chaque contact reçu par notification. Maximum : 4 par application. Active le mode automatique uniquement pour les contacts souhaités.",
            "♙"
        )

        section("QUESTIONS DU JOUR")

        QuestionRepository.dailyQuestions().forEach { q ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.rgb(23, 27, 41))
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.rgb(43, 48, 68))
                }
            }

            item.addView(TextView(this).apply {
                text = q.category.uppercase()
                textSize = 11f
                setTextColor(Color.rgb(165, 133, 255))
            })

            item.addView(TextView(this).apply {
                text = q.text
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(0, dp(7), 0, dp(8))
            })

            val copy = Button(this).apply {
                text = "Copier la question"
                isAllCaps = false
                setOnClickListener {
                    ClipboardHelper.copy(this@MainActivity, q.text)
                }
            }
            item.addView(copy)

            content.addView(item, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            })
        }

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(TextView(this).apply {
            text = "ZAP KEST  •  SIMPLE, RAPIDE, INTELLIGENT"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(135, 143, 169))
            setPadding(0, dp(12), 0, dp(4))
        })

        window.statusBarColor = Color.rgb(10, 12, 20)
        window.navigationBarColor = Color.rgb(10, 12, 20)
        window.decorView.systemUiVisibility = 0

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


    private fun configureGemini() {
        val prefs = getSharedPreferences("zap_kest", MODE_PRIVATE)
        val key = EditText(this).apply {
            hint = "Clé API Gemini"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("gemini_api_key", ""))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(key)
        }

        AlertDialog.Builder(this)
            .setTitle("Gemini — AutoZap")
            .setMessage(
                "Entre ta clé API Gemini. Tu peux en obtenir une sur Google AI Studio."
            )
            .setView(layout)
            .setPositiveButton("Enregistrer") { _, _ ->
                GeminiAssistant.saveKey(this, key.text.toString())
                Toast.makeText(
                    this,
                    if (GeminiAssistant.hasKey(this))
                        "Clé Gemini enregistrée"
                    else "Clé effacée",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
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
