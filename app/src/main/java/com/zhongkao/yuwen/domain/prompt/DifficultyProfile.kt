package com.zhongkao.yuwen.domain.prompt

/**
 * 结构化难度参数（方案 C + B）。
 *
 * 把"难度"从一个孤词扩成可控的出题约束：题型组合 / 考点密度 / 难点强制 /
 * 分值分布 / 现代文字数与文本复杂度。顺序即难度递增：
 *   基础 < 中等 < 偏难 < 拔尖(压轴)。
 *
 * 注意：难度只影响"设问/考点/分布"，不影响文言原文——课内甲文、库内乙文一律由
 * [com.zhongkao.yuwen.domain.usecase.GenerateExerciseUseCase] 用本地真实原文覆盖，
 * 防伪硬约束不变。拔尖档对标中考压轴，但仍要求"不超纲、不偏怪、不生造"。
 */
data class DifficultyProfile(
    /** 与 [com.zhongkao.yuwen.domain.usecase.GenerationRequest.difficulty] 字符串一致，作查表键。 */
    val key: String,
    /** 文言对比的难度细则（注入 user 消息）。 */
    val wenyanRubric: String,
    /** 现代文的难度细则（注入 system 消息）。 */
    val xiandaiRubric: String,
    /** 分值分布导向：越难，越向高分综合/探究题倾斜。 */
    val scoreShape: String,
    /** 现代文目标基准字数（随难度增长；文体再做微调）。 */
    val xiandaiWords: Int
) {
    companion object {
        val BASIC = DifficultyProfile(
            key = "基础",
            wenyanRubric = "以课内高频考点为主：常见实词解释、单一高频虚词辨析、按标志词断句、" +
                "可直译的短句翻译；设问直接、答案多可在文中直接定位，不设甲乙比较与开放探究题。",
            xiandaiRubric = "考点直白：信息提取、词句表层含义、主旨概括；设问单层，答案多有原文依据，不绕弯。",
            scoreShape = "各题分值均匀，单题不超过 4 分。",
            xiandaiWords = 700
        )

        val MEDIUM = DifficultyProfile(
            key = "中等",
            wenyanRubric = "题型覆盖实词（含个别一词多义）、高频虚词(之/而/于/以)辨析、断句、含重点词的句子翻译、" +
                "内容理解；至少一道甲乙比较题，含一处需结合语境推断的设问。",
            xiandaiRubric = "含义题需「表层+深层」两层；考查词句作用、人物形象、写作手法及作用、结构思路其一二；" +
                "含一道需要跨句推断的设问。",
            scoreShape = "翻译/比较/手法等综合题约占总分一半。",
            xiandaiWords = 800
        )

        val HARD = DifficultyProfile(
            key = "偏难",
            wenyanRubric = "实词必含一词多义/古今异义/通假其一；虚词辨析覆盖多个高频虚词在不同语境的异同；" +
                "断句不提供明显标志词；翻译句必含词类活用或特殊句式(判断/被动/倒装/省略)；" +
                "内容理解为甲乙比较异同并分析人物或手法；含一道开放探究题。",
            xiandaiRubric = "多层推断与跨段整合；含义题问「含义+作用+表达效果」；鉴赏修辞或描写手法及其作用；" +
                "梳理结构思路；含一道联系全文的探究/评价题。",
            scoreShape = "翻译/比较/手法/探究等高分综合题占总分六成以上，单题最高可达 5–6 分。",
            xiandaiWords = 900
        )

        val TOP = DifficultyProfile(
            key = "拔尖",
            wenyanRubric = "对标中考压轴：实词考一词多义且要求结合语境辨析；同一虚词在甲乙两文同字异义对比；" +
                "断句为长句、无标志、易错停顿；翻译句含词类活用与特殊句式叠加；内容理解要求概括人物精神、" +
                "比较甲乙主旨异同并知人论世；必含开放探究题，要求结合写作意图或现实作答。" +
                "难度对标压轴题，但不超纲、不偏怪、不生造、不脱离课内迁移。",
            xiandaiRubric = "文本更长更含蓄，含多线索或欲扬先抑等复杂结构；设问环环相扣，需全文整合与言外之意推断；" +
                "手法鉴赏需「判断+分析+评价效果」三层；含一道高阶探究题（比较阅读或联系实际），采分点细。",
            scoreShape = "综合与探究题占总分约七成，设置 1–2 道 6 分以上压轴题。",
            xiandaiWords = 1000
        )

        /** 由低到高，顺序即设置页下拉与难度递增顺序。 */
        val ALL = listOf(BASIC, MEDIUM, HARD, TOP)

        /** 设置页"难度"下拉选项。 */
        val LABELS: List<String> = ALL.map { it.key }

        /** 容错查表：未知值回退到[MEDIUM]，绝不抛异常。 */
        fun of(difficulty: String): DifficultyProfile =
            ALL.firstOrNull { it.key == difficulty } ?: MEDIUM
    }
}
