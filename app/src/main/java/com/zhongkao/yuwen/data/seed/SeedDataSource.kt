package com.zhongkao.yuwen.data.seed

import android.content.Context

/**
 * 从 assets/seed 读取随包内置的种子语料与命题配置（UTF-8）。
 */
class SeedDataSource(private val context: Context) {

    fun readKewenJson(): String = readAsset(FILE_KEWEN)
    fun readKewaiJson(): String = readAsset(FILE_KEWAI)
    fun readExamConfigJson(): String = readAsset(FILE_EXAM_CONFIG)

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val FILE_KEWEN = "seed/kewen_seed.json"
        const val FILE_KEWAI = "seed/kewai_seed.json"
        const val FILE_EXAM_CONFIG = "seed/exam_config.json"
    }
}
