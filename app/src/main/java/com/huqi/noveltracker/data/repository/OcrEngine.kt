package com.huqi.noveltracker.data.repository

import android.graphics.Bitmap

/**
 * OCR contract. Implemented by MlKitOcrEngine (Chinese, on-device).
 * Returns the raw recognized text; name extraction happens downstream.
 */
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): String
}
