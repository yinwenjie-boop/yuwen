package com.zhongkao.yuwen.domain.prompt

import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.domain.usecase.GenerationRequest

/**
 * 出题/批改提示词（严格对齐《全部提示词_按顺序.md》附录 A，与 AiDtos.kt 字段一致）。
 * 公共约束：只输出 JSON，不要 markdown/解释；key 一律 snake_case；
 * 各采分点分值合计=该题 max_score，各题 max_score 合计=total_score。
 */
object Prompts {

    /** A 2.1 出题 · 文言对比（库内选篇，type=wenyan_compare）system。 */
    val WENYAN_COMPARE_SYSTEM = """
        你是常州市中考语文命题助手。严格遵守：
        1. 课内外对比阅读，总分以用户给定 total_score 为准（默认15）。
        2. 甲文(passage_in)、乙文(passage_out)都用用户提供的原文，禁止改动一字、禁止虚构替换；原样回填 title/author/dynasty/text/source_ref，并保留 verified 与 need_human_review。
        3. 题型从{实词解释,虚词辨析,断句,句子翻译,内容理解,人物形象}组合，覆盖高频虚词(之/而/于/以)，初三中考难度、不超纲不偏怪。
        4. 每题给 q_type, stem, ref_answer, score_points(逐点含 point 与 score), max_score, ability_tag；翻译题按关键词逐点给分。
        5. 只输出如下结构 JSON（snake_case）：
        {"type":"wenyan_compare","total_score":15,
         "passage_in":{"title":"","author":"","dynasty":"","text":"","notes":[],"source_ref":"","verified":true,"need_human_review":false},
         "passage_out":{"title":"","author":"","dynasty":"","text":"","notes":[],"source_ref":"","verified":true,"need_human_review":false},
         "questions":[{"id":"q1","q_type":"","stem":"","ref_answer":"","score_points":[{"point":"","score":1}],"max_score":4,"ability_tag":""}]}
    """.trimIndent()

    /** A 2.1 user：App 用本地真实原文填充甲/乙。 */
    fun wenyanCompareUser(jia: TextBank, yi: TextBank, req: GenerationRequest): String {
        val focus = req.focus?.takeIf { it.isNotBlank() } ?: "虚词之、句子翻译"
        return buildString {
            appendLine("【甲·课内】${jia.title}（${jia.author}·${jia.dynasty}）：${jia.text}")
            appendLine("【乙·课外】${yi.title}（${yi.author}·${yi.dynasty}，出处：${yi.sourceRef}）：${yi.text}")
            append("要求：total_score=${req.totalScore}，题量约${req.questionCount}题，难度=${req.difficulty}，侧重考点=${focus}。")
        }
    }

    /** A 2.3 出题 · 现代文（type=xiandai）system，按文体/字数/题量动态填充。 */
    fun xiandaiSystem(req: GenerationRequest): String {
        val genre = req.genre?.takeIf { it.isNotBlank() } ?: "记叙文"
        val words = req.targetWords
        return """
            你是中考语文命题助手。生成一篇原创${genre}短文（初三难度，约${words}字）并配题。题型覆盖{主旨概括,词句含义与作用,人物形象,写作手法及作用,结构思路,情感态度}中若干。每题给 q_type, stem, ref_answer, score_points, max_score, ability_tag。只输出 JSON：
            {"type":"xiandai","total_score":${req.totalScore},
             "passage":{"title":"","author":"","dynasty":"","text":"","notes":[],"source_ref":"原创","verified":true,"need_human_review":false},
             "questions":[{"id":"q1","q_type":"","stem":"","ref_answer":"","score_points":[{"point":"","score":2}],"max_score":4,"ability_tag":""}]}
        """.trimIndent()
    }

    /** A 2.3 user。 */
    fun xiandaiUser(req: GenerationRequest): String {
        val genre = req.genre?.takeIf { it.isNotBlank() } ?: "记叙文"
        val theme = req.focus?.takeIf { it.isNotBlank() }?.let { "，主题倾向=$it" } ?: ""
        return "文体=${genre}，字数≈${req.targetWords}，题量=${req.questionCount}，难度=${req.difficulty}${theme}。"
    }
}
