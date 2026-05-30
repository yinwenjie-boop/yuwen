package com.zhongkao.yuwen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}

/** 用时曲线投影：仅取画曲线所需字段，避免拉出大段出题/批改快照。 */
data class ExerciseTimePoint(
    val id: Long,
    val createdAt: Long,
    val totalTimeSec: Int,
    val type: String
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

/** 错题本展示用投影：连带题干 / 题型 / 所属练习类型（按考点归类时用）。 */
data class WrongQuestionDetail(
    val wrongId: Long,
    val questionId: Long,
    val abilityTag: String,
    val addedAt: Long,
    val qType: String,
    val stem: String,
    val exerciseId: Long,
    val apiType: String
)

@Dao
interface WrongQuestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WrongQuestion): Long

    @Query("UPDATE wrong_question SET masteredFlag = :mastered WHERE id = :id")
    suspend fun setMastered(id: Long, mastered: Boolean)

    @Query("SELECT * FROM wrong_question WHERE masteredFlag = 0 ORDER BY addedAt DESC")
    fun observeUnmastered(): Flow<List<WrongQuestion>>

    @Query("SELECT COUNT(*) FROM wrong_question WHERE masteredFlag = 0")
    fun observeUnmasteredCount(): Flow<Int>

    @Query(
        """
        SELECT wq.id AS wrongId, wq.questionId AS questionId, wq.abilityTag AS abilityTag,
               wq.addedAt AS addedAt, q.qType AS qType, q.stem AS stem,
               q.exerciseId AS exerciseId, e.type AS apiType
        FROM wrong_question wq
        JOIN question q ON wq.questionId = q.id
        JOIN exercise e ON q.exerciseId = e.id
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
}
