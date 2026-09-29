package com.cs2.tracker.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** 轻量图片加载：OkHttp 下载 + 内存 LruCache + 磁盘缓存（替代 coil，纯离线可构建） */
object ImageLoader {
    private lateinit var appContext: Context

    private val mem = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun key(url: String): String =
        MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun diskFile(url: String): File =
        File(appContext.cacheDir, "img/${key(url)}").also { it.parentFile?.mkdirs() }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        mem.get(url)?.let { return@withContext it }
        val f = diskFile(url)
        if (f.exists()) {
            BitmapFactory.decodeFile(f.path)?.let { mem.put(url, it); return@withContext it }
        }
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body!!.bytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    mem.put(url, bmp)
                    f.writeBytes(bytes)
                }
                bmp
            }
        }.getOrNull()
    }
}
