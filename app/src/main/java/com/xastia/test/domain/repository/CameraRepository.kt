package com.xastia.test.domain.repository

import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun startCamera(previewView: PreviewView): Flow<ImageProxy>
    fun analyzeImage(image: ImageProxy): Double
}