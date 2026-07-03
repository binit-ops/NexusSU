package com.nexus.su

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Enable edge-to-edge display to match the sleek design concept
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // 2. Bind the XML layout we created
        setContentView(R.layout.activity_main)
        
        // Future Phase 3 Logic will go here:
        // - Initialize JNI/C++ bridge
        // - Check kernel node access (/dev/nexussu)
        // - Dynamically update the Device Info and Superuser text views
    }
}
