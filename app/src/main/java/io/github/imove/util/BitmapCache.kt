package io.github.imove.util

import android.graphics.Bitmap
import androidx.collection.LruCache

object BitmapCache {
    private val cache = LruCache<String, Bitmap>(calculateMaxSize())

    private fun calculateMaxSize(): Int {
        val maxMemory = Runtime.getRuntime().maxMemory()
        // maxSize is in KB. Use 20% of max heap, leave rest for Coil + app
        return (maxMemory / 1024 * 0.20).toInt()
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun contains(key: String): Boolean = cache.get(key) != null
}
