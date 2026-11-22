package com.sonicdemixer

import android.content.Context
import android.net.Uri
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.*
import java.nio.FloatBuffer
import kotlin.math.min

/** HT-Demucs ONNX, offline inference after the one-time model download. */
class DemucsEngine(private val context: Context) {
    companion object {
        const val SEGMENT = 343980
        const val MODEL_URL = "https://huggingface.co/StemSplitio/htdemucs-onnx/resolve/main/htdemucs_fp16weights.onnx"
        private const val MODEL_FILE = "htdemucs_fp16weights.onnx"
        private val STEMS = arrayOf("Drums", "Bass", "Other", "Vocals")
    }
    private val env by lazy { OrtEnvironment.getEnvironment() }
    private var cancelled = false
    private fun modelFile() = File(context.filesDir, MODEL_FILE)
    fun isModelReady() = modelFile().exists() && modelFile().length() > 100_000_000L
    fun cancel() { cancelled = true }

    fun downloadModel(progress: (Int) -> Unit) {
        if (isModelReady()) { progress(100); return }
        val part = File(context.filesDir, "$MODEL_FILE.part")
        val conn = java.net.URL(MODEL_URL).openConnection().apply {
            connectTimeout = 20000; readTimeout = 60000
            setRequestProperty("User-Agent", "SonicDemixer-Android")
        }
        conn.getInputStream().use { input ->
            FileOutputStream(part).use { output ->
                val buffer = ByteArray(1024 * 1024)
                var total = 0L
                val expected = conn.contentLengthLong.takeIf { it > 0 } ?: 174_000_000L
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    output.write(buffer, 0, n); total += n
                    progress((total * 100 / expected).coerceIn(0, 99).toInt())
                }
                output.fd.sync()
            }
        }
        if (part.length() < 100_000_000L) { part.delete(); throw IOException("النموذج لم يكتمل تنزيله") }
        if (!part.renameTo(modelFile())) { part.copyTo(modelFile(), true); part.delete() }
        progress(100)
    }

    fun separateWav(uri: Uri, progress: (Int) -> Unit): List<File> {
        cancelled = false
        val input = File(context.cacheDir, "source.wav")
        context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "تعذر فتح الملف" }
            input.outputStream().use { stream.copyTo(it) }
        }
        try {
            val audio = readWav(input)
            require(audio.rate == 44100) { "يلزم WAV بتردد 44100 هرتز" }
            require(isModelReady()) { "نزّل نموذج الفصل أولًا" }
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(maxOf(2, Runtime.getRuntime().availableProcessors() / 2))
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            env.createSession(modelFile().absolutePath, opts).use { session ->
                val inputName = session.inputNames.first()
                val outputName = session.outputNames.first()
                val stems = Array(4) { Array(2) { FloatArray(audio.left.size) } }
                var offset = 0
                while (offset < audio.left.size) {
                    if (cancelled) throw IOException("تم إلغاء الفصل")
                    val n = min(SEGMENT, audio.left.size - offset)
                    val planar = FloatArray(2 * SEGMENT)
                    System.arraycopy(audio.left, offset, planar, 0, n)
                    System.arraycopy(audio.right, offset, planar, SEGMENT, n)
                    OnnxTensor.createTensor(env, FloatBuffer.wrap(planar), longArrayOf(1, 2, SEGMENT.toLong())).use { tensor ->
                        session.run(mapOf(inputName to tensor)).use { result ->
                            val output = result[outputName] as OnnxTensor
                            val values = output.floatBuffer
                            for (s in 0..3) for (ch in 0..1) {
                                val base = (s * 2 + ch) * SEGMENT
                                for (i in 0 until n) stems[s][ch][offset + i] = values.get(base + i)
                            }
                        }
                    }
                    offset += n
                    progress((offset * 100 / audio.left.size).coerceIn(0, 100))
                }
                val folder = File(context.getExternalFilesDir(null), "SonicDemixer").apply { mkdirs() }
                return stems.mapIndexed { index, pair ->
                    val file = File(folder, "${STEMS[index].lowercase()}_${System.currentTimeMillis()}.wav")
                    writeStereoWav(file, pair[0], pair[1], audio.rate)
                    file
                }
            }
        } finally { input.delete() }
    }

    private data class Audio(val rate: Int, val left: FloatArray, val right: FloatArray)
    private fun RandomAccessFile.i32(): Int = Integer.reverseBytes(readInt())
    private fun RandomAccessFile.u16(): Int = java.lang.Short.toUnsignedInt(java.lang.Short.reverseBytes(readShort()))
    private fun RandomAccessFile.i16(): Short = java.lang.Short.reverseBytes(readShort())

    private fun readWav(file: File): Audio = RandomAccessFile(file, "r").use { raf ->
        val head = ByteArray(4); raf.readFully(head); require(String(head) == "RIFF")
        raf.skipBytes(4); raf.readFully(head); require(String(head) == "WAVE")
        var channels = 0; var rate = 0; var bits = 0; var format = 0
        var dataAt = -1L; var dataSize = 0
        while (raf.filePointer + 8 <= raf.length()) {
            raf.readFully(head); val id = String(head); val size = raf.i32(); val start = raf.filePointer
            if (id == "fmt ") {
                format = raf.u16(); channels = raf.u16(); rate = raf.i32()
                raf.skipBytes(6); bits = raf.u16()
            } else if (id == "data") { dataAt = start; dataSize = size }
            raf.seek(start + size + (size and 1))
            if (dataAt >= 0 && format != 0) break
        }
        require(format == 1 && bits == 16 && channels == 2) { "الصيغة المطلوبة WAV PCM 16-bit ستيريو" }
        require(dataAt >= 0) { "بيانات WAV غير موجودة" }
        val count = dataSize / 4; val l = FloatArray(count); val r = FloatArray(count)
        raf.seek(dataAt)
        for (i in 0 until count) { l[i] = raf.i16() / 32768f; r[i] = raf.i16() / 32768f }
        Audio(rate, l, r)
    }

    private fun writeStereoWav(file: File, left: FloatArray, right: FloatArray, rate: Int) {
        val dataBytes = left.size * 4
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { out ->
            fun le16(v: Int) { out.writeByte(v and 255); out.writeByte((v ushr 8) and 255) }
            fun le32(v: Int) { le16(v and 65535); le16(v ushr 16) }
            out.writeBytes("RIFF"); le32(36 + dataBytes); out.writeBytes("WAVEfmt ")
            le32(16); le16(1); le16(2); le32(rate); le32(rate * 4); le16(4); le16(16)
            out.writeBytes("data"); le32(dataBytes)
            for (i in left.indices) {
                le16((left[i].coerceIn(-1f, 1f) * 32767).toInt())
                le16((right[i].coerceIn(-1f, 1f) * 32767).toInt())
            }
        }
    }
}
