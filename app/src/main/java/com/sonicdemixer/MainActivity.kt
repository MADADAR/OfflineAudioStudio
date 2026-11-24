package com.sonicdemixer

import androidx.activity.ComponentActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.media.MediaPlayer
import android.graphics.Color
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var engine: DemucsEngine
    private var player: MediaPlayer? = null
    private var pendingExport: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = DemucsEngine(this)
        setContent { SonicDemixerUi(
            modelReady = engine.isModelReady(),
            onDownload = { progress ->
                executor.execute { try {
                    engine.downloadModel { p -> main.post { progress(p) } }
                    main.post { progress(100) }
                } catch(e: Exception) { main.post { progress(-1) } } }
            },
            onSeparate = { uri, callback ->
                executor.execute { try {
                    val result=engine.separateWav(uri) { p -> main.post { callback(p,null) } }
                    main.post { callback(100,result) }
                } catch(e: Exception) { main.post { callback(-1,null) } } }
            },
            onPlay = { file -> stopPlayback(); try {
                player=MediaPlayer().apply { setDataSource(file.absolutePath); setOnCompletionListener { stopPlayback() }; prepare(); start() }
            } catch(_:Exception) { Toast.makeText(this,"تعذر تشغيل الملف",Toast.LENGTH_SHORT).show() } },
            onExport = { file -> pendingExport=file; exportLauncher.launch(file.name) }
        ) }
    }
    private val exportLauncher=registerForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val file=pendingExport; pendingExport=null
        if(uri!=null && file!=null) executor.execute {
            try { contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } }
            catch(_:Exception) { main.post { Toast.makeText(this,"فشل الحفظ",Toast.LENGTH_SHORT).show() } }
        }
    }
    private fun stopPlayback(){ try{player?.stop()}catch(_:Exception){}; try{player?.release()}catch(_:Exception){}; player=null }
    override fun onDestroy(){ stopPlayback(); executor.shutdownNow(); super.onDestroy() }
}

@Composable private fun SonicDemixerUi(
    modelReady:Boolean,
    onDownload:( (Int)->Unit)->Unit,
    onSeparate:(Uri,(Int,List<File>?)->Unit)->Unit,
    onPlay:(File)->Unit,
    onExport:(File)->Unit
) {
    val context= LocalContext.current
    var modelIsReady by remember { mutableStateOf(modelReady) }
    val purple=ComposeColor(0xFF7C5CFF); val bg=ComposeColor(0xFF0B0D12); val surface=ComposeColor(0xFF141821)
    var selected by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(if(modelReady) "النموذج جاهز" else "يلزم تنزيل النموذج مرة واحدة") }
    var results by remember { mutableStateOf<List<File>>(emptyList()) }
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selected=uri; fileName=uri?.lastPathSegment?.substringAfterLast('/') ?: ""
        status=if(uri!=null) "تم اختيار الملف" else "لم يتم اختيار ملف"
    }
    Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(14.dp))
        Text("SonicDemixer",fontSize=30.sp,fontWeight=FontWeight.Bold,color=ComposeColor.White)
        Text("فصل صوتي بالذكاء الاصطناعي",color=ComposeColor(0xFFAAB3C4),fontSize=15.sp)
        Spacer(Modifier.height(22.dp))
        Card(colors=CardDefaults.cardColors(containerColor=surface),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("المعالجة المحلية",color=ComposeColor(0xFF45D6B5),fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp)); Text(status,color=ComposeColor.White)
                if(progress>0) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress={progress.coerceAtMost(100)/100f},color=purple,modifier=Modifier.fillMaxWidth()); Text("$progress%",color=ComposeColor.White) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick={ if(!busy){busy=true;progress=0;onDownload { p-> progress=p; if(p<0){status="تعذر تنزيل النموذج";busy=false}else if(p==100){modelIsReady=true;status="النموذج جاهز للعمل محليًا";busy=false} } } },enabled=!busy&&!modelIsReady,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=purple)) { Text(if(modelIsReady)"النموذج مثبت" else "تنزيل نموذج الذكاء الاصطناعي") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick={pick.launch(arrayOf("audio/*"))},enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp)) { Text("اختيار ملف صوتي") }
        if(fileName.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(fileName,color=ComposeColor(0xFFAAB3C4)) }
        Spacer(Modifier.height(14.dp))
        Button(onClick={ val u=selected; if(u==null) status="اختر ملفًا صوتيًا أولًا" else if(!modelIsReady) status="نزّل النموذج أولًا" else {busy=true;progress=0;status="جار فصل الصوت...";onSeparate(u){p,files-> if(p<0){status="تعذر الفصل";busy=false}else if(files!=null){results=files;status="اكتمل الفصل";busy=false}else progress=p}} },enabled=!busy&&modelIsReady&&selected!=null,modifier=Modifier.fillMaxWidth().height(58.dp),colors=ButtonDefaults.buttonColors(containerColor=ComposeColor(0xFF45D6B5),contentColor=bg)) { Text("فصل المسارات الصوتية",fontWeight=FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
        if(results.isNotEmpty()) {
            Text("المسارات الناتجة",color=ComposeColor.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
            results.forEach { file ->
                Card(colors=CardDefaults.cardColors(containerColor=surface),modifier=Modifier.fillMaxWidth().padding(vertical=6.dp),shape=RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(file.nameWithoutExtension.replaceFirstChar{it.uppercase()},color=ComposeColor.White,fontWeight=FontWeight.Bold)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth().padding(top=10.dp)) {
                            OutlinedButton(onClick={onPlay(file)},modifier=Modifier.weight(1f)){Text("▶ تشغيل")}
                            Button(onClick={onExport(file)},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=purple)){Text("حفظ WAV")}
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp)); Text("SonicDemixer • معالجة على جهازك بعد تنزيل النموذج",color=ComposeColor(0xFF667085),fontSize=12.sp)
    }
}
