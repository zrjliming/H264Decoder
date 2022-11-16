package com.cst.h264decoderdemo

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue


class MainActivity : AppCompatActivity(), Camera.PreviewCallback {

    // 抽取aac音频文件
    // ffmpeg -i douyin.MP4 -acodec copy -vn  douyin.aac
    // 抽取H264视频文件
    // ffmpeg -i douyin.MP4  -c:v copy -bsf:v h264_mp4toannexb -an  douyin.h264

    private val TAG: String = "H264"
    private val fileName: String = "test2.h264"
    private var isWriteFile:Boolean = true

    @Volatile
    private var mIsPreview = false

    private var camera: Camera? = null

    var framerate = 24

    var biterate = 8500 * 1000

    private var avcCodec: H264AvcEncoder? = null



    private val width = 1280
    private val height = 720

    private lateinit var path: String
    private lateinit var holder: SurfaceHolder
    private lateinit var holder2: SurfaceHolder
    private lateinit var holder3: SurfaceHolder
    private lateinit var holder4: SurfaceHolder

    var bytes: ByteArray? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e(TAG, "displayMetrics" + resources.displayMetrics.toString())
        checkPermission()
        getSupportCodec()
        initEncoder()
        initSurface()
    }


    private fun checkPermission() {
        // 简单处理下权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        {
            val permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requestPermissions(permissions, 1)
        }
    }


    private fun initSurface() {
//        id_sf.holder.addCallback(object : Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                Log.e(TAG, "surfaceCreated")
//                this@MainActivity.holder = holder
//            }
//
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int,
//            ) {
//
//            }
//
//            override fun surfaceDestroyed(p0: SurfaceHolder) {
//
//            }
//        })
        id_sf2.holder.addCallback(object : Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.e(TAG, "surfaceCreated")
                this@MainActivity.holder2 = holder }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {}
        })
        id_sf3.holder.addCallback(object : Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.e(TAG, "surfaceCreated")
                this@MainActivity.holder3 = holder
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {}
        })
        id_sf4.holder.addCallback(object : Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.e(TAG, "surfaceCreated")
                this@MainActivity.holder4 = holder
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {}
        })

    }

    fun play(view: View) {
        checkPermission();
        // demo测试，为方便一次性读取到内存
      //  bytes?.let { H264Decoder("1", width, height, it, holder.surface).start() }
        path = Environment.getExternalStorageDirectory().toString() + File.separator + "H264/test1.h264"
        bytes = FileUtil.getBytes(path)
        bytes?.let { H264Decoder("2", width, height, it, holder2.surface).start() }
        bytes?.let { H264Decoder("3", width, height, it, holder3.surface).start() }
        bytes?.let { H264Decoder("4", width, height, it, holder4.surface).start() }

//        H264DecoderISaveImage(path, width, height, holder.surface).start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getSupportCodec() {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codecs = list.codecInfos
        Log.d(TAG, "Decoders:")
        for (codec in codecs) {
            if (!codec.isEncoder) Log.d(TAG, codec.name)
        }
        Log.d(TAG, "Encoders:")
        for (codec in codecs) {
            if (codec.isEncoder) Log.d(TAG, codec.name)
        }
    }

    companion object {
        @JvmStatic
        var YUVQueue = ArrayBlockingQueue<ByteArray>(10)
    }

    private fun initEncoder() {
        if(isWriteFile){
           FileUtil.deleteFile("/sdcard/h264/"+fileName)
        }

        id_sf.holder.addCallback(object : Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.e(TAG, "surfaceCreated")
                camera = getBackCamera()
                startCamera(camera)
                avcCodec= H264AvcEncoder(width, height, framerate, biterate, if(isWriteFile)fileName else null )
                avcCodec!!.StartEncoderThread()
                this@MainActivity.holder = holder
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                if (null != camera) {
                    camera!!.setPreviewCallback(null)
                    camera!!.stopPreview()
                    camera!!.release()
                    camera = null
                    avcCodec!!.StopThread()
                }
            }
        })
    }

    private fun startCamera(mCamera: Camera?) {
        if (mCamera == null) return
        try {
            mIsPreview = false
            val cameraParameters : Camera.Parameters = mCamera.parameters
            val camInfo = CameraInfo()
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, camInfo)
            val cameraRotationOffset = camInfo.orientation
            val rotate: Int = (360 + cameraRotationOffset - getDegree()) % 360

            // 设置预览参数
            cameraParameters.setRotation(rotate) // 设置方向
            val max: IntArray = determineMaximumSupportedFramerate(cameraParameters)
            cameraParameters.setPreviewFpsRange(max[0], max[1]) // 设置最大FPS
            cameraParameters.setPreviewSize(width, height) //设置预览尺寸
            cameraParameters.previewFormat = ImageFormat.NV21 // 设置预览格式
            mCamera.parameters = cameraParameters

            // 设置方向
            val displayRotation: Int
            displayRotation = (cameraRotationOffset - getDegree() + 360) % 360
            mCamera.setDisplayOrientation(displayRotation)
            mCamera.setPreviewDisplay(id_sf.holder)
            mCamera.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @TargetApi(9)
    private fun getBackCamera(): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open(0) // attempt to get a Camera instance
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return c // returns null if camera is unavailable
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        // TODO Auto-generated method stub
        putYUVData(data, data!!.size)
    }
    fun putYUVData(buffer: ByteArray?, length: Int) {
        if (YUVQueue.size >= 10) {
            YUVQueue.poll()
        }
        YUVQueue.add(buffer)
    }

    /**
     * 获取当前屏幕旋转角度
     * @return
     */
    private fun getDegree(): Int {
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        return degrees
    }

    /**
     * 获取支持的最大帧率
     * @param parameters
     * @return
     */
    fun determineMaximumSupportedFramerate(parameters: Camera.Parameters): IntArray {
        var maxFps = intArrayOf(0, 0)
        val supportedFpsRanges = parameters.supportedPreviewFpsRange
        val it: Iterator<IntArray> = supportedFpsRanges.iterator()
        while (it.hasNext()) {
            val interval = it.next()
            if (interval[1] > maxFps[1] || interval[0] > maxFps[0] && interval[1] == maxFps[1]) {
                maxFps = interval
            }
        }
        return maxFps
    }

}

