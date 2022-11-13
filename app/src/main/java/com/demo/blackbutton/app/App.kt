package com.demo.blackbutton.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ProcessUtils
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.ad.AdLoad
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.ui.MainActivity
import com.demo.blackbutton.ui.StartupActivity
import com.demo.blackbutton.utils.*
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.Core
import com.google.android.gms.ads.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.*
import android.app.ActivityManager
import android.content.Context


class App : Application(), androidx.work.Configuration.Provider by Core,
    Application.ActivityLifecycleCallbacks, LifecycleObserver {

    companion object {
        // app进入后台（true进入；false未进入）
        var isBackData = false
        // 是否进入后台（三秒后）
        var whetherBackground = false
    }
    private var job: Job? = null
    private val LOG_TAG = "ad-log"
    private var ad_activity: Activity? = null
    private var top_activity: Activity? = null

    //当日日期
    var adDate = ""
    val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
    }
    private var flag = 0
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
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
        NetworkPing.getTimerThread()
        isAppOpenSameDay()
    }

    /**
     * 判断是否是当天打开
     */
    fun isAppOpenSameDay() {
        adDate = mmkv.decodeString(Constant.CURRENT_DATE, "").toString()
        if (adDate == "") {
            MmkvUtils.set(Constant.CURRENT_DATE, CalendarUtils.formatDateNow())
            KLog.e("TAG", "CalendarUtils.formatDateNow()=${CalendarUtils.formatDateNow()}")
        } else {
            KLog.e("TAG", "当前时间=${CalendarUtils.formatDateNow()}")
            KLog.e("TAG", "存储时间=${adDate}")

            KLog.e(
                "TAG",
                "两个时间比较=${CalendarUtils.dateAfterDate(adDate, CalendarUtils.formatDateNow())}"
            )
            if (CalendarUtils.dateAfterDate(adDate, CalendarUtils.formatDateNow())) {
                MmkvUtils.set(Constant.CURRENT_DATE, CalendarUtils.formatDateNow())
                MmkvUtils.set(Constant.CLICKS_COUNT, 0)
                MmkvUtils.set(Constant.SHOW_COUNT, 0)
            }
        }
    }


    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        job?.cancel()
        job = null
        KLog.e("TAG", "onMoveToForeground=$whetherBackground")
        if (whetherBackground) {
            jumpPage()
        }
    }

    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onSTOPJumpPage() {
        KLog.e("TAG", "onSTOPJumpPage=$whetherBackground")
        job = GlobalScope.launch {
            whetherBackground = false
            delay(3000L)
            whetherBackground = true
            ad_activity?.finish()
            ActivityCollector.getActivity(StartupActivity::class.java)?.finish()
        }
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        whetherBackground = false
        val intent = Intent(applicationContext, StartupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(Constant.RETURN_CURRENT_PAGE, true)
        applicationContext.startActivity(intent)
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is AdActivity) {
            top_activity = activity
        }

        KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
    }

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        KLog.v("Lifecycle", "onActivityStarted" + ActivityCollector.getActivityName(activity))
        if (activity !is AdActivity) {
            top_activity = activity
        }
        flag++
        isBackData = false
    }

    override fun onActivityResumed(p0: Activity) {
        KLog.v("Lifecycle", "onActivityResumed=" + ActivityCollector.getActivityName(p0))
        if (p0 !is AdActivity) {
            top_activity = p0
        }
    }

    override fun onActivityPaused(p0: Activity) {

        if (p0 is AdActivity) {
            ad_activity = p0
        } else {
            top_activity = p0
        }
        KLog.v("Lifecycle", "onActivityPaused=" + ActivityCollector.getActivityName(p0))
    }

    override fun onActivityStopped(p0: Activity) {
        flag--
        if (flag == 0) {
            isBackData = true
        }
        KLog.v("Lifecycle", "onActivityStopped=" + ActivityCollector.getActivityName(p0))
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        KLog.v("Lifecycle", "onActivitySaveInstanceState=" + ActivityCollector.getActivityName(p0))

    }

    override fun onActivityDestroyed(p0: Activity) {
        KLog.v("Lifecycle", "onActivityDestroyed" + ActivityCollector.getActivityName(p0))
        ad_activity = null
        top_activity = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }

}
