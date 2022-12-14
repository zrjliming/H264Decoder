package com.cst.h264decoderdemo

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream


object FileUtil {

    fun deleteFile(fileName: String?): Boolean {
        val file = File(fileName)
        return file.exists() && file.delete()
    }

    fun getBytes(path: String): ByteArray {
        val fos = FileInputStream(File(path))
        val bos = ByteArrayOutputStream()
        var len: Int
        val size = 8192
        var buf = ByteArray(size)
        while (true) {
            len = fos.read(buf, 0, size)
            if (len == -1) {
                break
            } else {
                bos.write(buf, 0, len)
            }
        }
        bos.flush()
        buf = bos.toByteArray()
        bos.close()
        fos.close()
        return buf
    }
}
