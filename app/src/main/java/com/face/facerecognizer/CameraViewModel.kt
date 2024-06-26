package com.face.facerecognizer

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.face.vision.model.FaceNetModel
import com.face.vision.model.ModelInfo
import com.face.vision.utils.AnalyseUserImageState
import com.face.vision.utils.FileReader
import com.face.vision.utils.FrameAnalyser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraViewModel(
    applicationContext: Application
) : AndroidViewModel(applicationContext) {
    private val _analiseState = MutableStateFlow<AnalyseUserImageState>(AnalyseUserImageState.Init)
    val analiseState = _analiseState.asStateFlow()

    private val faceNetModel: FaceNetModel =
        FaceNetModel(applicationContext, ModelInfo.FACENET_512_QUANTIZED)
    val frameAnalyser: FrameAnalyser = FrameAnalyser(faceNetModel) {
        _analiseState.value = it
    }
    private val fileReader: FileReader = FileReader(faceNetModel)

    private val fileReaderCallback = object : FileReader.ProcessCallback {
        override fun onProcessCompleted(
            data: ArrayList<Pair<String, FloatArray>>,
            numImagesWithNoFaces: Int
        ) {
            frameAnalyser.faceList = data
        }
    }

    init {
        loadImgDataSet()
    }

    private fun loadImgDataSet() {
        viewModelScope.launch {
            fileReader.run(
                arrayListOf(
                    //reference img
                    "" to Bitmap.createBitmap(600, 480, Bitmap.Config.ARGB_8888)
                ), callback = fileReaderCallback
            )
        }
    }


}