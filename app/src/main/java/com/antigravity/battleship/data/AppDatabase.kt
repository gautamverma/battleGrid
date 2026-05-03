package com.antigravity.battleship.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val missilesUsed: Int,
    val date: Long = System.currentTimeMillis()
)

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY missilesUsed ASC")
    fun getAllScores(): Flow<List<ScoreEntity>>

    @Insert
    suspend fun insert(score: ScoreEntity)
}

@Database(entities = [ScoreEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "battleship.db"
                ).build().also { INSTANCE = it }
            }
    }
}
