package com.sonicdemixer

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        val title = TextView(this).apply {
            text = "SonicDemixer"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "استوديو الصوت دون اتصال"
            textSize = 18f
            setTextColor(Color.rgb(203, 213, 225))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        val status = TextView(this).apply {
            text = "نسخة أولية — يجري تجهيز محرك الصوت"
            textSize = 14f
            setTextColor(Color.rgb(148, 163, 184))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        setContentView(root)
    }
}
