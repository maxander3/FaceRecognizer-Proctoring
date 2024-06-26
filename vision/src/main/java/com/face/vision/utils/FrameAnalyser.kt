package com.face.vision.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.face.vision.model.FaceNetModel
import com.face.vision.model.Prediction
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

class FrameAnalyser(
    private var model: FaceNetModel,
    private val analiseStateChangeListener: (AnalyseUserImageState) -> Unit
) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    private val nameScoreHashmap = HashMap<String, ArrayList<Float>>()
    private var subject = FloatArray(model.model.outputDims)

    private var isProcessing = false

    var faceList = ArrayList<Pair<String, FloatArray>>()

    private var t1: Long = 0L

    private val metricToBeUsed = "l2"


    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (isProcessing || faceList.size == 0) {
            image.close()
            return
        } else {
            isProcessing = true
            val cameraXImage = image.image!!
            var frameBitmap = Bitmap.createBitmap(
                cameraXImage.width,
                cameraXImage.height,
                Bitmap.Config.ARGB_8888
            )
            frameBitmap.copyPixelsFromBuffer(image.planes[0].buffer)
            frameBitmap =
                BitmapUtils.rotateBitmap(frameBitmap, image.imageInfo.rotationDegrees.toFloat())

            val inputImage = InputImage.fromBitmap(frameBitmap, 0)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope(Dispatchers.Default).launch {
                        runModel(faces, frameBitmap)
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }


    private suspend fun runModel(faces: List<Face>, cameraFrameBitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            t1 = System.currentTimeMillis()
            val predictions = ArrayList<Prediction>()
            if (faces.isEmpty() || faces.size != 1) {
                analiseStateChangeListener.invoke(AnalyseUserImageState.NoUser)
            } else {
                for (face in faces) {
                    try {
                        val croppedBitmap =
                            BitmapUtils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                        subject = model.getFaceEmbedding(croppedBitmap)
                        for (i in 0 until faceList.size) {
                            if (nameScoreHashmap[faceList[i].first] == null) {
                                val p = ArrayList<Float>()
                                if (metricToBeUsed == "cosine") {
                                    p.add(cosineSimilarity(subject, faceList[i].second))
                                } else {
                                    p.add(L2Norm(subject, faceList[i].second))
                                }
                                nameScoreHashmap[faceList[i].first] = p
                            } else {
                                if (metricToBeUsed == "cosine") {
                                    nameScoreHashmap[faceList[i].first]?.add(
                                        cosineSimilarity(
                                            subject,
                                            faceList[i].second
                                        )
                                    )
                                } else {
                                    nameScoreHashmap[faceList[i].first]?.add(
                                        L2Norm(
                                            subject,
                                            faceList[i].second
                                        )
                                    )
                                }
                            }
                        }

                        val avgScores = nameScoreHashmap.values.map { scores ->
                            scores.toFloatArray().average()
                        }

                        val names = nameScoreHashmap.keys.toTypedArray()
                        nameScoreHashmap.clear()

                        val bestScoreUserName: String = if (metricToBeUsed == "cosine") {
                            if (avgScores.maxOrNull()!! > model.model.cosineThreshold) {
                                analiseStateChangeListener.invoke(AnalyseUserImageState.UserRecognized)
                                names[avgScores.indexOf(avgScores.maxOrNull()!!)]
                            } else {
                                analiseStateChangeListener.invoke(AnalyseUserImageState.UserUnknown)
                                "Unknown"
                            }
                        } else {
                            if (avgScores.minOrNull()!! > model.model.l2Threshold) {
                                analiseStateChangeListener.invoke(AnalyseUserImageState.UserUnknown)
                                "Unknown"
                            } else {
                                analiseStateChangeListener.invoke(AnalyseUserImageState.UserRecognized)
                                names[avgScores.indexOf(avgScores.minOrNull()!!)]
                            }
                        }
                        predictions.add(
                            Prediction(
                                face.boundingBox,
                                bestScoreUserName
                            )
                        )

                    } catch (e: Exception) {
                        continue
                    }
                }
            }
            withContext(Dispatchers.Main) {
                isProcessing = false
            }

        }
    }

    private fun L2Norm(x1: FloatArray, x2: FloatArray): Float {
        return sqrt(x1.mapIndexed { i, xi -> (xi - x2[i]).pow(2) }.sum())
    }

    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val mag1 = sqrt(x1.map { it * it }.sum())
        val mag2 = sqrt(x2.map { it * it }.sum())
        val dot = x1.mapIndexed { i, xi -> xi * x2[i] }.sum()
        return dot / (mag1 * mag2)
    }

}

sealed class AnalyseUserImageState {
    data object UserUnknown : AnalyseUserImageState()
    data object UserRecognized : AnalyseUserImageState()
    data object NoUser : AnalyseUserImageState()
    data object Init : AnalyseUserImageState()
}