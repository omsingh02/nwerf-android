package com.omsingh.nwerf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

// JSON Parser Configuration
private val json = Json { ignoreUnknownKeys = true }
private val client = OkHttpClient()

@Serializable
private data class TelegramFileResponse(val ok: Boolean, val result: TelegramFileResult? = null)

@Serializable
private data class TelegramFileResult(val file_path: String? = null)

@Serializable
private data class TelegramAudioResponse(val ok: Boolean, val result: TelegramAudioResult? = null)

@Serializable
private data class TelegramAudioResult(val audio: TelegramAudioDetails? = null)

@Serializable
private data class TelegramAudioDetails(val file_id: String)

class TelegramClient(private val botToken: String, private val chatId: String) {

    suspend fun uploadAudio(file: File, title: String, artist: String): String = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("title", title)
            .addFormDataPart("performer", artist)
            .addFormDataPart(
                "audio",
                file.name,
                file.asRequestBody("audio/mpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendAudio")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Upload failed: ${response.code} - ${response.message}")
            val bodyString = response.body?.string() ?: throw IOException("Empty response body")
            val res = json.decodeFromString<TelegramAudioResponse>(bodyString)
            if (!res.ok || res.result?.audio == null) throw IOException("Telegram error: $bodyString")
            res.result.audio.file_id
        }
    }

    suspend fun getStreamUrl(fileId: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/getFile?file_id=$fileId")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GetFile failed: ${response.code}")
            val bodyString = response.body?.string() ?: throw IOException("Empty response")
            val res = json.decodeFromString<TelegramFileResponse>(bodyString)
            if (!res.ok || res.result?.file_path == null) throw IOException("Telegram getFile error: $bodyString")
            "https://api.telegram.org/file/bot$botToken/${res.result.file_path}"
        }
    }
}

// GitHub Gist API Models
@Serializable
private data class GistCreateRequest(
    val description: String,
    val public: Boolean,
    val files: Map<String, GistFileContent>
)

@Serializable
private data class GistFileContent(val content: String)

@Serializable
private data class GistResponse(
    val id: String,
    val files: Map<String, GistFileResponse>
)

@Serializable
private data class GistFileResponse(val content: String? = null, val raw_url: String? = null)

class GithubClient(private val pat: String) {

    private val authHeader = "Bearer $pat"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun createLibraryGist(): String = withContext(Dispatchers.IO) {
        val payload = GistCreateRequest(
            description = "Nwerf Library Backup",
            public = false,
            files = mapOf("nwerf-library.json" to GistFileContent("[]"))
        )
        val body = json.encodeToString(GistCreateRequest.serializer(), payload)
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("https://api.github.com/gists")
            .header("Authorization", authHeader)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to create Gist: ${response.code} - ${response.message}")
            val bodyString = response.body?.string() ?: throw IOException("Empty response")
            val res = json.decodeFromString<GistResponse>(bodyString)
            res.id
        }
    }

    suspend fun fetchLibraryTracks(gistId: String): List<Track> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/gists/$gistId")
            .header("Authorization", authHeader)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch Gist: ${response.code}")
            val bodyString = response.body?.string() ?: throw IOException("Empty response")
            val res = json.decodeFromString<GistResponse>(bodyString)
            val file = res.files["nwerf-library.json"] ?: throw IOException("Library file not found in Gist")
            val content = file.content ?: throw IOException("Gist library content is empty")
            json.decodeFromString<List<Track>>(content)
        }
    }

    suspend fun updateLibraryGist(gistId: String, tracks: List<Track>): Unit = withContext(Dispatchers.IO) {
        val content = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Track.serializer()), tracks)
        val payload = GistCreateRequest(
            description = "Nwerf Library Backup",
            public = false,
            files = mapOf("nwerf-library.json" to GistFileContent(content))
        )
        val body = json.encodeToString(GistCreateRequest.serializer(), payload)
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("https://api.github.com/gists/$gistId")
            .header("Authorization", authHeader)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .patch(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to update Gist: ${response.code} - ${response.message}")
        }
    }
}
