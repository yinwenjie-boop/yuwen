package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.data.ai.BadgeAward
import com.zhongkao.yuwen.data.db.Badge
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.data.db.WrongQuestion
import com.zhongkao.yuwen.data.repository.ProgressRepository
import com.zhongkao.yuwen.data.repository.WrongBookRepository
import com.zhongkao.yuwen.domain.incentive.BadgeRules
import com.zhongkao.yuwen.domain.incentive.IncentiveConfig
import com.zhongkao.yuwen.domain.incentive.ProgressCalculator
import com.zhongkao.yuwen.domain.incentive.WeakPointAccumulator
import java.time.LocalDate

/**
 * 阶段 4 结算落库（每次练习批改完成后调用一次）：
 *  1. 未满分题写 [WrongQuestion]（带 ability_tag）；
 *  2. 按考点滚动更新 WeakPointStat（attempts / wrongCount / avgTimeSec）；
 *  3. 本地计算 XP / 积分 / 连续打卡，更新 UserProgress，并按四象限+里程碑发徽章。
 *
 * 纯计算在 domain.incentive 各 object 内（已单测），此处只做编排与 IO。
 */
class RecordResultsUseCase(
    private val wrongBook: WrongBookRepository,
    private val progress: ProgressRepository,
    private val configProvider: () -> IncentiveConfig,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val today: () -> LocalDate = { LocalDate.now() }
) {
    /** 单题结算输入。 */
    data class QuestionOutcome(
        val questionRoomId: Long,
        val abilityTag: String,
        val gotScore: Int,
        val maxScore: Int,
        val timeSec: Int
    )

    /** 本次激励产出（供结果页结算动画展示）。 */
    data class Reward(
        val xpGained: Int,
        val coinsGained: Int,
        val newBadges: List<BadgeAward>,
        val leveledUp: Boolean,
        val progress: UserProgress
    )

    suspend fun record(
        questions: List<QuestionOutcome>,
        totalGot: Int,
        quadrantCode: String?,
        targetMet: Boolean,
        exerciseId: Long
    ): Reward {
        val ts = now()

        // 1. 错题入本（未满分）。
        questions.filter { it.gotScore < it.maxScore }.forEach { q ->
            wrongBook.addWrong(
                WrongQuestion(questionId = q.questionRoomId, abilityTag = q.abilityTag, addedAt = ts)
            )
        }

        // 2. 薄弱点统计：按考点聚合后滚动并入。
        questions.filter { it.abilityTag.isNotBlank() }
            .groupBy { it.abilityTag }
            .forEach { (tag, list) ->
                val merged = WeakPointAccumulator.merge(
                    existing = wrongBook.findWeakPoint(tag),
                    tag = tag,
                    roundAttempts = list.size,
                    roundWrong = list.count { it.gotScore < it.maxScore },
                    roundTimeSumSec = list.sumOf { it.timeSec },
                    now = ts
                )
                wrongBook.upsertWeakPoint(merged)
            }

        // 3. 激励：进度 + 徽章（本地计算）。
        val config = configProvider()
        val current = progress.getProgress() ?: UserProgress()
        val outcome = ProgressCalculator.apply(current, today(), totalGot, targetMet, config)
        progress.saveProgress(outcome.progress)

        val badges = buildList {
            BadgeRules.quadrantBadge(quadrantCode)?.let { add(it) }
            addAll(BadgeRules.milestones(outcome))
        }
        badges.forEach { b ->
            progress.awardBadge(Badge(code = b.code, name = b.name, exerciseId = exerciseId, earnedAt = ts))
        }

        return Reward(
            xpGained = outcome.xpGained,
            coinsGained = outcome.coinsGained,
            newBadges = badges,
            leveledUp = outcome.leveledUp,
            progress = outcome.progress
        )
    }
}
