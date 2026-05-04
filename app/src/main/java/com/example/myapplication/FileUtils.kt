package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

object FileUtils {
    fun getSize(uri: Uri, ctx: Context): Long? {
        val cursor = ctx.contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

            if (it.moveToFirst() && sizeIndex != -1) {
                return it.getLong(sizeIndex)
            }
        }

        return null
    }


    fun getStream(uri: Uri, ctx: Context): InputStream? {
        return ctx.contentResolver.openInputStream(uri)
    }
}
