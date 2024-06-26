package com.face.vision.utils

import android.graphics.Bitmap
import android.graphics.Rect
import com.face.vision.model.FaceNetModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileReader(private var faceNetModel: FaceNetModel) {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var numImagesWithNoFaces = 0
    private var imageCounter = 0
    private var numImages = 0
    private var data = ArrayList<Pair<String, Bitmap>>()
    private lateinit var callback: ProcessCallback

    private val imageData = ArrayList<Pair<String, FloatArray>>()


    fun run(data: ArrayList<Pair<String, Bitmap>>, callback: ProcessCallback) {
        numImages = data.size
        this.data = data
        this.callback = callback
        scanImage(data[imageCounter].first, data[imageCounter].second)
    }


    interface ProcessCallback {
        fun onProcessCompleted(data: ArrayList<Pair<String, FloatArray>>, numImagesWithNoFaces: Int)
    }


    private fun scanImage(name: String, image: Bitmap) {
        mainScope.launch {
            val inputImage = InputImage.fromByteArray(
                BitmapUtils.bitmapToNV21ByteArray(image),
                image.width,
                image.height,
                0,
                InputImage.IMAGE_FORMAT_NV21
            )
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.size != 0) {
                        mainScope.launch {
                            val embedding = getEmbedding(image, faces[0].boundingBox)
                            imageData.add(Pair(name, embedding))
                            if (imageCounter + 1 != numImages) {
                                imageCounter += 1
                                scanImage(data[imageCounter].first, data[imageCounter].second)
                            } else {
                                callback.onProcessCompleted(imageData, numImagesWithNoFaces)
                                reset()
                            }
                        }
                    } else {
                        numImagesWithNoFaces += 1
                        if (imageCounter + 1 != numImages) {
                            imageCounter += 1
                            scanImage(data[imageCounter].first, data[imageCounter].second)
                        } else {
                            callback.onProcessCompleted(imageData, numImagesWithNoFaces)
                            reset()
                        }
                    }
                }
        }
    }

    private suspend fun getEmbedding(image: Bitmap, bbox: Rect): FloatArray =
        withContext(Dispatchers.Default) {
            return@withContext faceNetModel.getFaceEmbedding(
                BitmapUtils.cropRectFromBitmap(
                    image,
                    bbox
                )
            )
        }


    private fun reset() {
        imageCounter = 0
        numImages = 0
        numImagesWithNoFaces = 0
        data.clear()
    }

}