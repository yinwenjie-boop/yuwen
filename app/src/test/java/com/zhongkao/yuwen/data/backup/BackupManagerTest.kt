package com.zhongkao.yuwen.data.backup

import com.zhongkao.yuwen.data.db.Attempt
import com.zhongkao.yuwen.data.db.Badge
import com.zhongkao.yuwen.data.db.Exercise
import com.zhongkao.yuwen.data.db.Question
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.data.db.WeakPointStat
import com.zhongkao.yuwen.data.db.WrongQuestion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    /** 内存假实现：BackupStore 是窄接口，单测无需 Room。 */
    private class FakeBackupStore : BackupStore {
        var progress: UserProgress? = null
        val weak = mutableListOf<WeakPointStat>()
        val badges = mutableListOf<Badge>()
        val wrongs = mutableListOf<WrongQuestion>()
        val questions = mutableMapOf<Long, Question>()
        val exercises = mutableMapOf<Long, Exercise>()
        val attempts = mutableListOf<Attempt>()
        private var seq = 0L
        private fun next() = ++seq

        override suspend fun loadProgress() = progress
        override suspend fun loadWeakPoints() = weak.toList()
        override suspend fun loadBadges() = badges.toList()
        override suspend fun loadWrongs() = wrongs.toList()
        override suspend fun loadQuestion(id: Long) = questions[id]
        override suspend fun loadExercise(id: Long) = exercises[id]
        override suspend fun loadLatestAttempt(questionId: Long) =
            attempts.filter { it.questionId == questionId }.maxByOrNull { it.id }

        override suspend fun saveProgress(p: UserProgress) { progress = p }
        override suspend fun upsertWeakPoint(s: WeakPointStat) {
            weak.removeAll { it.abilityTag == s.abilityTag }; weak += s
        }
        override suspend fun insertBadge(b: Badge): Long { val id = next(); badges += b.copy(id = id); return id }
        override suspend fun insertExercise(e: Exercise): Long { val id = next(); exercises[id] = e.copy(id = id); return id }
        override suspend fun insertQuestion(q: Question): Long { val id = next(); questions[id] = q.copy(id = id); return id }
        override suspend fun insertAttempt(a: Attempt): Long { val id = next(); attempts += a.copy(id = id); return id }
        override suspend fun insertWrong(w: WrongQuestion): Long { val id = next(); wrongs += w.copy(id = id); return id }
    }

    private suspend fun seed(store: FakeBackupStore) {
        val exId = store.insertExercise(Exercise(type = "wenyan_compare", createdAt = 100, status = "graded", totalTimeSec = 300))
        val qId = store.insertQuestion(
            Question(
                exerciseId = exId, genId = "q1", qType = "句子翻译", stem = "翻译此句", refAnswer = "参考译文",
                scorePoints = """[{"point":"关键词","score":4}]""", maxScore = 4, abilityTag = "句子翻译"
            )
        )
        store.insertAttempt(
            Attempt(
                questionId = qId, studentAnswer = "我的翻译", gotScore = 2, errorType = "漏译关键词",
                correctAnswer = "规范译文", explanation = "易错点", tip = "复习实词", timeSpentSec = 120, gradedAt = 200
            )
        )
        store.insertWrong(WrongQuestion(questionId = qId, abilityTag = "句子翻译", addedAt = 150, masteredFlag = false))
        store.upsertWeakPoint(WeakPointStat("句子翻译", attempts = 3, wrongCount = 2, avgTimeSec = 90, lastUpdated = 150))
        store.insertBadge(Badge(code = "fast_accurate", name = "又快又准", earnedAt = 160))
        store.progress = UserProgress(xp = 120, level = 2, levelTitle = "文言行者", coins = 30, streakDays = 5, lastPracticeDate = "2026-05-30")
    }

    @Test
    fun `导出再导入可还原错题薄弱点与进度`() = runTest {
        val src = FakeBackupStore().also { seed(it) }
        val json = BackupManager(src, now = { 999L }).exportToJson()

        // 备份绝不含 API Key。
        assertFalse(json.contains("api_key", ignoreCase = true))

        val dst = FakeBackupStore()
        val summary = BackupManager(dst, now = { 999L }).importFromJson(json)

        assertEquals(1, summary.wrongQuestions)
        assertEquals(1, summary.weakPoints)
        assertEquals(1, summary.badges)
        assertTrue(summary.progressRestored)

        // 错题自包含：题目+作答+所属练习被重建，错题本 JOIN 可用。
        assertEquals(1, dst.wrongs.size)
        val w = dst.wrongs.first()
        val q = dst.questions[w.questionId]!!
        assertEquals("翻译此句", q.stem)
        assertEquals("句子翻译", q.abilityTag)
        val ex = dst.exercises[q.exerciseId]!!
        assertEquals("wenyan_compare", ex.type)
        // 重建练习标 restored，不污染「近 N 次用时曲线」(只取 graded)。
        assertEquals(BackupManager.STATUS_RESTORED, ex.status)
        val at = dst.attempts.single { it.questionId == w.questionId }
        assertEquals(2, at.gotScore)
        assertEquals("规范译文", at.correctAnswer)

        assertEquals(120, dst.progress!!.xp)
        assertEquals("文言行者", dst.progress!!.levelTitle)
        assertEquals("句子翻译", dst.weak.single().abilityTag)
        assertEquals(2, dst.weak.single().wrongCount)
        assertEquals("又快又准", dst.badges.single().name)
    }

    @Test
    fun `导入非法JSON抛出可读异常且不改动数据`() = runTest {
        val dst = FakeBackupStore()
        val e = runCatching { BackupManager(dst).importFromJson("这不是备份文件") }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
        assertTrue(dst.wrongs.isEmpty())
        assertEquals(null, dst.progress)
    }
}
