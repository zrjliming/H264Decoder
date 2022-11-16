package com.cst.h264decoderdemo

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface


class H264Decoder(
    name: String,
    width: Int,
    height: Int,
    data: ByteArray,
    surface: Surface,
) : Thread() {
    private val TAG = "H264Decoder"
    var bytes: ByteArray? = null
    var mediaCodec: MediaCodec
    var surfaceName: String

    init {
        // demo测试，为方便一次性读取到内存
//        bytes = FileUtil.getBytes(path)
        bytes = data
        surfaceName=name
        // video/avc就是H264，创建解码器
        mediaCodec = MediaCodec.createDecoderByType("video/avc")
        val mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height)
        Log.e(TAG, "init mediaCodecName "+mediaCodec.name+"  width: " + width+" height "+height)
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mediaCodec.configure(mediaFormat, surface, null, 0)
        test()
    }

    override fun run() {
        mediaCodec.start()
        decodeSplitNalu()
//        decodeFixByte()
        mediaCodec.stop()
        mediaCodec.release()
    }

    @Volatile
    private var encodeFrameCount = 0

    private fun decodeSplitNalu() {
        if (bytes == null) {
            return
        }
        // 数据开始下标
        var startFrameIndex = 0
        val totalSizeIndex = bytes!!.size - 1
        Log.i(TAG, "totalSize=$totalSizeIndex")
        val inputBuffers = mediaCodec.inputBuffers
        val info = MediaCodec.BufferInfo()
        while (true) {
            // 1ms=1000us 微妙
            val inIndex = mediaCodec.dequeueInputBuffer(10_000)
            if (inIndex >= 0) {
                // 分割出一帧数据
                if (totalSizeIndex == 0 || startFrameIndex >= totalSizeIndex) {
                    Log.e(TAG, "startIndex >= totalSize-1 ,break")
                    break
                }
                val nextFrameStartIndex: Int =
                    findNextFrame(bytes!!, startFrameIndex + 1, totalSizeIndex)
                if (nextFrameStartIndex == -1) {
                    Log.e(TAG, "nextFrameStartIndex==-1 break")
                    break
                }
                // 填充数据
                val byteBuffer = inputBuffers[inIndex]
                byteBuffer.clear()
                byteBuffer.put(bytes!!, startFrameIndex, nextFrameStartIndex - startFrameIndex)

                mediaCodec.queueInputBuffer(inIndex, 0, nextFrameStartIndex - startFrameIndex, 0, 0)

                startFrameIndex = nextFrameStartIndex

            }else{
                Log.e(TAG, " dequeueInput error  inIndex "+inIndex)
            }
            var outIndex = mediaCodec.dequeueOutputBuffer(info, 10_000)
            Log.e(TAG, " surfaceName "+surfaceName+" decoder : " + encodeFrameCount+" outIndex"+outIndex)
            while (outIndex >= 0) {
                encodeFrameCount++
                // 这里用简单的时间方式保持视频的fps，不然视频会播放很快
                // demo 的H264文件是30fps
                try {
                    sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                // 参数2 渲染到surface上，surface就是mediaCodec.configure的参数2
                mediaCodec.releaseOutputBuffer(outIndex, true)
                outIndex = mediaCodec.dequeueOutputBuffer(info, 0)
            }
        }
    }

    private fun test(){
        var encodeFrameThread = Thread {
            while (true) {
                Log.e(TAG, " surfaceName "+surfaceName+" decoder frameCount: " + encodeFrameCount/5)
                encodeFrameCount = 0
                try {
                    sleep(5000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        encodeFrameThread.start()
    }


    private fun findNextFrame(bytes: ByteArray, startIndex: Int, totalSizeIndex: Int): Int {
        for (i in startIndex..totalSizeIndex) {
            // 00 00 00 01 H264的启始码
            if (bytes[i].toInt() == 0x00 && bytes[i + 1].toInt() == 0x00 && bytes[i + 2].toInt() == 0x00 && bytes[i + 3].toInt() == 0x01) {
//                Log.e(TAG, "bytes[i+4]=0X${Integer.toHexString(bytes[i + 4].toInt())}")
//                Log.e(TAG, "bytes[i+4]=${(bytes[i + 4].toInt().and(0X1F))}")
                return i
                // 00 00 01 H264的启始码
            } else if (bytes[i].toInt() == 0x00 && bytes[i + 1].toInt() == 0x00 && bytes[i + 2].toInt() == 0x01) {
//                Log.e(TAG, "bytes[i+3]=0X${Integer.toHexString(bytes[i + 3].toInt())}")
//                Log.e(TAG, "bytes[i+3]=${(bytes[i + 3].toInt().and(0X1F))}")
                return i
            }
        }
        return -1
    }


}
