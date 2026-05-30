package com.zhongkao.yuwen.data.seed

import android.content.Context
import com.zhongkao.yuwen.domain.ExamConfig
import java.io.File

/**
 * exam_config 校准入口（阶段 5）。
 *
 * 默认用随包内置 assets/seed/exam_config.json（数值为真题反推 + 公开资料的「估计值」）。
 * 拿到常州市2026《语文学科命题说明》后，按其 calibration_instruction 改好同结构 JSON，
 * 经 [saveOverride] 导入为「校准覆盖文件」存进 filesDir，优先于内置文件生效（下次启动读取）。
 *
 * 保存前先用 [ExamConfig.parse] 校验可解析，解析失败抛异常、绝不写入坏配置。
 */
class CalibrationStore(
    private val context: Context,
    private val seedDataSource: SeedDataSource
) {
    private val overrideFile: File
        get() = File(context.filesDir, OVERRIDE_PATH)

    fun hasOverride(): Boolean = overrideFile.exists()

    /** 当前生效的 exam_config JSON：有校准覆盖用覆盖，否则用内置估计值。 */
    fun readActiveExamConfigJson(): String =
        if (hasOverride()) overrideFile.readText(Charsets.UTF_8)
        else seedDataSource.readExamConfigJson()

    /** 校验可解析后保存为覆盖文件；解析失败由 ExamConfig.parse 抛出，调用方需捕获并提示。 */
    fun saveOverride(json: String) {
        ExamConfig.parse(json) // 解析即校验；失败抛出，不落坏文件
        overrideFile.parentFile?.mkdirs()
        overrideFile.writeText(json, Charsets.UTF_8)
    }

    /** 恢复为内置估计值。 */
    fun clearOverride() {
        if (overrideFile.exists()) overrideFile.delete()
    }

    private companion object {
        const val OVERRIDE_PATH = "calibration/exam_config.json"
    }
}
