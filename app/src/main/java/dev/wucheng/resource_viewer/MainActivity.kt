package dev.wucheng.resource_viewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import dev.wucheng.resource_viewer.data.local.dao.AppConfigDao
import dev.wucheng.resource_viewer.ui.base.FatalErrorHolder
import dev.wucheng.resource_viewer.ui.components.AppShell
import dev.wucheng.resource_viewer.ui.components.ErrorView
import dev.wucheng.resource_viewer.ui.components.ErrorViewLevel
import dev.wucheng.resource_viewer.ui.components.PrivacyConsentDialog
import dev.wucheng.resource_viewer.ui.theme.ResourceViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val appConfigDao: AppConfigDao by inject()

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
        setContent {
            ResourceViewerTheme {
                val fatalError by FatalErrorHolder.fatalError.collectAsState()
                val windowSizeClass = calculateWindowSizeClass(this)
                var showPrivacyDialog by remember { mutableStateOf(false) }
                var hasAcceptedPrivacy by remember { mutableStateOf(false) }
                var permissionsGranted by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                // 检查隐私政策状态
                LaunchedEffect(Unit) {
                    val accepted = withContext(Dispatchers.IO) {
                        appConfigDao.hasAcceptedPrivacy() ?: false
                    }
                    hasAcceptedPrivacy = accepted
                    if (!accepted) {
                        showPrivacyDialog = true
                    }
                }

                // 检查权限状态
                LaunchedEffect(hasAcceptedPrivacy) {
                    if (hasAcceptedPrivacy) {
                        permissionsGranted = checkPermissions()
                        if (!permissionsGranted) {
                            requestPermissions { granted ->
                                permissionsGranted = granted
                            }
                        }
                    }
                }

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
                }
                // M12: 隐私政策弹窗
                else if (showPrivacyDialog) {
                    PrivacyConsentDialog(
                        onAccept = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    appConfigDao.updatePrivacyAccepted(true)
                                }
                                showPrivacyDialog = false
                                hasAcceptedPrivacy = true
                            }
                        },
                        onDecline = {
                            finish()
                        },
                    )
                }
                // 主界面
                else if (hasAcceptedPrivacy) {
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
