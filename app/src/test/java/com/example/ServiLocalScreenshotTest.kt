package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.example.core.data.repository.MockDataRepository
import com.example.core.ui.theme.MyApplicationTheme
import com.example.features.home.viewmodel.HomeViewModel
import com.example.features.explore.viewmodel.ServiceDetailViewModel
import com.example.features.explore.ui.ServiceDetailScreen
import com.example.features.home.ui.HomeScreen
import com.example.features.onboarding.ui.OnboardingScreen
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ServiLocalScreenshotTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingScreenshot() {
        composeTestRule.setContent {
            MyApplicationTheme { OnboardingScreen(onGetStarted = {}) }
        }
        saveScreenshot("servilocal_onboarding.png")
    }

    @Test
    fun homeScreenshot() {
        val homeViewModel = HomeViewModel(MockDataRepository())
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(homeViewModel, onExplore = {}, onOpenDetail = {}, onOpenNotifications = {})
            }
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Recomendados para ti").fetchSemanticsNodes().isNotEmpty()
        }
        saveScreenshot("servilocal_home.png")
    }

    @Test
    fun serviceDetailScreenshot() {
        val detailViewModel = ServiceDetailViewModel(MockDataRepository())
        composeTestRule.setContent {
            MyApplicationTheme {
                ServiceDetailScreen(
                    postId = "pub_1",
                    viewModel = detailViewModel,
                    onBack = {},
                    onOpenChat = {}
                )
            }
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Detalle de Publicación").fetchSemanticsNodes().isNotEmpty()
        }
        saveScreenshot("servilocal_service_detail.png")
    }

    private fun saveScreenshot(fileName: String) {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the project root from ${System.getProperty("user.dir")}")
        val target = File(root, "app/src/test/screenshots/$fileName")
        target.parentFile?.mkdirs()
        composeTestRule.waitForIdle()
        val decorView = composeTestRule.activity.window.decorView
        check(decorView.width > 0 && decorView.height > 0) { "The app window has no drawable size" }
        val bitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
        decorView.draw(Canvas(bitmap))
        target.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
        bitmap.recycle()
    }
}
