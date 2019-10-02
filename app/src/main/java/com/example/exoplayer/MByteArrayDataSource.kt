package com.example.exoplayer

import android.net.Uri

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec

import java.io.IOException

/**
 * Created by Anže Kožar on 12.3.2017.
 */

class MByteArrayDataSource(private val data: ByteArray) : DataSource {

    private var uri: Uri? = null
    private var readPosition: Int = 0
    private var bytesRemaining: Int = 0

    init {
        readPosition = 0
        bytesRemaining = data.size
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        readPosition = dataSpec.position.toInt()
        bytesRemaining = (if (dataSpec.length == C.LENGTH_UNSET.toLong())
            data.size - dataSpec.position
        else
            dataSpec.length).toInt()
        if (bytesRemaining <= 0 || readPosition + bytesRemaining > data.size) {
            throw IOException(
                "Unsatisfiable range: [" + readPosition + ", " + dataSpec.length
                        + "], length: " + data.size
            )
        }
        return bytesRemaining.toLong()
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        var readLength = readLength
        if (readLength == 0) {
            return 0
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT
        }

        readLength = Math.min(readLength, bytesRemaining)
        System.arraycopy(data, readPosition, buffer, offset, readLength)
        readPosition += readLength
        bytesRemaining -= readLength
        return readLength
    }

    override fun getUri(): Uri? {
        return uri
    }

    @Throws(IOException::class)
    override fun close() {
        uri = null
    }
}
