package com.example.chakpiwat.demossocr

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.core.CvType
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private val DIGITS_LOOKUP = hashMapOf(
        arrayOf(1, 1, 1, 1, 1, 1, 0) to "0",
        arrayOf(1, 1, 0, 0, 0, 0, 0) to "1",
        arrayOf(1, 0, 1, 1, 0, 1, 1) to "2",
        arrayOf(1, 1, 1, 0, 0, 1, 1) to "3",
        arrayOf(1, 1, 0, 0, 1, 0, 1) to "4",
        arrayOf(0, 1, 1, 0, 1, 1, 1) to "5",
        arrayOf(0, 1, 1, 1, 1, 1, 1) to "6",
        arrayOf(1, 1, 0, 0, 0, 1, 0) to "7",
        arrayOf(1, 1, 1, 1, 1, 1, 1) to "8",
        arrayOf(1, 1, 1, 0, 1, 1, 1) to "9",
        arrayOf(0, 0, 0, 0, 0, 1, 1) to "-"
    )

    private val THRESHOLD = 35.0
    private val H_W_Ratio = 1.9
    private val ARC_TAN_THETA = 6.0

    private lateinit var loaderCallback: BaseLoaderCallback

    private lateinit var grayImg: Mat
    private lateinit var blurredImg: Mat
    private lateinit var dst: Mat
    private val digitsPositions: ArrayList<ArrayList<Pair<Int, Int>>> = arrayListOf()

    private var height: Double = 0.0
    private var width: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lateinit var imageMat: Mat
        loaderCallback = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        imageMat = Mat()
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }

        button.setOnClickListener {
            loadImage()
            preprocess()
            findDigitsPositions()
            recognizeDigitsAreaMethod()
            showImage()
        }
    }

    private fun loadImage() {
        val img: Mat = Utils.loadResource(this, R.drawable.test1)
        // return Gray Image :grayImg
        grayImg = Mat()
        Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_RGB2GRAY)

        // return Blurred Image: blurredImg
        blurredImg = Mat()
        Imgproc.GaussianBlur(grayImg, blurredImg, Size(7.0,7.0), 0.0)

        // get height and width
        height = grayImg.size().height
        width = grayImg.size().width
    }

    private fun preprocess() {
        val kernelSize = Size(5.0, 5.0)

        val clahe = Imgproc.createCLAHE(2.0, Size(6.0, 6.0))
        clahe.apply(blurredImg, blurredImg)

        dst = Mat()
        Imgproc.adaptiveThreshold(blurredImg, dst, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 127, THRESHOLD)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, kernelSize)
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_OPEN, kernel)
    }

    private fun showImage() {
        val matTobeShown = dst
        val bitmap = Bitmap.createBitmap(matTobeShown.cols(), matTobeShown.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(matTobeShown, bitmap)
        imageView.setImageBitmap(bitmap)
        imageView.invalidate()
    }

    private fun helperExtract(oneDArray: Mat, threshold: Int = 20, dim: Int = 0) : ArrayList<Pair<Int, Int>> {
        val res: ArrayList<Pair<Int, Int>> = arrayListOf()
        var flag = 0
        var temp = 0

        val size: Int = if (dim == 0) {
            oneDArray.cols()
        } else {
            oneDArray.rows()
        }

        for (i in 0 until size) {
            val elem: Int = if (dim == 0) {
                oneDArray[0,i][0].toInt()
            } else {
                oneDArray[i,0][0].toInt()
            }
            if (elem < 12 * 255) {
                if (flag > threshold) {
                    val start = i - flag
                    val end = i
                    temp = end
                    if (end - start > 20) {
                        res.add(Pair(start, end))
                    }
                }
                flag = 0
            } else {
                flag += 1
            }
        }
        if (flag > threshold) {
            val start = temp
            val end = oneDArray.cols()
            if (end - start > 50) {
                res.add(Pair(start, end))
            }
        }
        return res
    }

    private fun findDigitsPositions() {
        val reservedThreshold = 20

        val sumAxis0 = Mat()
        Core.reduce(dst, sumAxis0, 0, Core.REDUCE_SUM, CvType.CV_32S)
        val horizonPosition: ArrayList<Pair<Int, Int>> = helperExtract(sumAxis0, reservedThreshold, 0)
        val sumAxis1 = Mat()
        Core.reduce(dst, sumAxis1, 1, Core.REDUCE_SUM, CvType.CV_32S)
        var verticalPosition: ArrayList<Pair<Int, Int>> = helperExtract(sumAxis1, reservedThreshold * 4, 1)

        // make vertical_position has only one element
        if (verticalPosition.count() > 1) {
            verticalPosition = arrayListOf(Pair(verticalPosition[0].first, verticalPosition[verticalPosition.count() - 1].second))
        }


        for (h in horizonPosition) {
            for (v in verticalPosition) {
                digitsPositions.add(arrayListOf(Pair(h.first, v.first), Pair(h.second, v.second)))
            }
        }
        assert(digitsPositions.count() > 0)
    }

    private fun recognizeDigitsAreaMethod() {
        val input = dst
        val output = blurredImg

        val digits: ArrayList<String> = arrayListOf()

        for (c in digitsPositions) {
            var x0 = c[0].first
            val y0 = c[0].second
            val x1 = c[1].first
            val y1 = c[1].second
            var roi = Mat(input, Range(y0, y1), Range(x0, x1))
            val h = roi.cols()
            var w = roi.rows()
            val supposeW = max(1, (h / H_W_Ratio).toInt())

            if (x1 - x0 < 25 && Core.countNonZero(roi) / ((y1 - y0) * (x1 - x0)) < 0.2)
                continue

            if (w < supposeW / 2) {
                x0 = max(x0 + w - supposeW, 0)
                roi = Mat(input, Range(y0, y1), Range(x0, x1))
                w = roi.rows()
            }

            val centerY = h / 2
            val quaterY1 = h / 4
            val quaterY3 = quaterY1 * 3
            val centerX = w / 2
            val lineWidth = 5

            val width = (max((w * 0.15).toInt(), 1) + max((h * 0.15).toInt(), 1)) / 2
            val smallDelta = ((h / ARC_TAN_THETA) / 4).toInt()

            val segments = arrayListOf(
                Pair(Pair(w - 2 * width, quaterY1 - lineWidth), Pair(w, quaterY1 + lineWidth)),
                Pair(Pair(w - 2 * width, quaterY3 - lineWidth), Pair(w, quaterY3 + lineWidth)),
                Pair(Pair(centerX - lineWidth - smallDelta, h - 2 * width), Pair(centerX - smallDelta + lineWidth, h)),
                Pair(Pair(0, quaterY3 - lineWidth), Pair(2 * width, quaterY3 + lineWidth)),
                Pair(Pair(0, quaterY1 - lineWidth), Pair(2 * width, quaterY1 + lineWidth)),
                Pair(Pair(centerX - lineWidth, 0), Pair(centerX + lineWidth, 2 * width)),
                Pair(Pair(centerX - lineWidth, centerY - lineWidth), Pair(centerX + lineWidth, centerY + lineWidth))
            )

            val on = Array(segments.count()) { 0 }

            for ((i, segment) in segments.withIndex()) {
                val xa = segment.first.first
                val ya = segment.first.second
                val xb = segment.second.first
                val yb = segment.second.second

                val segRoi = Mat(roi, Range(ya, yb), Range(xa, xb))

                val total = Core.countNonZero(segRoi)
                val area = (xb - xa) * (yb - ya) * 0.9

                if (total / area > 0.25) {
                    on[i] = 1
                }
            }

            val digit = DIGITS_LOOKUP[on] ?: "*"
            digits.add(digit)
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
}
