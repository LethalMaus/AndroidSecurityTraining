package dev.jamescullimore.android_security_training

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.Density
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.android.resources.ScreenRatio
import com.android.resources.ScreenRound
import com.android.resources.ScreenSize
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.device.DevicePreviewInfoParser
import sergio.sastre.composable.preview.scanner.android.device.domain.Device
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.preview.getAnnotation
import kotlin.math.ceil

object ComposablePreviewProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<ComposablePreview<AndroidPreviewInfo>> =
        AndroidComposablePreviewScanner()
            .scanPackageTrees(
                "dev.jamescullimore.android_security_training",
                "dev.jamescullimore.android_security_training.vuln",
                "dev.jamescullimore.android_security_training.pinning",
                "dev.jamescullimore.android_security_training.re",
                "dev.jamescullimore.android_security_training.e2e",
                "dev.jamescullimore.android_security_training.perm",
                "dev.jamescullimore.android_security_training.links",
                "dev.jamescullimore.android_security_training.storage",
                "dev.jamescullimore.android_security_training.root",
                "dev.jamescullimore.android_security_training.web",
                "dev.jamescullimore.android_security_training.users",
                "dev.jamescullimore.android_security_training.risks",
            )
            .includeAnnotationInfoForAllOf(PaparazziConfig::class.java)
            .getPreviews()
}

class Dimensions(
    val screenWidthInPx: Int,
    val screenHeightInPx: Int
)

object ScreenDimensions {
    fun dimensions(
        parsedDevice: Device,
        widthDp: Int,
        heightDp: Int
    ): Dimensions {
        val conversionFactor = parsedDevice.densityDpi / 160f
        val previewWidthInPx = ceil(widthDp * conversionFactor).toInt()
        val previewHeightInPx = ceil(heightDp * conversionFactor).toInt()
        return Dimensions(
            screenHeightInPx = when (heightDp > 0) {
                true -> previewHeightInPx
                false -> parsedDevice.dimensions.height.toInt()
            },
            screenWidthInPx = when (widthDp > 0) {
                true -> previewWidthInPx
                false -> parsedDevice.dimensions.width.toInt()
            }
        )
    }
}

object DeviceConfigBuilder {
    fun build(preview: AndroidPreviewInfo): DeviceConfig {
        val parsedDevice =
            DevicePreviewInfoParser.parse(preview.device)?.inPx() ?: return DeviceConfig()

        val dimensions = ScreenDimensions.dimensions(
            parsedDevice = parsedDevice,
            widthDp = preview.widthDp,
            heightDp = preview.heightDp
        )

        return DeviceConfig(
            screenHeight = dimensions.screenHeightInPx,
            screenWidth = dimensions.screenWidthInPx,
            density = Density(parsedDevice.densityDpi),
            xdpi = parsedDevice.densityDpi, // not 100% precise
            ydpi = parsedDevice.densityDpi, // not 100% precise
            size = ScreenSize.valueOf(parsedDevice.screenSize.name),
            ratio = ScreenRatio.valueOf(parsedDevice.screenRatio.name),
            screenRound = ScreenRound.valueOf(parsedDevice.shape.name),
            orientation = ScreenOrientation.valueOf(parsedDevice.orientation.name),
            locale = preview.locale.ifBlank { "en" },
            nightMode = when (preview.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES) {
                true -> NightMode.NIGHT
                false -> NightMode.NOTNIGHT
            }
        )
    }
}

object PaparazziPreviewRule {
    fun createFor(preview: ComposablePreview<AndroidPreviewInfo>): Paparazzi {
        val previewInfo = preview.previewInfo
        return Paparazzi(
            deviceConfig = DeviceConfigBuilder.build(preview.previewInfo),
            supportsRtl = true,
            showSystemUi = previewInfo.showSystemUi,
            renderingMode = when {
                previewInfo.showSystemUi -> SessionParams.RenderingMode.NORMAL
                previewInfo.widthDp > 0 && previewInfo.heightDp > 0 -> SessionParams.RenderingMode.FULL_EXPAND
                previewInfo.heightDp > 0 -> SessionParams.RenderingMode.V_SCROLL
                else -> SessionParams.RenderingMode.SHRINK
            },
            maxPercentDifference = preview.getAnnotation<PaparazziConfig>()?.maxPercentDifference ?: 0.0
        )
    }
}

@Composable
fun SystemUiSize(
    widthInDp: Int,
    heightInDp: Int,
    content: @Composable () -> Unit
) {
    Box(Modifier
        .size(
            width = widthInDp.dp,
            height = heightInDp.dp
        )
        .background(Color.White)
    ) {
        content()
    }
}

@Composable
fun PreviewBackground(
    showBackground: Boolean,
    backgroundColor: Long,
    content: @Composable () -> Unit
) {
    when (showBackground) {
        false -> content()
        true -> {
            val color = when (backgroundColor != 0L) {
                true -> Color(backgroundColor)
                false -> Color.White
            }
            Box(Modifier.background(color)) {
                content()
            }
        }
    }
}