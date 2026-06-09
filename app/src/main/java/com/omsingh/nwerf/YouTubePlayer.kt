package com.omsingh.nwerf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.regex.Pattern

object YouTubePlayer {
    private val client = OkHttpClient()

    suspend fun searchYouTube(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val html = response.body?.string() ?: return@withContext null
                val pattern = Pattern.compile("\"videoId\":\"([^\"]+)\"")
                val matcher = pattern.matcher(html)

                if (matcher.find()) {
                    val videoId = matcher.group(1)
                    return@withContext "https://www.youtube.com/watch?v=$videoId"
                }
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
