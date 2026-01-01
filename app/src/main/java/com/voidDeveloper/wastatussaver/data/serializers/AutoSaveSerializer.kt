package com.voidDeveloper.wastatussaver.data.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveUserPref
import java.io.InputStream
import java.io.OutputStream

object AutoSaveSerializer : Serializer<AutoSaveUserPref> {

    override val defaultValue: AutoSaveUserPref = AutoSaveUserPref.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AutoSaveUserPref {
        try {
            return AutoSaveUserPref.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read AutoSaveUserPref proto.", exception)
        }
    }

    override suspend fun writeTo(t: AutoSaveUserPref, output: OutputStream) {
        t.writeTo(output)
    }

}
