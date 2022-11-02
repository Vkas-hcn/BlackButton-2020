package com.demo.blackbutton.app

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.blankj.utilcode.util.ProcessUtils
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.ui.MainActivity
import com.demo.blackbutton.utils.ResUtils
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.Core
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil

class App : Application(), androidx.work.Configuration.Provider by Core {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            ResUtils.init(this)
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }

}
