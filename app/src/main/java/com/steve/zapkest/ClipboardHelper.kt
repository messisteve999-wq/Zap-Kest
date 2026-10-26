package com.steve.zapkest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
object ClipboardHelper {
    fun copy(context: Context, text: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Question Zap Kest", text))
        Toast.makeText(context, "Question copiée. Colle-la dans ta conversation.", Toast.LENGTH_SHORT).show()
    }
}
