package com.steve.zapkest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class FloatingWindowService : Service() {
    companion object { private const val MAX_WINDOWS = 2; private const val NOTIFICATION_ID = 7001 }
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private val panels = mutableListOf<Panel>()
    private val handler = Handler(Looper.getMainLooper())
    private val green = Color.rgb(37, 211, 102)
    private val dark = Color.rgb(16, 21, 18)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private data class Panel(
        val root: LinearLayout,
        val params: WindowManager.LayoutParams,
        val categoryLabel: TextView,
        val questionLabel: TextView,
        var category: String,
        var current: Question
    )

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        notifyForeground()
        showBubble()
        handler.postDelayed(refreshRunnable, 112 * 60 * 60 * 1000L)
    }

    private fun notifyForeground() {
        val id = "zap_kest_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(id, "Zap Kest", NotificationManager.IMPORTANCE_LOW))
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, id) else @Suppress("DEPRECATION") Notification.Builder(this)
        startForeground(NOTIFICATION_ID, builder.setContentTitle("Zap Kest est actif")
            .setContentText("Jusqu'à deux fenêtres flottantes.").setSmallIcon(android.R.drawable.ic_dialog_info).build())
    }

    private fun params(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; this.x = x; this.y = y }

    private fun showBubble() {
        if (bubble != null) return
        val b = TextView(this).apply {
            text = "♛ SM"; textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK); setPadding(dp(10), dp(10), dp(10), dp(10)); elevation = dp(8).toFloat()
            contentDescription = "Ouvrir une fenêtre Zap Kest"
        }
        val p = params(dp(64), dp(52), dp(12), dp(190))
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        b.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = p.x; startY = p.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt(); val dy = (e.rawY - downY).toInt()
                    if (abs(dx) + abs(dy) > dp(8)) moved = true
                    p.x = startX + dx; p.y = startY + dy
                    try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) openPanel(); true }
                else -> false
            }
        }
        bubble = b
        wm.addView(b, p)
    }

    private fun openPanel() {
        if (panels.size >= MAX_WINDOWS) {
            Toast.makeText(this, "Deux fenêtres sont déjà ouvertes. Ferme-en une pour en ouvrir une autre.", Toast.LENGTH_SHORT).show()
            return
        }
        val category = QuestionRepository.categories[panels.size % QuestionRepository.categories.size]
        val current = DailyQuestionEngine.next(this, category)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(dark); elevation = dp(12).toFloat()
        }
        val header = TextView(this).apply { text = "ZAP KEST • FENÊTRE ${panels.size + 1}"; textSize = 14f; setTextColor(green) }
        val categoryLabel = TextView(this).apply { text = category; textSize = 12f; setTextColor(green); setPadding(0, dp(6), 0, dp(4)) }
        val questionLabel = TextView(this).apply { text = current.text; textSize = 16f; setTextColor(Color.WHITE); setPadding(0, dp(8), 0, dp(8)) }
        fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() } }
        box.addView(header); box.addView(categoryLabel); box.addView(questionLabel)
        val state = Panel(box, params(dp(300), -2, dp(10 + panels.size * 18), dp(260 + panels.size * 45)), categoryLabel, questionLabel, category, current)
        box.addView(button("Question / message suivant") { nextQuestion(state) })
        box.addView(button("Copier") { ClipboardHelper.copy(this, state.current.text) })
        box.addView(button("Changer de catégorie") { chooseCategory(state) })
        box.addView(button("Fermer cette fenêtre") { closePanel(state) })
        try {
            wm.addView(box, state.params)
            panels.add(state)
        } catch (_: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir la fenêtre. Vérifie l'autorisation de superposition.", Toast.LENGTH_LONG).show()
        }
    }

    private fun nextQuestion(state: Panel) {
        state.current = DailyQuestionEngine.next(this, state.category)
        state.questionLabel.text = state.current.text
    }

    private fun chooseCategory(state: Panel) {
        val cats = QuestionRepository.categories
        val index = cats.indexOf(state.category).coerceAtLeast(0)
        state.category = cats[(index + 1) % cats.size]
        state.categoryLabel.text = state.category
        state.current = DailyQuestionEngine.next(this, state.category)
        state.questionLabel.text = state.current.text
        Toast.makeText(this, "Catégorie : ${state.category}", Toast.LENGTH_SHORT).show()
        GoogleSearchSource.refresh(this, state.category) { found ->
            if (panels.contains(state) && found.isNotEmpty()) {
                state.current = found.first()
                state.questionLabel.text = state.current.text
            }
        }
    }

    private fun closePanel(state: Panel) {
        try { wm.removeView(state.root) } catch (_: Exception) {}
        panels.remove(state)
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            panels.toList().forEach { state ->
                GoogleSearchSource.refresh(this@FloatingWindowService, state.category) { found ->
                    if (panels.contains(state)) {
                        state.current = found.firstOrNull() ?: DailyQuestionEngine.next(this@FloatingWindowService, state.category)
                        state.questionLabel.text = state.current.text
                    }
                }
            }
            handler.postDelayed(this, 112 * 60 * 60 * 1000L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        panels.toList().forEach { try { wm.removeView(it.root) } catch (_: Exception) {} }
        panels.clear()
        try { bubble?.let { wm.removeView(it) } } catch (_: Exception) {}
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
