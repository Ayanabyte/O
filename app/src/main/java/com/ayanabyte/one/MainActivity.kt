package com.ayanabyte.one

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ayanabyte.one.ui.BrandIntro
import com.ayanabyte.one.ui.HomeScreen

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContent { OneApplication() }
    }
}

@Composable
private fun OneApplication() {
    var showIntro by rememberSaveable { mutableStateOf(true) }

    if (showIntro) {
        BrandIntro(onFinished = { showIntro = false })
    } else {
        HomeScreen()
    }
}
