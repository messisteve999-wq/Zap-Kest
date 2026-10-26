package com.steve.zapkest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingWindowService : Service() {
    private lateinit var wm:WindowManager
    private var bubble:View?=null
    private var panel:View?=null
    private var current:Question?=null
    private var category:String?=null
    private val green=Color.rgb(37,211,102); private val dark=Color.rgb(16,21,18)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(){super.onCreate();notifyForeground();wm=getSystemService(WINDOW_SERVICE) as WindowManager;showBubble()}
    private fun notifyForeground(){
        val id="zap_kest_overlay"; val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel(id,"Zap Kest",NotificationManager.IMPORTANCE_LOW))
        val b=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,id) else @Suppress("DEPRECATION") Notification.Builder(this)
        startForeground(7001,b.setContentTitle("Zap Kest est actif").setContentText("Touchez la bulle pour une question.").setSmallIcon(android.R.drawable.ic_dialog_info).build())
    }
    private fun lp(w:Int,h:Int,x:Int=dp(16),y:Int=dp(200))=WindowManager.LayoutParams(w,h,if(Build.VERSION.SDK_INT>=26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;this.x=x;this.y=y}
    private fun showBubble(){
        if(bubble!=null)return
        val b=TextView(this).apply{text="ZK";textSize=16f;gravity=Gravity.CENTER;setTextColor(Color.BLACK);setBackgroundColor(green);setPadding(dp(14),dp(12),dp(14),dp(12));elevation=dp(8).toFloat();contentDescription="Ouvrir Zap Kest"}
        val p=lp(dp(56),dp(52)); var downX=0f;var downY=0f;var sx=0;var sy=0;var moved=false
        b.setOnTouchListener { v,e -> when(e.action){MotionEvent.ACTION_DOWN->{downX=e.rawX;downY=e.rawY;sx=p.x;sy=p.y;moved=false;true};MotionEvent.ACTION_MOVE->{val dx=(e.rawX-downX).toInt();val dy=(e.rawY-downY).toInt();if(abs(dx)+abs(dy)>dp(8))moved=true;p.x=sx+dx;p.y=sy+dy;try{wm.updateViewLayout(v,p)}catch(_:Exception){};true};MotionEvent.ACTION_UP->{if(!moved)togglePanel();true};else->false} }
        bubble=b;wm.addView(b,p)
    }
    private fun togglePanel(){ if(panel!=null){try{wm.removeView(panel)}catch(_:Exception){};panel=null}else showPanel() }
    private fun showPanel(){
        current=DailyQuestionEngine.next(this,category)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));setBackgroundColor(dark);elevation=dp(12).toFloat()}
        val title=TextView(this).apply{text="ZAP KEST • QUESTION DU JOUR";textSize=14f;setTextColor(green)}
        val q=TextView(this).apply{text=current?.text.orEmpty();textSize=16f;setTextColor(Color.WHITE);setPadding(0,dp(10),0,dp(10))}
        fun button(s:String,fn:()->Unit)=Button(this).apply{text=s;setOnClickListener{fn()}}
        box.addView(title);box.addView(q)
        box.addView(button("Autre question"){current=DailyQuestionEngine.next(this,category);q.text=current?.text.orEmpty()})
        box.addView(button("Copier"){current?.let{ClipboardHelper.copy(this,it.text)}})
        box.addView(button("Changer de catégorie"){val cs=QuestionRepository.categories.toTypedArray();android.app.AlertDialog.Builder(this).setTitle("Catégorie").setItems(cs){_,i->category=cs[i];current=DailyQuestionEngine.next(this,category);q.text=current?.text.orEmpty()}.show()})
        box.addView(button("Fermer le panneau"){togglePanel()})
        panel=box;try{wm.addView(box,lp(dp(300),-2,dp(10),dp(270)))}catch(_:Exception){panel=null}
    }
    override fun onDestroy(){try{bubble?.let{wm.removeView(it)}}catch(_:Exception){};try{panel?.let{wm.removeView(it)}}catch(_:Exception){};bubble=null;panel=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
