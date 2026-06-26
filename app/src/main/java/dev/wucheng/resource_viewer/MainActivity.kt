package dev.wucheng.resource_viewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.wucheng.resource_viewer.ui.base.FatalErrorHolder
import dev.wucheng.resource_viewer.ui.components.AppShell
import dev.wucheng.resource_viewer.ui.components.ErrorView
import dev.wucheng.resource_viewer.ui.components.ErrorViewLevel
import dev.wucheng.resource_viewer.ui.theme.ResourceViewerTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResourceViewerTheme {
                val fatalError by FatalErrorHolder.fatalError.collectAsState()

                if (fatalError != null) {
                    ErrorView(
                        message = fatalError!!,
                        canRetry = true,
                        onRetry = {
                            FatalErrorHolder.clear()
                            recreate()
                        },
                        level = ErrorViewLevel.PAGE,
                    )
                } else {
                    val windowSizeClass = calculateWindowSizeClass(this)
                    AppShell(widthSizeClass = windowSizeClass.widthSizeClass)
                }
            }
        }
    }
}
