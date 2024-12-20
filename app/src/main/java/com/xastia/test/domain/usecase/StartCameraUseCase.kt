package com.xastia.test.domain.usecase

import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.xastia.test.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow

class StartCameraUseCase(val cameraRepository: CameraRepository) {
    fun execute(previewView: PreviewView): Flow<ImageProxy> {
        return cameraRepository.startCamera(previewView)
    }
}