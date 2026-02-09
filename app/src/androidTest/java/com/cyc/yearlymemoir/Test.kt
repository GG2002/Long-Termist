package com.cyc.yearlymemoir

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cyc.yearlymemoir.venassists.ImageProcessing
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

// kotlin 类
@RunWith(AndroidJUnit4::class)
class ImageProcessingTest {
    @Test
    fun test() {
        // 从 Assets 读取文件流
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open("test.jpg")
        val image = BitmapFactory.decodeStream(inputStream)
        assertNotNull(image)

        // 进行图像处理操作
        val width = image.width
        val height = image.height

        val topLeftRect = Rect(0, height / 5, width / 2, height * 3 / 5)
        val topLeftCropped = Bitmap.createBitmap(
            image,
            topLeftRect.left,
            topLeftRect.top,
            topLeftRect.width(),
            topLeftRect.height()
        )

        val (regions, _) = ImageProcessing.getNormalizedRegions(
            topLeftCropped,
            saveIntermediates = true
        )
        val ptNorm = ImageProcessing.strategyFirstColRight(regions)
        val finalXY = ImageProcessing.mapToGlobal(
            ptNorm,
            topLeftRect,
            50 to 20
        )
        println(finalXY)
    }
}