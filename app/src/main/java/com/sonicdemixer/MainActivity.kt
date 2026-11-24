package com.sonicdemixer

import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonicDemixer() }
    }
}

@Composable
private fun SonicDemixer() {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("لم يتم اختيار ملف") }
    var duration by remember { mutableStateOf(0L) }
    var position by remember { mutableStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }
    var eqOn by remember { mutableStateOf(false) }
    var bands by remember { mutableStateOf(List(5) { 0f }) }
    var bass by remember { mutableStateOf(0f) }
    var stereo by remember { mutableStateOf(0f) }
    var eq by remember { mutableStateOf<Equalizer?>(null) }
    var bassFx by remember { mutableStateOf<BassBoost?>(null) }
    var stereoFx by remember { mutableStateOf<Virtualizer?>(null) }
    var trimStart by remember { mutableStateOf(0L) }
    var trimEnd by remember { mutableStateOf(0L) }
    var trimPreview by remember { mutableStateOf(false) }
    var seek by remember { mutableStateOf<Float?>(null) }

    fun applyEffects() {
        try {
            eq?.release(); bassFx?.release(); stereoFx?.release()
            eq = Equalizer(0, player.audioSessionId).apply { enabled = eqOn }
            bassFx = BassBoost(0, player.audioSessionId).apply { enabled = true; setStrength((bass*1000).toInt().toShort()) }
            stereoFx = Virtualizer(0, player.audioSessionId).apply { enabled = true; setStrength((stereo*1000).toInt().toShort()) }
            eq?.let { e ->
                val count = e.numberOfBands.toInt()
                for (i in 0 until minOf(5,count)) {
                    val range = e.bandLevelRange
                    val level = (bands[i]*100).toInt().coerceIn(range[0].toInt(),range[1].toInt())
                    try { e.setBandLevel(i.toShort(), level.toShort()) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) { eq=null; bassFx=null; stereoFx=null }
    }

    DisposableEffect(player) { onDispose { eq?.release(); bassFx?.release(); stereoFx?.release(); player.release() } }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            playing = player.isPlaying
            if (trimPreview && trimEnd>trimStart && playing && position>=trimEnd) {
                player.pause(); player.seekTo(trimStart)
            }
            delay(250)
        }
    }
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        selected?.let {
            uri=it
            try { context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            fileName=it.lastPathSegment?.substringAfterLast('/') ?: "ملف صوتي"
            player.setMediaItem(MediaItem.fromUri(it)); player.prepare(); player.playWhenReady=false
            trimStart=0L; trimEnd=0L; trimPreview=false
            applyEffects()
        }
    }

    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    item {
                        Text("SonicDemixer",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
                        Text("استوديو الصوت المحلي",style=MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text("المشغل",style=MaterialTheme.typography.titleLarge)
                                Text(fileName,textAlign=TextAlign.Center)
                                Button(onClick={pick.launch(arrayOf("audio/*"))}) { Text("اختيار ملف صوتي") }
                                if (uri!=null) {
                                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Button(onClick={if(player.isPlaying) player.pause() else player.play()}) { Text(if(playing) "إيقاف مؤقت" else "تشغيل") }
                                        OutlinedButton(onClick={player.pause();player.seekTo(0)}) { Text("إعادة") }
                                    }
                                    Text("${time(position)} / ${time(duration)}")
                                    Slider(value=seek ?: if(duration>0) position.toFloat()/duration else 0f, onValueChange={seek=it}, onValueChangeFinished={seek?.let{player.seekTo((it*duration).roundToLong())};seek=null}, enabled=duration>0)
                                    Text("سرعة التشغيل: ${"%.1f".format(speed)}x")
                                    Slider(value=speed,onValueChange={speed=it},valueRange=.5f..2f,steps=5)
                                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick={player.seekTo((position-10000).coerceAtLeast(0))}) { Text("-10 ث") }
                                        OutlinedButton(onClick={player.seekTo((position+10000).coerceAtMost(duration))}) { Text("+10 ث") }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("المؤثرات",style=MaterialTheme.typography.titleLarge)
                                Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(eqOn,{eqOn=it;try{eq?.enabled=it}catch(_:Exception){}});Text("المعادل الصوتي") }
                                val names=listOf("منخفض","منخفض متوسط","متوسط","مرتفع متوسط","مرتفع")
                                names.forEachIndexed { index,n ->
                                    Text("$n • ${bands[index].toInt()} dB")
                                    Slider(value=bands[index],onValueChange={v->bands=bands.toMutableList().also{it[index]=v};try{eq?.setBandLevel(index.toShort(),(v*100).toInt().toShort())}catch(_:Exception){}},valueRange=-15f..15f)
                                }
                                Text("تعزيز الجهير • ${bass.toInt()}%")
                                Slider(value=bass,onValueChange={bass=it;try{bassFx?.setStrength((it*1000).toInt().toShort())}catch(_:Exception){}},valueRange=0f..1f)
                                Text("توسيع الستيريو • ${stereo.toInt()}%")
                                Slider(value=stereo,onValueChange={stereo=it;try{stereoFx?.setStrength((it*1000).toInt().toShort())}catch(_:Exception){}},valueRange=0f..1f)
                                Text("قد يختلف دعم مؤثرات الصوت حسب الجهاز.")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("تحديد جزء للمعاينة",style=MaterialTheme.typography.titleLarge)
                                if(duration>0) {
                                    Text("البداية: ${time(trimStart)}")
                                    Slider(value=trimStart.toFloat().coerceIn(0f,duration.toFloat()),onValueChange={trimStart=it.toLong().coerceAtMost((trimEnd.takeIf{it>0}?:duration)-1000).coerceAtLeast(0)},valueRange=0f..duration.toFloat())
                                    Text("النهاية: ${time(trimEnd.takeIf{it>trimStart}?:duration)}")
                                    Slider(value=(trimEnd.takeIf{it>trimStart}?:duration).toFloat().coerceIn(0f,duration.toFloat()),onValueChange={trimEnd=it.toLong().coerceAtLeast(trimStart+1000).coerceAtMost(duration)},valueRange=0f..duration.toFloat())
                                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(trimPreview,{trimPreview=it;if(it&&trimEnd<=trimStart)trimEnd=duration});Text("تفعيل معاينة النطاق") }
                                    OutlinedButton(onClick={player.seekTo(trimStart);player.play()},enabled=trimEnd>trimStart) { Text("معاينة الجزء") }
                                    Text("القص والتصدير النهائي لم يُنفّذا بعد.",style=MaterialTheme.typography.bodySmall)
                                } else Text("اختر ملفًا أولًا")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("ميزات قيد التطوير",style=MaterialTheme.typography.titleLarge)
                                Text("فصل المسارات محليًا • BPM • تصدير ودمج • موجة الصوت")
                                Text("لا يوجد رفع للملفات أو إعلانات أو تحليلات.",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun time(ms:Long):String {
    if(ms<0)return "00:00"
    val s=ms/1000
    return "%02d:%02d".format(s/60,s%60)
}
