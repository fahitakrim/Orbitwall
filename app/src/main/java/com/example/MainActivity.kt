package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ui.MainApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Coil for maximum fidelity: ARGB_8888, hardware acceleration, and 250 MB disk cache
        val imageLoader = ImageLoader.Builder(this)
            .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
            .allowHardware(true)
            .allowRgb565(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("orbitwall_image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            MainApp(modifier = Modifier.fillMaxSize())
        }
    }
}
