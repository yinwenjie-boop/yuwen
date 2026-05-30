package com.zhongkao.yuwen.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseValidatorTest {

    private fun q(id: String, max: Int, points: List<ScorePoint> = listOf(ScorePoint("p", max))) =
        GenQuestion(
            id = id, qType = "实词解释", stem = "解释加点词", refAnswer = "答案",
            scorePoints = points, maxScore = max, abilityTag = "文言实词"
        )

    @Test
    fun `合法的现代文出题校验通过且可自动入库`() {
        val ex = GeneratedExercise(
            type = "xiandai",
            totalScore = 4,
            passage = Passage(title = "原创短文", text = "正文……"),
            questions = listOf(q("q1", 4))
        )
        val report = AiResponseValidator.validateGenerated(ex)
        assertTrue(report.errors.toString(), report.isUsable)
        assertTrue(report.canAutoInsert)
    }

    @Test
    fun `文言对比缺乙文出处应报硬错误`() {
        val ex = GeneratedExercise(
            type = "wenyan_compare",
            totalScore = 4,
            passageIn = Passage(title = "三峡", text = "自三峡七百里中……", verified = true),
            passageOut = Passage(title = "课外篇", text = "某文……", sourceRef = "", verified = true),
            questions = listOf(q("q1", 4))
        )
        val report = AiResponseValidator.validateGenerated(ex)
        assertFalse(report.isUsable)
        assertTrue(report.errors.any { it.contains("乙文缺少出处") })
    }

    @Test
    fun `乙文未确认真实性应转人工不可自动入库`() {
        val ex = GeneratedExercise(
            type = "wenyan_compare",
            totalScore = 4,
            passageIn = Passage(title = "三峡", text = "自三峡……", verified = true),
            passageOut = Passage(
                title = "课外篇", text = "某真实古文……",
                sourceRef = "《世说新语》南朝宋·刘义庆",
                verified = false, needHumanReview = true
            ),
            questions = listOf(q("q1", 4))
        )
        val report = AiResponseValidator.validateGenerated(ex)
        assertTrue(report.isUsable)        // 内容结构可用
        assertTrue(report.needsReview)     // 但需人工确认
        assertFalse(report.canAutoInsert)  // 不得自动入正式题库
    }

    @Test
    fun `采分点合计不等于满分给出警告但仍可用`() {
        val ex = GeneratedExercise(
            type = "xiandai",
            totalScore = 4,
            passage = Passage(title = "短文", text = "正文"),
            questions = listOf(q("q1", 4, points = listOf(ScorePoint("p1", 1), ScorePoint("p2", 1))))
        )
        val report = AiResponseValidator.validateGenerated(ex)
        assertTrue(report.isUsable)
        assertTrue(report.warnings.any { it.contains("采分点合计") })
    }

    @Test
    fun `批改得分越界报硬错误`() {
        val g = GradingResult(
            perQuestion = listOf(
                QuestionGrade(id = "q1", gotScore = 6, maxScore = 4, explanation = "x", tip = "y")
            ),
            totalGot = 6, totalFull = 4
        )
        val report = AiResponseValidator.validateGrading(g, expectedIds = setOf("q1"))
        assertFalse(report.isUsable)
        assertTrue(report.errors.any { it.contains("got_score") })
    }

    @Test
    fun `批改题目id与出题不一致报错`() {
        val g = GradingResult(
            perQuestion = listOf(
                QuestionGrade(id = "q1", gotScore = 2, maxScore = 4, explanation = "x", tip = "y")
            ),
            totalGot = 2, totalFull = 4
        )
        val report = AiResponseValidator.validateGrading(g, expectedIds = setOf("q1", "q2"))
        assertFalse(report.isUsable)
        assertTrue(report.errors.any { it.contains("不一致") })
    }
}
