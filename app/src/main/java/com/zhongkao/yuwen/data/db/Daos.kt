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

    @Query("SELECT COUNT(*) FROM text_bank")
    suspend fun count(): Int
}

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

    @Query("SELECT * FROM attempt WHERE questionId = :questionId ORDER BY gradedAt DESC")
    suspend fun byQuestion(questionId: Long): List<Attempt>
}

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
}

@Dao
interface WeakPointStatDao {
    @Upsert
    suspend fun upsert(stat: WeakPointStat)

    @Query("SELECT * FROM weak_point_stat ORDER BY wrongCount DESC LIMIT :limit")
    fun observeTop(limit: Int): Flow<List<WeakPointStat>>

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
