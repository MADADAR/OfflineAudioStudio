package com.madadar.offlineaudio

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private var player: MediaPlayer? = null
    private var selected: Uri? = null
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,48,32,32) }
        status = TextView(this).apply { text = "اختر ملفًا صوتيًا من هاتفك"; textSize = 20f }
        layout.addView(status)
        layout.addView(Button(this).apply { text = "اختيار ملف صوتي"; setOnClickListener { pickAudio() } })
        layout.addView(Button(this).apply { text = "تشغيل"; setOnClickListener { playAudio() } })
        layout.addView(Button(this).apply { text = "إيقاف"; setOnClickListener { player?.pause() } })
        layout.addView(Button(this).apply { text = "إعادة التشغيل"; setOnClickListener { player?.seekTo(0); player?.start() } })
        val note = TextView(this).apply { text = "نسخة أولية: تشغيل ملفات الصوت محليًا. فصل الغناء والآلات والتحكم المتقدم قيد التطوير."; textSize = 16f }
        layout.addView(note)
        setContentView(layout)
    }
    private fun pickAudio() { val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="audio/*"; addCategory(Intent.CATEGORY_OPENABLE) }; startActivityForResult(i,100) }
    @Deprecated("legacy result callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==100 && resultCode==RESULT_OK){ selected=data?.data; status.text="تم اختيار الملف"; selected?.let { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } } }
    private fun playAudio() { val u=selected ?: run { status.text="اختر ملفًا أولًا"; return }; try { player?.release(); player=MediaPlayer().apply { setDataSource(this@MainActivity,u); setOnPreparedListener { it.start() }; setOnCompletionListener { status.text="انتهى التشغيل" }; prepareAsync() }; status.text="جارٍ تجهيز الصوت…" } catch(e:Exception){ status.text="تعذر تشغيل الملف: ${e.message}" } }
    override fun onDestroy(){ player?.release(); player=null; super.onDestroy() }
}