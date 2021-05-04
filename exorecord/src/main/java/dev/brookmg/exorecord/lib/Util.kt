package dev.brookmg.exorecord.lib

import java.nio.ByteBuffer

object Util {

    fun Int.toByteArray(capacity: Int) : ByteArray {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(capacity)
        byteBuffer.putInt(this)
        return byteBuffer.array().reversedArray()
    }

    fun Short.toByteArray(capacity: Int) : ByteArray {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(capacity)
        byteBuffer.putShort(this)
        return byteBuffer.array().reversedArray()
    }

}