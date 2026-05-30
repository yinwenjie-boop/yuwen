package com.zhongkao.yuwen.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhongkao.yuwen.YuwenApp
import com.zhongkao.yuwen.ui.nav.AppNavHost
import com.zhongkao.yuwen.ui.theme.ZhongkaoYuwenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = YuwenApp.from(application)
        setContent {
            ZhongkaoYuwenTheme {
                AppNavHost(keyStore = container.secureKeyStore)
            }
        }
    }
}
