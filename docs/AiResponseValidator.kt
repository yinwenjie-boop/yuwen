package com.zhongkao.yuwen.data.ai

/**
 * AI 返回校验（对应 SPEC v2 §6 的「字段缺失/verified=false/need_human_review 拦截」要求）。
 *
 * 三级结果：
 *  - 硬错误(errors)：解析虽成功但内容不可用 → 拦截，提示重试，绝不入库。
 *  - 需人工确认(needsReview)：文言课外篇 verified=false / need_human_review=true
 *    → 不可自动入正式题库，转「待确认」。
 *  - 警告(warnings)：可用但提示风险（如采分点合计≠满分）。
 */
data class ValidationReport(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val needsReview: Boolean = false
) {
    val isUsable: Boolean get() = errors.isEmpty()
    val canAutoInsert: Boolean get() = isUsable && !needsReview
}

object AiResponseValidator {

    private val GEN_TYPES = setOf("wenyan_compare", "wenyan_single", "xiandai")

    fun validateGenerated(ex: GeneratedExercise): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var needsReview = false

        if (ex.type !in GEN_TYPES) errors += "未知出题类型: ${ex.type}"
        if (ex.totalScore <= 0) errors += "total_score 非法: ${ex.totalScore}"
        if (ex.questions.isEmpty()) errors += "questions 为空"

        // 按类型校验篇目结构
        when (ex.type) {
            "wenyan_compare" -> {
                val pin = ex.passageIn
                val pout = ex.passageOut
                if (pin == null || pin.text.isBlank()) errors += "对比阅读缺少甲文(passage_in)"
                if (pout == null || pout.text.isBlank()) errors += "对比阅读缺少乙文(passage_out)"
                // 文言防伪：课外乙文必须有出处，且未确认的需转人工
                pout?.let {
                    if (it.sourceRef.isBlank()) errors += "乙文缺少出处(source_ref)"
                    if (!it.verified || it.needHumanReview) {
                        needsReview = true
                        warnings += "乙文未确认真实性，需人工确认后方可入库"
                    }
                }
            }
            "wenyan_single", "xiandai" -> {
                if (ex.passage == null || ex.passage.text.isBlank()) errors += "缺少正文(passage)"
                if (ex.type == "wenyan_single") {
                    ex.passage?.let {
                        if (it.sourceRef.isBlank()) warnings += "文言单篇建议补出处"
                        if (!it.verified || it.needHumanReview) needsReview = true
                    }
                }
            }
        }

        // 逐题校验
        val ids = mutableSetOf<String>()
        var sumMax = 0
        ex.questions.forEachIndexed { i, q ->
            val tag = "题${i + 1}(${q.id})"
            if (q.id.isBlank()) errors += "$tag id 为空"
            if (!ids.add(q.id)) errors += "$tag id 重复"
            if (q.stem.isBlank()) errors += "$tag 题干为空"
            if (q.refAnswer.isBlank()) errors += "$tag 缺少参考答案"
            if (q.maxScore <= 0) errors += "$tag max_score 非法"
            if (q.abilityTag.isBlank()) warnings += "$tag 缺少能力标签，将影响薄弱点统计"
            if (q.scorePoints.isEmpty()) {
                warnings += "$tag 缺少采分点，批改将退化为整体给分"
            } else {
                val sp = q.scorePoints.sumOf { it.score }
                if (sp != q.maxScore) warnings += "$tag 采分点合计($sp)≠满分(${q.maxScore})"
            }
            sumMax += q.maxScore
        }
        if (ex.questions.isNotEmpty() && sumMax != ex.totalScore) {
            warnings += "各题满分合计($sumMax)≠total_score(${ex.totalScore})"
        }

        return ValidationReport(errors, warnings, needsReview)
    }

    fun validateGrading(g: GradingResult, expectedIds: Set<String>? = null): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (g.perQuestion.isEmpty()) errors += "per_question 为空"

        var sumGot = 0
        var sumFull = 0
        g.perQuestion.forEach { q ->
            val tag = "批改(${q.id})"
            if (q.maxScore <= 0) errors += "$tag max_score 非法"
            if (q.gotScore < 0 || q.gotScore > q.maxScore)
                errors += "$tag got_score(${q.gotScore}) 超出 0..${q.maxScore}"
            if (q.explanation.isBlank()) warnings += "$tag 缺少解析"
            if (q.tip.isBlank()) warnings += "$tag 缺少提升建议"
            val hitSum = q.pointCheck.filter { it.hit }.sumOf { it.score }
            if (q.pointCheck.isNotEmpty() && hitSum != q.gotScore)
                warnings += "$tag 命中采分点合计($hitSum)≠得分(${q.gotScore})"
            sumGot += q.gotScore
            sumFull += q.maxScore
        }
        if (sumGot != g.totalGot) warnings += "total_got(${g.totalGot})≠逐题合计($sumGot)"
        if (sumFull != g.totalFull) warnings += "total_full(${g.totalFull})≠逐题合计($sumFull)"

        expectedIds?.let { exp ->
            val got = g.perQuestion.map { it.id }.toSet()
            if (got != exp) errors += "批改题目id与出题不一致: 缺${exp - got}, 多${got - exp}"
        }

        return ValidationReport(errors, warnings)
    }
}
