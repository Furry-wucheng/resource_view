package dev.wucheng.resource_viewer.proguard

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * M28.1: ProGuard/R8 规则测试
 *
 * 验证 proguard-rules.pro 包含所有必要的 keep 规则。
 */
class ProGuardRulesTest {

    private val proguardFile = File("proguard-rules.pro")

    @Test
    fun `proguard rules file should exist`() {
        assertTrue("proguard-rules.pro should exist", proguardFile.exists())
    }

    @Test
    fun `should keep Room entities`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep Room Entity classes",
            content.contains("Room") || content.contains("@Entity")
        )
    }

    @Test
    fun `should keep smbj classes for SMB compatibility`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep smbj classes",
            content.contains("smbj") || content.contains("com.hierynomus")
        )
    }

    @Test
    fun `should keep pdfium native methods`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep pdfium native methods",
            content.contains("pdfium") || content.contains("native")
        )
    }

    @Test
    fun `should keep Coil image loader`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep Coil classes",
            content.contains("coil") || content.contains("io.coil-kt")
        )
    }

    @Test
    fun `should keep Media3 ExoPlayer`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep Media3/ExoPlayer classes",
            content.contains("media3") || content.contains("exoplayer") || content.contains("androidx.media3")
        )
    }

    @Test
    fun `should keep Compose runtime`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep Compose runtime classes",
            content.contains("compose") || content.contains("androidx.compose")
        )
    }

    @Test
    fun `should keep Koin dependency injection`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep Koin classes",
            content.contains("koin") || content.contains("io.insert-koin")
        )
    }

    @Test
    fun `should keep BouncyCastle for SMB security`() {
        val content = proguardFile.readText()
        assertTrue(
            "Should keep BouncyCastle classes",
            content.contains("bouncycastle") || content.contains("org.bouncycastle")
        )
    }
}
