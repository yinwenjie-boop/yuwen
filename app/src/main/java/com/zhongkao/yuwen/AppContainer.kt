package com.zhongkao.yuwen

import android.content.Context
import com.zhongkao.yuwen.core.network.NetworkModule
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.core.settings.IncentiveSettingsStore
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.db.AppDatabase
import com.zhongkao.yuwen.data.repository.ExerciseRepository
import com.zhongkao.yuwen.data.repository.ProgressRepository
import com.zhongkao.yuwen.data.repository.TextBankRepository
import com.zhongkao.yuwen.data.repository.WrongBookRepository
import com.zhongkao.yuwen.data.seed.SeedDataSource
import com.zhongkao.yuwen.data.seed.SeedImporter
import com.zhongkao.yuwen.domain.ExamConfig
import com.zhongkao.yuwen.domain.usecase.GenerateExerciseUseCase
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import com.zhongkao.yuwen.domain.usecase.GradeExerciseUseCase
import com.zhongkao.yuwen.domain.usecase.RecordResultsUseCase

/**
 * 轻量手写依赖容器（阶段 0 不引入 Hilt，保持单 module 简单）。
 * 由 [YuwenApp] 持有，UI 通过 LocalContext.applicationContext 取用。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val secureKeyStore: SecureKeyStore by lazy { SecureKeyStore(appContext) }

    /** 激励数值（XP/积分/连胜系数），设置页可调、本地存。 */
    val incentiveSettingsStore: IncentiveSettingsStore by lazy { IncentiveSettingsStore(appContext) }

    private val database: AppDatabase by lazy { AppDatabase.get(appContext) }

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

    private val seedDataSource: SeedDataSource by lazy { SeedDataSource(appContext) }

    /** 命题/时间基准配置（随包内置，解析一次）。 */
    val examConfig: ExamConfig by lazy { ExamConfig.parse(seedDataSource.readExamConfigJson()) }

    /** DeepSeek 客户端：Key 在每次请求时从 SecureKeyStore 现取，改 Key 即时生效。 */
    val deepSeekApi: DeepSeekApi by lazy {
        NetworkModule.createDeepSeekApi(secureKeyStore)
    }

    val generateExerciseUseCase: GenerateExerciseUseCase by lazy {
        GenerateExerciseUseCase(textBankRepository, deepSeekApi, secureKeyStore)
    }

    val gradeExerciseUseCase: GradeExerciseUseCase by lazy {
        GradeExerciseUseCase(deepSeekApi, secureKeyStore)
    }

    /** 阶段 4：批改后写错题本 + 薄弱点 + 激励（数值现取，改设置即时生效）。 */
    val recordResultsUseCase: RecordResultsUseCase by lazy {
        RecordResultsUseCase(
            wrongBook = wrongBookRepository,
            progress = progressRepository,
            configProvider = { incentiveSettingsStore.getConfig() }
        )
    }

    /** 出题设置页 → 答题页之间传递本次出题请求（单用户本地 App，内存暂存即可）。 */
    var pendingRequest: GenerationRequest? = null

    /** 复习页"针对薄弱点再出一套" → 出题设置页预填的侧重考点。 */
    var pendingFocus: String? = null

    /**
     * 首次启动灌库：仅当语料表为空时，从 assets 读取课内/课外种子并按防伪规则导入。
     * 返回本次导入汇总；已灌过则返回 null。
     */
    suspend fun seedIfNeeded(): SeedSummary? {
        if (textBankRepository.count() > 0) return null

        val kewen = SeedImporter.import(seedDataSource.readKewenJson())
        val kewai = SeedImporter.import(seedDataSource.readKewaiJson())

        val all = kewen.importable + kewai.importable
        if (all.isNotEmpty()) textBankRepository.seed(all)

        return SeedSummary(
            kewenImported = kewen.kewenCount,
            kewaiImported = kewai.kewaiCount,
            skipped = kewen.skipped + kewai.skipped
        )
    }

    data class SeedSummary(
        val kewenImported: Int,
        val kewaiImported: Int,
        val skipped: List<SeedImporter.Skipped>
    )
}
