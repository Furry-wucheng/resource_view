package dev.wucheng.resource_viewer.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.wucheng.resource_viewer.ui.components.PrivacyConsentDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrivacyConsentDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display privacy policy title`() {
        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("隐私政策").assertIsDisplayed()
    }

    @Test
    fun `should display welcome message`() {
        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("欢迎使用 Resource Viewer！").assertIsDisplayed()
    }

    @Test
    fun `should display privacy commitments`() {
        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("所有数据仅存储在您的设备本地").assertIsDisplayed()
        composeTestRule.onNodeWithText("不会上传任何个人信息到服务器").assertIsDisplayed()
        composeTestRule.onNodeWithText("不会收集或分享您的使用数据").assertIsDisplayed()
        composeTestRule.onNodeWithText("您可以随时在设置中清除所有数据").assertIsDisplayed()
    }

    @Test
    fun `should display permission explanation`() {
        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("权限说明：").assertIsDisplayed()
        composeTestRule.onNodeWithText("媒体文件访问：用于浏览和管理您的资源文件").assertIsDisplayed()
        composeTestRule.onNodeWithText("存储权限：用于读取设备上的文件").assertIsDisplayed()
    }

    @Test
    fun `should display accept and decline buttons`() {
        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("同意").assertIsDisplayed()
        composeTestRule.onNodeWithText("退出应用").assertIsDisplayed()
    }

    @Test
    fun `should call onAccept when accept button clicked`() {
        var acceptCalled = false

        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = { acceptCalled = true },
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("同意").performClick()

        assert(acceptCalled)
    }

    @Test
    fun `should call onDecline when decline button clicked`() {
        var declineCalled = false

        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = { declineCalled = true },
            )
        }

        composeTestRule.onNodeWithText("退出应用").performClick()

        assert(declineCalled)
    }

    @Test
    fun `should display consent instruction text`() {
        composeTestRule.setContent {
            PrivacyConsentDialog(
                onAccept = {},
                onDecline = {},
            )
        }

        composeTestRule.onNodeWithText("点击"同意"即表示您了解并同意上述隐私政策。").assertIsDisplayed()
    }
}
