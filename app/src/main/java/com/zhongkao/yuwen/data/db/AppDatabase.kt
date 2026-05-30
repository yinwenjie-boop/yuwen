package com.zhongkao.yuwen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TextBank::class,
        Exercise::class,
        Question::class,
        Attempt::class,
        WrongQuestion::class,
        WeakPointStat::class,
        UserProgress::class,
        Badge::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun textBankDao(): TextBankDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun wrongQuestionDao(): WrongQuestionDao
    abstract fun weakPointStatDao(): WeakPointStatDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        private const val DB_NAME = "yuwen.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
    }
}
