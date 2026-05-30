package com.zhongkao.yuwen

import android.content.Context
import com.zhongkao.yuwen.core.network.NetworkModule
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.db.AppDatabase
import com.zhongkao.yuwen.data.repository.ExerciseRepository
import com.zhongkao.yuwen.data.repository.ProgressRepository
import com.zhongkao.yuwen.data.repository.TextBankRepository
import com.zhongkao.yuwen.data.repository.WrongBookRepository

/**
 * 轻量手写依赖容器（阶段 0 不引入 Hilt，保持单 module 简单）。
 * 由 [YuwenApp] 持有，UI 通过 LocalContext.applicationContext 取用。
 */
class AppContainer(context: Context) {

    val secureKeyStore: SecureKeyStore by lazy { SecureKeyStore(context) }

    private val database: AppDatabase by lazy { AppDatabase.get(context) }

    val textBankRepository: TextBankRepository by lazy {
        TextBankRepository(database.textBankDao())
    }
    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao(), database.questionDao(), database.attemptDao())
    }
    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(database.userProgressDao(), database.badgeDao())
    }
    val wrongBookRepository: WrongBookRepository by lazy {
        WrongBookRepository(database.wrongQuestionDao(), database.weakPointStatDao())
    }

    /** DeepSeek 客户端：Key 在每次请求时从 SecureKeyStore 现取，改 Key 即时生效。 */
    val deepSeekApi: DeepSeekApi by lazy {
        NetworkModule.createDeepSeekApi(secureKeyStore)
    }
}
