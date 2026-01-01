package com.voidDeveloper.wastatussaver.data.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.voidDeveloper.wastatussaver.data.datastore.proto.StatusMediaList
import java.io.InputStream
import java.io.OutputStream

object StatusMediaListSerializer : Serializer<StatusMediaList> {

    override val defaultValue: StatusMediaList = StatusMediaList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): StatusMediaList {
        try {
            return StatusMediaList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read StatusMediaList proto.", exception)
        }
    }

    override suspend fun writeTo(t: StatusMediaList, output: OutputStream) {
        t.writeTo(output)
    }
}