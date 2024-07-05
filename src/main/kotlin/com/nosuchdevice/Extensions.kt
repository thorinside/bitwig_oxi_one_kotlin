package com.nosuchdevice

fun String.decodeHex(): ByteArray {
  return replace(" ","").chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
