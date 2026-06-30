package com.appblish.filora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.appblish.filora.core.ui.theme.FiloraTheme
import com.appblish.filora.navigation.FiloraNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Single activity hosting the Compose navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FiloraTheme {
                FiloraNavHost()
            }
        }
    }
}
