package com.example.chakpiwat.demossocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.core.Mat
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.CLAHE
import org.opencv.core.CvType

class MainActivity : AppCompatActivity() {

    private lateinit var loaderCallback: BaseLoaderCallback

    private lateinit var grayImg: Mat
    private lateinit var blurredImg: Mat
    private lateinit var dst: Mat

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

        val clahe = Imgproc.createCLAHE()
        val claheMat = Mat(height.toInt(), width.toInt(), CvType.CV_8UC1)
        clahe.apply(blurredImg, claheMat)

        dst = Mat()
        Imgproc.adaptiveThreshold(grayImg, dst, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15,2.0)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, kernelSize)
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_OPEN, kernel)
    }

    private fun showImage() {
        // show blurred image
        val bitmap = Bitmap.createBitmap(blurredImg.cols(), blurredImg.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(blurredImg, bitmap)
        imageView.setImageBitmap(bitmap)
        imageView.invalidate()
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
