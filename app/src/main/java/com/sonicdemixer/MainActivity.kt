package com.sonicdemixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.media.MediaPlayer
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
    private lateinit var resultBox: LinearLayout
    private var selected: Uri? = null
    private var player: MediaPlayer? = null
    private var playingFile: File? = null
    private var pendingExport: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = DemucsEngine(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        fun label(text: String, size: Float, color: Int) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER; setPadding(0, 10, 0, 10)
        }
        root.addView(label("SonicDemixer", 28f, Color.WHITE))
        root.addView(label("فصل صوتي بالذكاء الاصطناعي — معالجة محلية", 16f, Color.LTGRAY))
        status = label(if (engine.isModelReady()) "النموذج جاهز — يعمل دون إنترنت" else "نزّل النموذج مرة واحدة لتفعيل الفصل Offline", 14f, Color.rgb(125, 211, 252))
        root.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(progress, LinearLayout.LayoutParams(-1, 12))
        download = Button(this).apply { text = "تنزيل نموذج AI" }
        choose = Button(this).apply { text = "اختيار ملف صوتي" }
        separate = Button(this).apply { text = "فصل المسارات الأربعة"; isEnabled = engine.isModelReady() }
        root.addView(download); root.addView(choose); root.addView(separate)
        root.addView(label("المسارات الناتجة — تشغيل منفصل", 18f, Color.WHITE))
        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(resultBox)
        root.addView(label("الملفات الناتجة WAV. اختر حفظ WAV لنقل المسار إلى مكان تختاره.", 12f, Color.LTGRAY))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            selected = data?.data
            status.text = if (selected != null) "تم اختيار الملف. جاهز للفصل." else "لم يتم اختيار ملف"
        } else if (requestCode == 42 && resultCode == RESULT_OK) {
            val uri = data?.data; val file = pendingExport
            if (uri != null && file != null) executor.execute {
                try { copyToUri(uri, file); main.post { status.text = "تم حفظ الملف خارج التطبيق." } }
                catch (e: Exception) { main.post { status.text = "فشل الحفظ: ${e.message}" } }
            }
            pendingExport = null
        }
    }

    private fun downloadModel() {
        setBusy(true); status.text = "جار تنزيل نموذج الفصل..."
        executor.execute {
            try {
                engine.downloadModel { p -> main.post { progress.progress = p; status.text = "تنزيل النموذج: $p%" } }
                main.post { status.text = "اكتمل تنزيل النموذج. يمكنك العمل Offline."; separate.isEnabled = true; setBusy(false) }
            } catch (e: Exception) { main.post { status.text = "فشل التنزيل: ${e.message}"; setBusy(false) } }
        }
    }

    private fun runSeparation() {
        val uri = selected
        if (uri == null) { status.text = "اختر ملفًا صوتيًا أولًا"; return }
        if (!engine.isModelReady()) { status.text = "نزّل النموذج أولًا"; return }
        stopPlayback()
        setBusy(true); progress.progress = 0; status.text = "جار تجهيز الصوت..."
        executor.execute {
            try {
                val files = engine.separateWav(uri) { p -> main.post { progress.progress = p; status.text = "الفصل: $p%" } }
                main.post { showResults(files); status.text = "اكتمل الفصل وحُفظت المسارات بصيغة WAV."; setBusy(false) }
            } catch (e: Exception) { main.post { status.text = "تعذر الفصل: ${e.message}"; setBusy(false) } }
        }
    }

    private fun showResults(files: List<File>) {
        resultBox.removeAllViews()
        files.forEach { file ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,8,0,8) }
            val name = TextView(this).apply { text = file.nameWithoutExtension.replaceFirstChar { it.uppercase() }; textSize = 16f; setTextColor(Color.WHITE) }
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val play = Button(this).apply { text = "▶ تشغيل" }
            val stop = Button(this).apply { text = "■ إيقاف" }
            val save = Button(this).apply { text = "حفظ WAV" }
            play.setOnClickListener { playFile(file) }
            stop.setOnClickListener { if (playingFile == file) stopPlayback() }
            save.setOnClickListener { exportFile(file) }
            controls.addView(play); controls.addView(stop); controls.addView(save)
            row.addView(name); row.addView(controls)
            resultBox.addView(row)
        }
    }

    private fun playFile(file: File) {
        stopPlayback()
        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stopPlayback() }
                setOnErrorListener { _, _, _ -> stopPlayback(); status.text = "تعذر تشغيل المسار"; true }
                prepare(); start()
            }
            playingFile = file; status.text = "تشغيل: ${file.name}"
        } catch (e: Exception) { status.text = "خطأ في التشغيل: ${e.message}" }
    }

    private fun stopPlayback() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null; playingFile = null
    }

    private fun exportFile(file: File) {
        pendingExport = file
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "audio/wav"
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        startActivityForResult(intent, 42)
    }

    private fun copyToUri(uri: Uri, file: File) {
        contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
    }

    private fun setBusy(busy: Boolean) {
        download.isEnabled = !busy; choose.isEnabled = !busy
        separate.isEnabled = !busy && engine.isModelReady()
    }
    override fun onDestroy() { stopPlayback(); executor.shutdownNow(); super.onDestroy() }
}
