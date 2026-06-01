package com.xastia.test.domain.repository

import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.xastia.test.domain.pulse.RgbChannels
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun startCamera(previewView: PreviewView): Flow<ImageProxy>

    /**
     * Усереднена яскравість Y-каналу (luma) у центральній ROI.
     */
    fun analyzeImage(image: ImageProxy): Double

    /**
     * Усереднене значення червоного (R) каналу у центральній ROI.
     * Це сирий PPG-сигнал — основа для обчислення пульсу.
     */
    fun analyzeRedChannel(image: ImageProxy): Double

    /**
     * Усереднені значення всіх трьох каналів RGB у центральній ROI.
     * Потрібні для FingerDetector — детекція «палець на лінзі» через ratio
     * каналів (R має сильно домінувати над G і B).
     */
    fun analyzeRgb(image: ImageProxy): RgbChannels

    /**
     * Вмикає/вимикає вбудований ліхтарик (torch).
     * Стабільне джерело світла критично важливе для PPG —
     * без нього зміни кольору шкіри загубляться у фоновому освітленні.
     */
    fun enableTorch(enable: Boolean)
}
