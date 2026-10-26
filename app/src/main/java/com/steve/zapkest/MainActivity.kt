package com.steve.zapkest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val green = Color.rgb(37, 211, 102)
    private val dark = Color.rgb(16, 21, 18)
    private val card = Color.rgb(28, 36, 31)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s:String, size:Float, color:Int=Color.WHITE) = TextView(this).apply { text=s; textSize=size; setTextColor(color); setPadding(dp(4),dp(5),dp(4),dp(5)) }
    private fun btn(s:String, action:()->Unit) = Button(this).apply { text=s; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(31,117,65)); setOnClickListener { action() } }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun render() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(dark); setPadding(dp(16),dp(20),dp(16),dp(16)) }
        root.addView(label("ZAP KEST",28f,green))
        root.addView(label("Des questions naturelles pour faire connaissance et créer une complicité, sans pression.",16f))
        root.addView(label("QUESTIONS DU JOUR",18f,green))
        val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        QuestionRepository.dailyQuestions().forEach { q ->
            val item = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(card); setPadding(dp(10),dp(7),dp(10),dp(7)) }
            item.addView(label(q.category.uppercase(),11f,green)); item.addView(label(q.text,15f)); item.addView(btn("Copier la question") { ClipboardHelper.copy(this@MainActivity,q.text) })
            list.addView(item,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(9) })
        }
        val scroll=ScrollView(this); scroll.addView(list); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(btn("Activer la fenêtre flottante") { enableOverlay() })
        root.addView(btn("Choisir une catégorie") { chooseCategory() })
        setContentView(root)
    }
    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return }
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this,FloatingWindowService::class.java)) else startService(Intent(this,FloatingWindowService::class.java))
    }
    private fun chooseCategory() {
        val cats=QuestionRepository.categories.toTypedArray()
        android.app.AlertDialog.Builder(this).setTitle("Choisir une catégorie").setItems(cats) { _,i ->
            val qs=QuestionRepository.questionsFor(cats[i]); android.app.AlertDialog.Builder(this).setTitle(cats[i]).setItems(qs.map { it.text }.toTypedArray()) { _,j -> ClipboardHelper.copy(this,qs[j].text) }.setNegativeButton("Fermer",null).show()
        }.setNegativeButton("Annuler",null).show()
    }
}
