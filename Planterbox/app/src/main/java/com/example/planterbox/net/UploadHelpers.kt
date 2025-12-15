package com.example.planterbox.net

import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

fun bitmapToImagePart(bitmap: Bitmap): MultipartBody.Part {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
    val bytes = baos.toByteArray()
    val body = bytes.toRequestBody("image/jpeg".toMediaType())
    return MultipartBody.Part.createFormData("image", "upload.jpg", body)
}

fun textPart(value: String) =
    value.toRequestBody("text/plain".toMediaType())
