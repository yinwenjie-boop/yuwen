package com.zhongkao.yuwen

import android.app.Application

/** Application 入口，持有全局依赖容器。 */
class YuwenApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        fun from(app: Application): AppContainer = (app as YuwenApp).container
    }
}
