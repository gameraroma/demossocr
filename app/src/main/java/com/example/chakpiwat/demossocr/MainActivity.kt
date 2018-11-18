package com.example.chakpiwat.demossocr

import android.graphics.Bitmap
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

class MainActivity : AppCompatActivity() {

    private lateinit var loaderCallback: BaseLoaderCallback

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
            val img: Mat = Utils.loadResource(this, R.drawable.test1)
            val grayImg = Mat()
            Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_RGB2GRAY)

            val high = grayImg.size().height
            val width = grayImg.size().width

            val blurredImg = Mat()
            Imgproc.GaussianBlur(grayImg, blurredImg, Size(7.0,7.0), 0.0)

            val bitmap = Bitmap.createBitmap(blurredImg.cols(), blurredImg.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(blurredImg, bitmap)
            imageView.setImageBitmap(bitmap)
            imageView.invalidate()
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
