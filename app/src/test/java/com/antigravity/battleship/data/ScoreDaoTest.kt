package com.antigravity.battleship.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScoreDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ScoreDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.scoreDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and retrieve score`() = runTest {
        dao.insert(ScoreEntity(missilesUsed = 25))
        val scores = dao.getAllScores().first()
        assertEquals(1, scores.size)
        assertEquals(25, scores[0].missilesUsed)
    }

    @Test
    fun `scores ordered by missilesUsed ascending`() = runTest {
        dao.insert(ScoreEntity(missilesUsed = 50))
        dao.insert(ScoreEntity(missilesUsed = 20))
        dao.insert(ScoreEntity(missilesUsed = 35))
        val scores = dao.getAllScores().first()
        assertEquals(listOf(20, 35, 50), scores.map { it.missilesUsed })
    }

    @Test
    fun `multiple scores stored independently`() = runTest {
        dao.insert(ScoreEntity(missilesUsed = 10))
        dao.insert(ScoreEntity(missilesUsed = 10))
        val scores = dao.getAllScores().first()
        assertEquals(2, scores.size)
    }

    @Test
    fun `score entity has auto-generated id`() = runTest {
        dao.insert(ScoreEntity(missilesUsed = 30))
        dao.insert(ScoreEntity(missilesUsed = 40))
        val scores = dao.getAllScores().first()
        assertNotEquals(scores[0].id, scores[1].id)
    }

    @Test
    fun `score entity stores date`() = runTest {
        val before = System.currentTimeMillis()
        dao.insert(ScoreEntity(missilesUsed = 15))
        val after = System.currentTimeMillis()
        val score = dao.getAllScores().first()[0]
        assertTrue(score.date in before..after)
    }

    @Test
    fun `empty database returns empty list`() = runTest {
        val scores = dao.getAllScores().first()
        assertTrue(scores.isEmpty())
    }
}
