package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity used strictly for on-device playback position and stream metadata cache.
 * Not a replacement for the ShareDabba website database.
 */
@Entity(tableName = "cached_streams")
data class CachedStreamEntity(
    @PrimaryKey val streamId: String,
    val title: String,
    val streamUrl: String,
    val lastPlaybackPositionMs: Long = 0L,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface StreamCacheDao {
    @Query("SELECT * FROM cached_streams ORDER BY cachedAtEpochMs DESC")
    fun observeCachedStreams(): Flow<List<CachedStreamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreamCache(item: CachedStreamEntity)

    @Query("DELETE FROM cached_streams WHERE streamId = :streamId")
    suspend fun removeCachedStream(streamId: String)

    @Query("DELETE FROM cached_streams")
    suspend fun clearAllCache()
}

@Database(entities = [CachedStreamEntity::class], version = 1, exportSchema = false)
abstract class ShareDabbaCacheDatabase : RoomDatabase() {
    abstract fun streamCacheDao(): StreamCacheDao

    companion object {
        @Volatile
        private var INSTANCE: ShareDabbaCacheDatabase? = null

        fun getInstance(context: Context): ShareDabbaCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShareDabbaCacheDatabase::class.java,
                    "sharedabba_local_cache.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
