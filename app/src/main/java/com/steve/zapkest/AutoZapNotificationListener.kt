package com.steve.zapkest

import android.app.Notification
import android.app.RemoteInput
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class AutoZapNotificationListener : NotificationListenerService() {

    data class Message(
        val app: String,
        val sender: String,
        val text: String,
        val time: Long
    )

    companion object {
        private const val PREFS = "zap_kest"
        private const val MAX_MESSAGES = 20
        private val recent = ArrayList<Message>()

        @Synchronized
        private fun addMessage(message: Message) {
            recent.add(message)
            while (recent.size > MAX_MESSAGES) recent.removeAt(0)
        }

        @Synchronized
        fun messages(): List<Message> = recent.toList()

        fun copyRecent(context: Context): Int {
            val snapshot = messages().takeLast(MAX_MESSAGES)
            if (snapshot.isEmpty()) return 0

            val content = snapshot.joinToString("\n") {
                "${it.sender}: ${it.text}"
            }

            val clipboard =
                context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText("AutoZap - messages", content)
            )

            return snapshot.size
        }

        fun openNotificationAccess(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        fun autoReplyEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("autozap_auto_reply", false)

        fun setAutoReply(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("autozap_auto_reply", enabled).apply()
        }
    }

    private var windowManager: WindowManager? = null
    private var overlay: LinearLayout? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()?.trim().orEmpty()

        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ).orEmpty().trim()

        if (sender.isBlank() || text.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        addMessage(Message(appName, sender, text, sbn.postTime))

        val replyAction = notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        if (prefs.getBoolean("autozap_overlay_enabled", true) &&
            Build.VERSION.SDK_INT >= 23 &&
            Settings.canDrawOverlays(this)
        ) {
            showAutoZap(appName, sender, text, replyAction)
        }

        if (autoReplyEnabled(this) && replyAction != null) {
            val conversation = messages().takeLast(8).joinToString("\n") {
                "${it.sender}: ${it.text}"
            }

            GeminiAssistant.generate(this, conversation) { answer ->
                if (answer.isNotBlank() &&
                    !answer.startsWith("Configure d'abord") &&
                    !answer.startsWith("Connexion") &&
                    !answer.startsWith("Erreur") &&
                    !answer.startsWith("Limite") &&
                    !answer.startsWith("Clé Gemini")
                ) {
                    sendReply(replyAction, answer)
                }
            }
        }
    }

    private fun showAutoZap(
        appName: String,
        sender: String,
        message: String,
        action: Notification.Action?
    ) {
        try {
            val manager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager = manager
            removeOverlay()

            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(Color.WHITE)
                elevation = dp(10).toFloat()
            }

            root.addView(TextView(this).apply {
                text = "S  •  AUTOZAP"
                textSize = 17f
                setTextColor(Color.BLACK)
            })

            root.addView(TextView(this).apply {
                text = "$appName · $sender\n$message"
                textSize = 13f
                setTextColor(Color.DKGRAY)
                maxLines = 5
                setPadding(0, dp(6), 0, dp(6))
            })

            val suggestion = TextView(this).apply {
                text = "Préparation de la suggestion Gemini…"
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(0, dp(5), 0, dp(8))
            }
            root.addView(suggestion)

            fun addButton(label: String, click: () -> Unit) {
                root.addView(Button(this).apply {
                    text = label
                    setTextColor(Color.BLACK)
                    setOnClickListener { click() }
                })
            }

            addButton("Copier la suggestion") {
                val answer = suggestion.text.toString()
                if (!answer.startsWith("Préparation")) {
                    val clipboard =
                        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("AutoZap", answer)
                    )
                    Toast.makeText(
                        this, "Suggestion copiée", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (action != null) {
                addButton("Envoyer la réponse") {
                    val answer = suggestion.text.toString()
                    if (!answer.startsWith("Préparation") &&
                        !answer.startsWith("Configure d'abord") &&
                        !answer.startsWith("Connexion") &&
                        !answer.startsWith("Erreur")
                    ) {
                        sendReply(action, answer)
                    }
                }
            }

            addButton("Copier jusqu'à 20 messages") {
                val count = copyRecent(this)
                Toast.makeText(
                    this,
                    if (count > 0) "$count messages copiés"
                    else "Aucun message disponible",
                    Toast.LENGTH_SHORT
                ).show()
            }

            addButton("Fermer AutoZap") { removeOverlay() }

            val type = if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                dp(300),
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dp(8)
                y = dp(150)
            }

            manager.addView(root, params)
            overlay = root

            val conversation = messages().takeLast(8).joinToString("\n") {
                "${it.sender}: ${it.text}"
            }

            GeminiAssistant.generate(
                this,
                conversation.ifBlank { "$sender: $message" }
            ) { answer ->
                if (overlay === root) suggestion.text = answer
            }
        } catch (_: Exception) {
            // L'accès aux fenêtres flottantes peut être désactivé par Android.
        }
    }

    private fun sendReply(action: Notification.Action, answer: String) {
        try {
            val inputs = action.remoteInputs ?: return
            val intent = Intent()
            val results = Bundle()

            inputs.filter { it.allowFreeFormInput }.forEach {
                results.putCharSequence(it.resultKey, answer)
            }

            RemoteInput.addResultsToIntent(inputs, intent, results)
            action.actionIntent.send(this, 0, intent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Réponse directe non prise en charge par cette application.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun removeOverlay() {
        try {
            overlay?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        overlay = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
