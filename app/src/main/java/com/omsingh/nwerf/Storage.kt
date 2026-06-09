package com.omsingh.nwerf

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

// Datastore for Credentials
val Context.dataStore by preferencesDataStore(name = "nwerf_settings")

class SettingsStore(private val context: Context) {
    companion object {
        val BOT_TOKEN = stringPreferencesKey("bot_token")
        val CHAT_ID = stringPreferencesKey("chat_id")
        val GITHUB_PAT = stringPreferencesKey("github_pat")
        val GIST_ID = stringPreferencesKey("gist_id")
        val HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
    }

    val botToken: Flow<String?> = context.dataStore.data.map { it[BOT_TOKEN] }
    val chatId: Flow<String?> = context.dataStore.data.map { it[CHAT_ID] }
    val githubPat: Flow<String?> = context.dataStore.data.map { it[GITHUB_PAT] }
    val gistId: Flow<String?> = context.dataStore.data.map { it[GIST_ID] }
    val hasSeenTutorial: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_TUTORIAL] ?: false }

    suspend fun saveTelegramSettings(token: String, chat: String) {
        context.dataStore.edit {
            it[BOT_TOKEN] = token
            it[CHAT_ID] = chat
        }
    }

    suspend fun saveGistSettings(pat: String, gist: String) {
        context.dataStore.edit {
            it[GITHUB_PAT] = pat
            it[GIST_ID] = gist
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun setTutorialSeen() {
        context.dataStore.edit { it[HAS_SEEN_TUTORIAL] = true }
    }
}

// Room Database for Tracks Library
@Serializable
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val file_id: String,
    val added_at: Long,
    val cover_art: String? = null,
    val lyrics: String? = null,
    val album: String? = null,
    val release_date: String? = null,
    val shazam_count: Int? = null,
    val genres: String? = null,
    val apple_music_url: String? = null
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY added_at DESC")
    fun getAllTracksFlow(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY added_at DESC")
    suspend fun getAllTracks(): List<Track>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<Track>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: Track)

    @Delete
    suspend fun delete(track: Track)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

@Database(entities = [Track::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nwerf_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
