package com.xastia.test.domain.repository

import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun startCamera(previewView: PreviewView): Flow<ImageProxy>

    /**
     * Усереднена яскравість Y-каналу (luma) у центральній ROI.
     * Використовується для детекції закриття об'єктива пальцем у InstructionActivity.
     */
    fun analyzeImage(image: ImageProxy): Double

    /**
     * Усереднене значення червоного (R) каналу у центральній ROI 50% × 50%.
     * Це сирий PPG-сигнал — основа для обчислення пульсу.
     */
    fun analyzeRedChannel(image: ImageProxy): Double

    /**
     * Вмикає/вимикає вбудований ліхтарик (torch).
     * Стабільне джерело світла критично важливе для PPG —
     * без нього зміни кольору шкіри загубляться у фоновому освітленні.
     */
    fun enableTorch(enable: Boolean)
}
