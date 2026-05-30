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
        你是常州市中考语文命题助手。题型、问法与采分点一律对标江苏（常州）中考课内外对比阅读真题，贴近中考实战。严格遵守：
        1. 课内外对比阅读，总分以用户给定 total_score 为准（默认15）。
        2. 甲文(passage_in)、乙文(passage_out)都用用户提供的原文，禁止改动一字、禁止虚构替换；原样回填 title/author/dynasty/text/source_ref，并保留 verified 与 need_human_review。
        3. 题型从{实词解释,虚词辨析,断句,句子翻译,内容理解,人物形象,比较鉴赏}组合：实词重点考一词多义并迁移课内学过的义项；虚词覆盖常见十虚词(之/于/者/为/而/以/乃/其/夫/然)在甲乙两文的异同；断句按语意停顿；翻译按"信达雅"与"留删换补调变"对关键词逐点给分。初三中考难度、不超纲不偏怪。
        4. 每题给 q_type, stem, ref_answer, score_points(逐点含 point 与 score), max_score, ability_tag；翻译题按关键词逐点给分。
        5. 只输出如下结构 JSON（snake_case）：
        {"type":"wenyan_compare","total_score":15,
         "passage_in":{"title":"","author":"","dynasty":"","text":"","notes":[],"source_ref":"","verified":true,"need_human_review":false},
         "passage_out":{"title":"","author":"","dynasty":"","text":"","notes":[],"source_ref":"","verified":true,"need_human_review":false},
         "questions":[{"id":"q1","q_type":"","stem":"","ref_answer":"","score_points":[{"point":"","score":1}],"max_score":4,"ability_tag":""}]}
    """.trimIndent()

    /** A 2.1 user：App 用本地真实原文填充甲/乙。难度按 [DifficultyProfile] 展开为硬性细则。 */
    fun wenyanCompareUser(jia: TextBank, yi: TextBank, req: GenerationRequest): String {
        val focus = req.focus?.takeIf { it.isNotBlank() } ?: "虚词之、句子翻译"
        val p = DifficultyProfile.of(req.difficulty)
        return buildString {
            appendLine("【甲·课内】${jia.title}（${jia.author}·${jia.dynasty}）：${jia.text}")
            appendLine("【乙·课外】${yi.title}（${yi.author}·${yi.dynasty}，出处：${yi.sourceRef}）：${yi.text}")
            appendLine("要求：total_score=${req.totalScore}，题量约${req.questionCount}题，难度=${req.difficulty}，侧重考点=${focus}。")
            append("【难度要求·${req.difficulty}】${p.wenyanRubric} ${p.scoreShape}")
        }
    }

    /**
     * A 2.2 出题 · 文言（AI 提议课外篇·兜底）：本地课外库为空时追加到对比 system 之后。
     * 严守防伪：只能引真实传世文献，不能确认出处时输出空 text 并打 need_human_review。
     */
    val WENYAN_PROPOSE_APPENDIX = """
        若需你补充乙文：必须是真实存在的传世文献片段，注明 source_ref(书名+朝代+作者)；禁止原创/改写/仿写文言文；不能确认真实出处时，输出空 text 并将 need_human_review 置 true、verified 置 false，在 source_ref 写明原因。
    """.trimIndent()

    /** A 2.2 user：仅给课内甲文，请 AI 据实补充乙文（结果须经人工确认才入库）。 */
    fun wenyanProposeUser(jia: TextBank, req: GenerationRequest): String {
        val focus = req.focus?.takeIf { it.isNotBlank() } ?: "虚词之、句子翻译"
        val p = DifficultyProfile.of(req.difficulty)
        return buildString {
            appendLine("【甲·课内】${jia.title}（${jia.author}·${jia.dynasty}）：${jia.text}")
            appendLine("【乙·课外】本地课外库暂无合适篇目，请你按上述规则补充一篇真实存在的课外文言片段作为乙文，并据实填写 source_ref/verified/need_human_review。")
            appendLine("要求：total_score=${req.totalScore}，题量约${req.questionCount}题，难度=${req.difficulty}，侧重考点=${focus}。")
            append("【难度要求·${req.difficulty}】${p.wenyanRubric} ${p.scoreShape}")
        }
    }

    /**
     * 现代文各文体的命题要点（对标江苏中考真题题型/问法）。
     * 文学类按"四要素/赏析"，实用论述类按"信息/方法/语言/论证"，非连续性文本按"多材料+图表"。
     */
    private fun xiandaiGenreSpec(genre: String): String = when (genre) {
        "小说" -> "按小说四要素命题：情节概括与作用、人物形象及塑造手法(正面/侧面描写)、" +
            "环境描写(自然/社会)及作用、主题探究(开放性)；可含标题含义或叙事视角题。"
        "散文" -> "考查：主旨与情感、词句含义与表达作用(表层+深层)、写作手法(借景抒情/托物言志/对比等)及作用、" +
            "行文线索与结构思路、标题含义。"
        "说明文" -> "考查：说明对象及其特征、说明方法及作用(举例子/列数字/作比较/打比方等)、说明顺序、" +
            "说明语言准确性(如某词能否删去及原因)、信息提取与概括。"
        "议论文" -> "考查：中心论点、论据类型及作用、论证方法及作用(举例/道理/对比/比喻论证)、" +
            "论证思路与结构、联系实际谈看法。"
        "非连续性文本" -> "由 2–4 则材料组成，正文用「材料一/材料二/…」分隔，含文字并可包含可文字化的图表/数据描述；" +
            "考查：信息筛选与概括、多则材料异同比较、图表数据解读、依据材料探究或提建议、联系生活实际。"
        else -> "考查：内容概括、词句含义与作用、人物形象、写作手法及作用、结构思路、情感态度。" // 记叙文
    }

    /** A 2.3 出题 · 现代文（type=xiandai）system，按文体/字数/题量/难度动态填充，题型对标江苏中考。 */
    fun xiandaiSystem(req: GenerationRequest): String {
        val genre = req.genre?.takeIf { it.isNotBlank() } ?: "记叙文"
        val words = req.targetWords
        val p = DifficultyProfile.of(req.difficulty)
        // 非连续性文本是一组材料而非单篇短文，开头措辞另作区分。
        val lead = if (genre == "非连续性文本") {
            "围绕一个贴近初中生的主题，编写一组原创非连续性文本材料（初三难度，总字数约${words}字）并配题。"
        } else {
            "生成一篇原创${genre}短文（初三难度，约${words}字）并配题。"
        }
        return """
            你是中考语文命题助手。题型、问法与采分点对标江苏（常州）中考现代文阅读真题。${lead}
            【文体要点·${genre}】${xiandaiGenreSpec(genre)}
            【难度要求·${req.difficulty}】${p.xiandaiRubric} ${p.scoreShape}
            每题给 q_type, stem, ref_answer, score_points, max_score, ability_tag。只输出 JSON：
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

    /** A 2.4 批改（两类通用）system。 */
    val GRADING_SYSTEM = """
        你是常州中考语文阅卷老师，按“踩点给分”严格批改。对每题输出：
        - got_score：依 score_points 命中给分，不超过 max_score；
        - point_check：逐采分点{point,hit,score}，命中分合计应=got_score；
        - error_type：错因，具体到知识点（如“误将‘之’判为代词”）；
        - correct_answer：规范正确答法；
        - explanation：为什么这样答、易错点；
        - tip：一条可立即执行的提升建议，指向具体复习点。
        最后给 total_got、total_full、weak_points、next_advice。只输出 JSON：
        {"per_question":[{"id":"","got_score":0,"max_score":0,"point_check":[{"point":"","hit":true,"score":0}],"error_type":"","correct_answer":"","explanation":"","tip":""}],
         "total_got":0,"total_full":0,"weak_points":[],"next_advice":""}
    """.trimIndent()

    /** A 2.4 user：填入出题 questions 原样 JSON 与学生作答。 */
    fun gradingUser(questionsJson: String, answersJson: String): String = buildString {
        appendLine("题目与参考答案：$questionsJson")
        appendLine("学生作答：$answersJson")
        append("请按上述规则批改。")
    }
}
