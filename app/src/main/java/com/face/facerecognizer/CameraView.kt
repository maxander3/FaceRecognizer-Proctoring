package com.face.facerecognizer

import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.face.vision.utils.AnalyseUserImageState
import kotlinx.coroutines.delay
import java.util.concurrent.Executors


@Composable
fun CameraView(modifier: Modifier = Modifier, onFaceError: () -> Unit) {
    val viewModel: CameraViewModel = viewModel()

    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.analiseState.collectAsState()
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {

                ProcessCameraProvider.getInstance(context).apply {

                    addListener(
                        {
                            try {
                                val cameraProvider = this.get()
                                val preview: androidx.camera.core.Preview =
                                    androidx.camera.core.Preview.Builder().build()
                                val cameraSelector: CameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                    .build()
                                preview.setSurfaceProvider(surfaceProvider)
                                val imageFrameAnalysis = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(480, 640))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build()
                                imageFrameAnalysis.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    viewModel.frameAnalyser
                                )
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageFrameAnalysis
                                )
                            } catch (e: Throwable) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        ContextCompat.getMainExecutor(context)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    when (state) {
        AnalyseUserImageState.UserRecognized -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Green.copy(0.2f)
                    )
            )
        }

        AnalyseUserImageState.Init -> {}
        else -> {
            var timer by remember {
                mutableIntStateOf(5)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Red.copy(0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timer.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            LaunchedEffect(key1 = Unit) {
                while (timer > 0) {
                    delay(1000)
                    timer--
                }
                onFaceError.invoke()
            }

        }
    }
}