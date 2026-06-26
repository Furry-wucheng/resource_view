# ============================================
# M28.1: ProGuard / R8 规则
# Resource Viewer - Android
# ============================================

# --------------------------------------------
# 通用 Android 规则
# --------------------------------------------

# 保留行号信息用于调试
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解
-keepattributes *Annotation*

# 保留泛型签名
-keepattributes Signature

# 保留异常信息
-keepattributes Exceptions

# --------------------------------------------
# Room 数据库
# --------------------------------------------

# 保留所有 Entity 类（反射用于数据库映射）
-keep class dev.wucheng.resource_viewer.data.local.entity.** { *; }

# 保留 Room 注解
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *

# 保留 Room 生成的实现类
-keep class *_Impl { *; }

# --------------------------------------------
# Koin 依赖注入
# --------------------------------------------

# 保留 Koin Module 定义
-keep class org.koin.** { *; }
-keep class dev.wucheng.resource_viewer.di.** { *; }

# 保留 Koin 注解
-keep @org.koin.core.annotation.** class * { *; }

# --------------------------------------------
# SMB (smbj) 库
# --------------------------------------------

# 保留 smbj 核心类
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# 保留 SMB 协议相关类
-keep class com.hierynomus.msdtyp.** { *; }
-keep class com.hierynomus.mssmb2.** { *; }
-keep class com.hierynomus.smbj.** { *; }
-keep class com.hierynomus.smbj.auth.** { *; }
-keep class com.hierynomus.smbj.connection.** { *; }
-keep class com.hierynomus.smbj.session.** { *; }
-keep class com.hierynomus.smbj.share.** { *; }
-keep class com.hierynomus.smbj.transport.** { *; }

# 保留 mbassy 事件总线（smbj 依赖）
-keep class net.engio.mbassy.** { *; }
-dontwarn net.engio.mbassy.**

# 忽略 javax.el（mbassy 可选依赖，Android 不需要）
-dontwarn javax.el.**

# --------------------------------------------
# BouncyCastle（SMB 安全依赖）
# --------------------------------------------

# 保留 BouncyCastle 加密类
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# 保留 BC Provider
-keep class org.bouncycastle.jce.provider.** { *; }

# --------------------------------------------
# pdfium-android（PDF 渲染）
# --------------------------------------------

# 保留 pdfium JNI 接口
-keep class com.shockwave.pdfium.** { *; }

# 保留 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 pdfium 托管类
-keep class io.legere.pdfium.** { *; }
-keep class io.legere.pdfiumandroid.** { *; }

# --------------------------------------------
# Coil 图片加载
# --------------------------------------------

# 保留 Coil 核心类
-keep class coil3.** { *; }
-keep class coil.** { *; }

# 保留 Coil Compose 集成
-keep class coil3.compose.** { *; }

# 保留 Coil 视频帧支持
-keep class coil3.video.** { *; }
-keep class coil.decode.** { *; }

# --------------------------------------------
# Media3 / ExoPlayer 视频播放
# --------------------------------------------

# 保留 Media3 核心类
-keep class androidx.media3.** { *; }

# 保留 ExoPlayer 相关类
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.extractor.** { *; }

# 保留 Media3 工厂类（反射创建）
-keep class * extends androidx.media3.exoplayer.source.MediaSource$Factory { *; }
-keep class * extends androidx.media3.datasource.DataSource$Factory { *; }

# --------------------------------------------
# Jetpack Compose
# --------------------------------------------

# 保留 Compose 运行时
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }

# 保留 Compose 编译器生成的代码
-keep class **.ComposableSingletons* { *; }
-keep class **_*ComposableLambda* { *; }
-keep class **.*Composable* { *; }

# 保留 Compose Navigation
-keep class androidx.navigation.compose.** { *; }

# --------------------------------------------
# 序列化 / 数据类
# --------------------------------------------

# 保留所有 data class（用于 Room 和 Navigation 参数）
-keep class dev.wucheng.resource_viewer.data.local.entity.** { *; }
-keep class dev.wucheng.resource_viewer.domain.model.** { *; }

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --------------------------------------------
# zip4j / Commons Compress（压缩包处理）
# --------------------------------------------

# 保留 zip4j 核心类
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**

# 保留 Commons Compress
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# 保留压缩格式支持
-keep class org.apache.commons.compress.compressors.** { *; }
-keep class org.apache.commons.compress.archivers.** { *; }

# --------------------------------------------
# Security Crypto（加密存储）
# --------------------------------------------

# 保留 AndroidX Security
-keep class androidx.security.crypto.** { *; }

# --------------------------------------------
# kotlinx.coroutines
# --------------------------------------------

# 保留协程核心
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --------------------------------------------
# UUID
# --------------------------------------------

# 保留 benasher-uuid
-keep class com.benasher.uuid.** { *; }

# --------------------------------------------
# 测试相关（仅 debug）
# --------------------------------------------

# 保留测试框架
-keep class org.junit.** { *; }
-keep class io.mockk.** { *; }
-dontwarn io.mockk.**

# --------------------------------------------
# 抑制常见警告
# --------------------------------------------

# 忽略 javax.annotation
-dontwarn javax.annotation.**

# 忽略 sun.misc.Unsafe
-dontwarn sun.misc.Unsafe

# 忽略 Google Play Core（如果存在）
-dontwarn com.google.android.play.core.**
