package com.zhongkao.yuwen.domain.incentive

import com.zhongkao.yuwen.data.db.WeakPointStat

/**
 * 薄弱点累计（SPEC §7.3）：把本轮某考点的作答次数 / 错题数 / 用时并入历史统计，
 * 用加权方式滚动平均用时。纯函数。
 */
object WeakPointAccumulator {

    fun merge(
        existing: WeakPointStat?,
        tag: String,
        roundAttempts: Int,
        roundWrong: Int,
        roundTimeSumSec: Int,
        now: Long
    ): WeakPointStat {
        val prevAttempts = existing?.attempts ?: 0
        val prevWrong = existing?.wrongCount ?: 0
        val prevAvg = existing?.avgTimeSec ?: 0

        val newAttempts = prevAttempts + roundAttempts
        val newWrong = prevWrong + roundWrong
        val newAvg = if (newAttempts > 0) {
            (prevAvg * prevAttempts + roundTimeSumSec) / newAttempts
        } else 0

        return WeakPointStat(
            abilityTag = tag,
            attempts = newAttempts,
            wrongCount = newWrong,
            avgTimeSec = newAvg,
            lastUpdated = now
        )
    }
}
