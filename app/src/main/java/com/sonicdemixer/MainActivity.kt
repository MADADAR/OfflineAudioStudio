package com.sonicdemixer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
class MainActivity : ComponentActivity() {
 companion object { init { System.loadLibrary("sonic_native") } }
 external fun nativeEngineVersion(): Int
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   MaterialTheme {
    Surface {
     Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
      Text("SonicDemixer")
      Spacer(Modifier.height(8.dp))
      Text("Offline Audio Studio")
      Text("Native engine version: " + nativeEngineVersion().toString())
     }
    }
   }
  }
 }
}