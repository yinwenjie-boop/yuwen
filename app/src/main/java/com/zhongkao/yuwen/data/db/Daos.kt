package com.zhongkao.yuwen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO 骨架（阶段 0）。只给后续阶段需要的最小查询面；具体业务查询在各阶段补。
 */

@Dao
interface TextBankDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TextBank>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TextBank): Long

    @Query("SELECT * FROM text_bank WHERE category = :category ORDER BY title")
    fun observeByCategory(category: String): Flow<List<TextBank>>

    @Query("SELECT * FROM text_bank WHERE id = :id")
    suspend fun findById(id: Long): TextBank?

    @Query("SELECT * FROM text_bank WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): TextBank?

    @Query("SELECT * FROM text_bank WHERE category = :category AND verified = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun randomVerified(category: String): TextBank?

    @Query("SELECT COUNT(*) FROM text_bank")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM text_bank WHERE category = :category")
    fun observeCountByCategory(category: String): Flow<Int>

    // —— 阶段 5：AI 提议课外篇的人工确认流（verified=0 即「待确认」）——
    @Query("SELECT * FROM text_bank WHERE category = :category AND verified = 0 ORDER BY id DESC")
    fun observePending(category: String): Flow<List<TextBank>>

    @Query("SELECT COUNT(*) FROM text_bank WHERE category = :category AND verified = 0")
    fun observePendingCount(category: String): Flow<Int>

    @Query("UPDATE text_bank SET verified = :verified WHERE id = :id")
    suspend fun setVerified(id: Long, verified: Boolean)

    @Query("DELETE FROM text_bank WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/** 用时曲线投影：仅取画曲线所需字段，避免拉出大段出题/批改快照。 */
data class ExerciseTimePoint(
    val id: Long,
    val createdAt: Long,
    val totalTimeSec: Int,
    val type: String
)

/** 练习历史列表投影：含设置/批改快照（解析出难度、文体、得分），不拉大段出题快照 payloadJson。 */
data class ExerciseSummary(
    val id: Long,
    val type: String,
    val createdAt: Long,
    val totalTimeSec: Int,
    val configJson: String,
    val gradingJson: String
)

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun findById(id: Long): Exercise?

    @Query("SELECT * FROM exercise ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT id, createdAt, totalTimeSec, type FROM exercise WHERE status = 'graded' ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentGraded(limit: Int): Flow<List<ExerciseTimePoint>>

    /** 练习历史：已批改的练习，新→旧。 */
    @Query("SELECT id, type, createdAt, totalTimeSec, configJson, gradingJson FROM exercise WHERE status = 'graded' ORDER BY createdAt DESC")
    fun observeGradedSummaries(): Flow<List<ExerciseSummary>>

    // —— 删除一次练习的级联清理（错题→作答→题目→练习）——
    @Query("DELETE FROM wrong_question WHERE questionId IN (SELECT id FROM question WHERE exerciseId = :exerciseId)")
    suspend fun deleteWrongOfExercise(exerciseId: Long)

    @Query("DELETE FROM attempt WHERE questionId IN (SELECT id FROM question WHERE exerciseId = :exerciseId)")
    suspend fun deleteAttemptsOfExercise(exerciseId: Long)

    @Query("DELETE FROM question WHERE exerciseId = :exerciseId")
    suspend fun deleteQuestionsOfExercise(exerciseId: Long)

    @Query("DELETE FROM exercise WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)

    /** 原子级联删除：错题→作答→题目→练习，避免留下孤儿行。 */
    @Transaction
    suspend fun deleteExerciseCascade(exerciseId: Long) {
        deleteWrongOfExercise(exerciseId)
        deleteAttemptsOfExercise(exerciseId)
        deleteQuestionsOfExercise(exerciseId)
        deleteExerciseById(exerciseId)
    }
}

@Dao
interface QuestionDao {
    @Insert
    suspend fun insertAll(questions: List<Question>): List<Long>

    @Query("SELECT * FROM question WHERE exerciseId = :exerciseId")
    suspend fun byExercise(exerciseId: Long): List<Question>

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun findById(id: Long): Question?
}

@Dao
interface AttemptDao {
    @Insert
    suspend fun insertAll(attempts: List<Attempt>): List<Long>

    @Insert
    suspend fun insert(attempt: Attempt): Long

    @Update
    suspend fun update(attempt: Attempt)

    @Query("SELECT * FROM attempt WHERE questionId = :questionId ORDER BY id DESC LIMIT 1")
    suspend fun latestByQuestion(questionId: Long): Attempt?

    @Query("SELECT * FROM attempt WHERE questionId = :questionId ORDER BY gradedAt DESC")
    suspend fun byQuestion(questionId: Long): List<Attempt>
}

/**
 * 错题本展示用投影：题干 / 题型 / 所属练习类型，
 * 并连带最近一次批改细节（你的作答 / 正确答法 / 错因 / 解析 / 提升建议 / 得分），
 * 供复习页错题卡展开显示。作答字段经 LEFT JOIN，缺失时用默认值。
 */
data class WrongQuestionDetail(
    val wrongId: Long,
    val questionId: Long,
    val abilityTag: String,
    val addedAt: Long,
    val qType: String,
    val stem: String,
    val exerciseId: Long,
    val apiType: String,
    val maxScore: Int = 0,
    val refAnswer: String = "",
    val studentAnswer: String = "",
    val gotScore: Int = 0,
    val correctAnswer: String = "",
    val errorType: String = "",
    val explanation: String = "",
    val tip: String = ""
)

@Dao
interface WrongQuestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WrongQuestion): Long

    @Query("UPDATE wrong_question SET masteredFlag = :mastered WHERE id = :id")
    suspend fun setMastered(id: Long, mastered: Boolean)

    @Query("SELECT * FROM wrong_question ORDER BY addedAt DESC")
    suspend fun getAll(): List<WrongQuestion>

    @Query("SELECT * FROM wrong_question WHERE masteredFlag = 0 ORDER BY addedAt DESC")
    fun observeUnmastered(): Flow<List<WrongQuestion>>

    @Query("SELECT COUNT(*) FROM wrong_question WHERE masteredFlag = 0")
    fun observeUnmasteredCount(): Flow<Int>

    @Query(
        """
        SELECT wq.id AS wrongId, wq.questionId AS questionId, wq.abilityTag AS abilityTag,
               wq.addedAt AS addedAt, q.qType AS qType, q.stem AS stem,
               q.exerciseId AS exerciseId, e.type AS apiType,
               q.maxScore AS maxScore, q.refAnswer AS refAnswer,
               IFNULL(a.studentAnswer, '') AS studentAnswer, IFNULL(a.gotScore, 0) AS gotScore,
               IFNULL(a.correctAnswer, '') AS correctAnswer, IFNULL(a.errorType, '') AS errorType,
               IFNULL(a.explanation, '') AS explanation, IFNULL(a.tip, '') AS tip
        FROM wrong_question wq
        JOIN question q ON wq.questionId = q.id
        JOIN exercise e ON q.exerciseId = e.id
        LEFT JOIN attempt a ON a.id = (
            SELECT id FROM attempt WHERE questionId = q.id ORDER BY gradedAt DESC, id DESC LIMIT 1
        )
        WHERE wq.masteredFlag = 0
        ORDER BY wq.addedAt DESC
        """
    )
    fun observeDetails(): Flow<List<WrongQuestionDetail>>
}

@Dao
interface WeakPointStatDao {
    @Upsert
    suspend fun upsert(stat: WeakPointStat)

    @Query("SELECT * FROM weak_point_stat ORDER BY wrongCount DESC LIMIT :limit")
    fun observeTop(limit: Int): Flow<List<WeakPointStat>>

    @Query("SELECT * FROM weak_point_stat ORDER BY wrongCount DESC, avgTimeSec DESC")
    fun observeAll(): Flow<List<WeakPointStat>>

    @Query("SELECT * FROM weak_point_stat")
    suspend fun getAll(): List<WeakPointStat>

    @Query("SELECT * FROM weak_point_stat WHERE abilityTag = :tag")
    suspend fun find(tag: String): WeakPointStat?
}

@Dao
interface UserProgressDao {
    @Upsert
    suspend fun upsert(progress: UserProgress)

    @Query("SELECT * FROM user_progress WHERE id = :id")
    fun observe(id: Int = UserProgress.SINGLETON_ID): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = :id")
    suspend fun get(id: Int = UserProgress.SINGLETON_ID): UserProgress?
}

@Dao
interface BadgeDao {
    @Insert
    suspend fun insert(badge: Badge): Long

    @Query("SELECT * FROM badge ORDER BY earnedAt DESC")
    fun observeAll(): Flow<List<Badge>>

    @Query("SELECT * FROM badge ORDER BY earnedAt DESC")
    suspend fun getAll(): List<Badge>
}
