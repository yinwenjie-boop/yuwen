package com.zhongkao.yuwen

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application 入口，持有全局依赖容器，并在首启动灌入种子语料。 */
class YuwenApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        appScope.launch {
            container.progressRepository.ensureInitialized()
            val summary = container.seedIfNeeded()
            if (summary != null) {
                // 不打印任何语料正文/Key，只记数量与被拦截原因，便于排查防伪过滤。
                Log.i(TAG, "首启动灌库完成：课内 ${summary.kewenImported} 篇，课外 ${summary.kewaiImported} 篇，跳过 ${summary.skipped.size} 条")
                summary.skipped.forEach { Log.w(TAG, "跳过 ${it.id}（${it.title}）：${it.reason}") }
            }
        }
    }

    companion object {
        private const val TAG = "YuwenApp"
        fun from(app: Application): AppContainer = (app as YuwenApp).container
    }
}
