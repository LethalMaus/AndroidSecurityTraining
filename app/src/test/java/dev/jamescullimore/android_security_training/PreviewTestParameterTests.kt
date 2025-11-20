package dev.jamescullimore.android_security_training

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.device.DevicePreviewInfoParser
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

@RunWith(TestParameterInjector::class)
class PreviewTestParameterTests(
    @TestParameter(valuesProvider = ComposablePreviewProvider::class)
    val preview: ComposablePreview<AndroidPreviewInfo>,
) {

    @get:Rule
    val paparazzi: Paparazzi = PaparazziPreviewRule.createFor(preview)

    @Test
    fun snapshot() {
        paparazzi.snapshot {
            val info = preview.previewInfo
            val content: @Composable () -> Unit = {
                PreviewBackground(
                    showBackground = if (info.showSystemUi) true else info.showBackground,
                    backgroundColor = info.backgroundColor
                ) {
                    preview()
                }
            }
            when (info.showSystemUi) {
                true -> {
                    DevicePreviewInfoParser.parse(info.device)?.inDp()?.let { parsedDevice ->
                        SystemUiSize(
                            widthInDp = parsedDevice.dimensions.width.toInt(),
                            heightInDp = parsedDevice.dimensions.height.toInt()
                        ) {
                            content()
                        }
                    } ?: content()
                }

                false -> content()
            }
        }
    }
}