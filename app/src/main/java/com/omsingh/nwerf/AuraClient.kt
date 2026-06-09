package com.omsingh.nwerf

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AuraClient(private val apiKey: String) {
    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // Replace this with the actual Hugging Face Space URL
    private val apiUrl = "https://ghutck-aura-api.hf.space"

    fun identify(audioFile: File): Track? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio_file", audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("$apiUrl/identify")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw Exception("API Error ${response.code}: $errorBody")
                }

                val jsonString = response.body?.string() ?: throw Exception("Empty response body")
                val json = JSONObject(jsonString)

                if (!json.optBoolean("success", false)) {
                    throw Exception(json.optString("error", "Unknown API error"))
                }

                val metadata = json.getJSONObject("metadata")
            
            return Track(
                id = java.util.UUID.randomUUID().toString(),
                title = metadata.optString("title", "Unknown"),
                artist = metadata.optString("artist", "Unknown"),
                file_id = "", // Will be filled later if downloaded
                added_at = System.currentTimeMillis(),
                cover_art = metadata.optString("cover_art", null).takeIf { it.isNotBlank() },
                lyrics = metadata.optString("lyrics", null).takeIf { it.isNotBlank() },
                album = metadata.optString("album", null).takeIf { it.isNotBlank() },
                release_date = metadata.optString("release_date", null).takeIf { it.isNotBlank() },
                shazam_count = if (metadata.has("shazam_count")) metadata.getInt("shazam_count") else null,
                genres = metadata.optString("genres", null).takeIf { it.isNotBlank() },
                apple_music_url = metadata.optString("apple_music_url", null).takeIf { it.isNotBlank() }
            )
            }
        } catch (e: java.net.UnknownHostException) {
            throw Exception("You appear to be offline. Please check your internet connection.")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("The connection timed out. The server might be asleep or unreachable.")
        } catch (e: Exception) {
            throw e
        }
    }

    fun downloadAndUpload(title: String, artist: String, botToken: String, chatId: String): String {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", title)
            .addFormDataPart("artist", artist)
            .addFormDataPart("bot_token", botToken)
            .addFormDataPart("chat_id", chatId)
            .build()

        val request = Request.Builder()
            .url("$apiUrl/download")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw Exception("API Error ${response.code}: $errorBody")
                }

                val jsonString = response.body?.string() ?: throw Exception("Empty response body")
                val json = JSONObject(jsonString)

                if (!json.optBoolean("success", false)) {
                    throw Exception(json.optString("error", "Unknown API error"))
                }

                return json.getString("file_id")
            }
        } catch (e: java.net.UnknownHostException) {
            throw Exception("You appear to be offline. Please check your internet connection.")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("The connection timed out. The server might be asleep or unreachable.")
        } catch (e: Exception) {
            throw e
        }
    }
}
