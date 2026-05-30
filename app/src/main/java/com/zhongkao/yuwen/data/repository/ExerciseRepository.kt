package com.zhongkao.yuwen.data.repository

import com.zhongkao.yuwen.data.db.Attempt
import com.zhongkao.yuwen.data.db.AttemptDao
import com.zhongkao.yuwen.data.db.Exercise
import com.zhongkao.yuwen.data.db.ExerciseDao
import com.zhongkao.yuwen.data.db.Question
import com.zhongkao.yuwen.data.db.QuestionDao
import kotlinx.coroutines.flow.Flow

/**
 * 练习仓库（骨架）：练习 + 题目 + 作答。
 * 出题/批改的写入要先经 AiResponseValidator 校验（阶段 2/3 接入）。
 */
class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao
) {
    fun observeAll(): Flow<List<Exercise>> = exerciseDao.observeAll()

    suspend fun createExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)
    suspend fun saveQuestions(questions: List<Question>): List<Long> = questionDao.insertAll(questions)
    suspend fun questionsOf(exerciseId: Long): List<Question> = questionDao.byExercise(exerciseId)

    suspend fun saveAttempts(attempts: List<Attempt>): List<Long> = attemptDao.insertAll(attempts)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
    suspend fun findExercise(id: Long): Exercise? = exerciseDao.findById(id)
}
