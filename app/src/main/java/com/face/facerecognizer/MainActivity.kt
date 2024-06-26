package com.face.facerecognizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.face.facerecognizer.ui.theme.FaceRecognizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    0
                )
            }
            FaceRecognizerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FloatingDraggableItem(initialOffset = { state ->
                        IntOffset(
                            x = (state.containerSize.width - state.contentSize.width),
                            y = state.containerSize.height - state.contentSize.height,
                        )
                    }) {
                        CameraView() {
                            this@MainActivity.finish()
                        }
                    }
                }
            }
        }
    }
}
