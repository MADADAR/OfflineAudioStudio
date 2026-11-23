package com.sonicdemixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var engine: DemucsEngine
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var download: Button
    private lateinit var choose: Button
    private lateinit var separate: Button
    private var selected: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = DemucsEngine(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 48, 28, 28)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        fun label(text: String, size: Float, color: Int) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER; setPadding(0, 12, 0, 12)
        }
        root.addView(label("SonicDemixer", 30f, Color.WHITE))
        root.addView(label("فصل صوتي بالذكاء الاصطناعي — محليًا", 17f, Color.LTGRAY))
        root.addView(label("النموذج HT-Demucs يفصل الغناء والطبول والجهير وباقي الآلات.", 14f, Color.WHITE))
        status = label(if (engine.isModelReady()) "النموذج جاهز — يمكن العمل دون إنترنت" else "يلزم تنزيل النموذج مرة واحدة (حوالي 166 ميجابايت)", 14f, Color.rgb(125, 211, 252))
        root.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, 12))
        download = Button(this).apply { text = "تنزيل نموذج الذكاء الاصطناعي" }
        choose = Button(this).apply { text = "اختيار ملف صوتي (MP3 أو WAV)" }
        separate = Button(this).apply { text = "ابدأ فصل المسارات"; isEnabled = engine.isModelReady() }
        root.addView(download, LinearLayout.LayoutParams(-1, -2))
        root.addView(choose, LinearLayout.LayoutParams(-1, -2))
        root.addView(separate, LinearLayout.LayoutParams(-1, -2))
        root.addView(label("يدعم حاليًا WAV PCM 16-bit، ستيريو، 44100 Hz.\nتُحفظ النتائج في ملفات التطبيق: Android/data/com.sonicdemixer/files/SonicDemixer", 12f, Color.LTGRAY))
        setContentView(ScrollView(this).apply { addView(root) })

        download.setOnClickListener { downloadModel() }
        choose.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, 41)
        }
        separate.setOnClickListener { runSeparation() }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            selected = data?.data
            status.text = if (selected != null) "تم اختيار الملف. اضغط بدء فصل المسارات." else "لم يتم اختيار ملف"
        }
    }

    private fun downloadModel() {
        setBusy(true); status.text = "جارٍ تنزيل النموذج..."
        executor.execute {
            try {
                engine.downloadModel { p -> main.post { progress.progress = p; status.text = "تنزيل النموذج: $p%" } }
                main.post { status.text = "النموذج جاهز. يمكن الآن فصل الصوت دون إنترنت."; separate.isEnabled = true; setBusy(false) }
            } catch (e: Exception) {
                main.post { status.text = "فشل التنزيل: ${e.message}"; setBusy(false) }
            }
        }
    }

    private fun runSeparation() {
        val uri = selected
        if (uri == null) { status.text = "اختر ملف MP3 أو WAV أولًا"; return }
        if (!engine.isModelReady()) { status.text = "نزّل النموذج أولًا"; return }
        setBusy(true); progress.progress = 0; status.text = "جارٍ تجهيز الصوت..."
        executor.execute {
            try {
                val files = engine.separateWav(uri) { p -> main.post { progress.progress = p; status.text = "فصل المسارات: $p%" } }
                main.post {
                    status.text = "اكتمل الفصل!\n" + files.joinToString("\n") { it.name } + "\nتم الحفظ في مجلد SonicDemixer داخل مساحة التطبيق."
                    setBusy(false)
                }
            } catch (e: Exception) {
                main.post { status.text = "تعذر الفصل: ${e.message}"; setBusy(false) }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        download.isEnabled = !busy
        choose.isEnabled = !busy
        separate.isEnabled = !busy && engine.isModelReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}
