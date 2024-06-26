package com.face.vision.model

import android.graphics.Rect

data class Prediction( var bbox : Rect, var label : String)