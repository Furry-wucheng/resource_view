package dev.wucheng.resource_viewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dev.wucheng.resource_viewer.ui.base.FatalErrorHolder
import dev.wucheng.resource_viewer.ui.components.AppShell
import dev.wucheng.resource_viewer.ui.components.ErrorView
import dev.wucheng.resource_viewer.ui.components.ErrorViewLevel
import dev.wucheng.resource_viewer.ui.theme.ResourceViewerThemeWithSettings

class MainActivity : ComponentActivity() {

    // M12: 权限请求回调
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        onPermissionResult?.invoke(allGranted)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 直接请求权限
        if (!checkPermissions()) {
            requestPermissions { }
        }

        setContent {
            ResourceViewerThemeWithSettings {
                val colorScheme = MaterialTheme.colorScheme
                SideEffect {
                    window.statusBarColor = colorScheme.surface.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
                }
                val fatalError by FatalErrorHolder.fatalError.collectAsState()
                val windowSizeClass = calculateWindowSizeClass(this)

                // M26: 错误处理
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
                    AppShell(widthSizeClass = windowSizeClass.widthSizeClass)
                }
            }
        }
    }

    /**
     * M12: 检查是否已获取所有必要权限
     */
    private fun checkPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * M12: 请求必要权限
     */
    private fun requestPermissions(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult
        requestPermissionLauncher.launch(getRequiredPermissions().toTypedArray())
    }

    /**
     * M12: 获取需要请求的权限列表
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            // Android 12 及以下
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
