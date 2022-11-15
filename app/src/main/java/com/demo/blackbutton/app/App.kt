package com.demo.blackbutton.app

import android.annotation.SuppressLint
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
import android.os.Environment
import android.util.Log
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.firebase.FirebaseApp
import java.io.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*


class App : Application(), androidx.work.Configuration.Provider by Core,
    Application.ActivityLifecycleCallbacks, LifecycleObserver {

    companion object {
        // app进入后台（true进入；false未进入）
        var isBackData = false
        // 是否进入后台（三秒后）
        var whetherBackground = false
        val mmkv by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
        }
        //当日日期
        var adDate = ""
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
    }
    private var job: Job? = null
    private val LOG_TAG = "ad-log"
    private var ad_activity: Activity? = null
    private var top_activity: Activity? = null

    private var flag = 0
    override fun onCreate() {
        super.onCreate()
        initCrash()
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        MMKV.initialize(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
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



    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        job?.cancel()
        job = null
        KLog.v("Lifecycle", "onMoveToForeground=$whetherBackground")
        //从后台切过来，跳转启动页
        if (whetherBackground&& !isBackData) {
            jumpPage()
        }
    }

    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onSTOPJumpPage() {
        KLog.v("Lifecycle", "onSTOPJumpPage=$whetherBackground")
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
//        ad_activity?.finish()
        whetherBackground = false
        val intent = Intent(top_activity, StartupActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(Constant.RETURN_CURRENT_PAGE, true)
        top_activity?.startActivity(intent)

    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is AdActivity) {
            top_activity = activity
        }else{
            ad_activity = activity
        }

        KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
    }

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
        if (activity !is AdActivity) {
            top_activity = activity
        }else{
            ad_activity = activity
        }
        flag++
        isBackData = false
    }

    override fun onActivityResumed(p0: Activity) {
        KLog.v("Lifecycle", "onActivityResumed=" + p0.javaClass.name)
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
        KLog.v("Lifecycle", "onActivityPaused=" + p0.javaClass.name)
    }

    override fun onActivityStopped(p0: Activity) {
        flag--
        if (flag == 0) {
            isBackData = true
        }
        KLog.v("Lifecycle", "onActivityStopped=" + p0.javaClass.name)
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        KLog.v("Lifecycle", "onActivitySaveInstanceState=" + p0.javaClass.name)

    }

    override fun onActivityDestroyed(p0: Activity) {
        KLog.v("Lifecycle", "onActivityDestroyed" + p0.javaClass.name)
        ad_activity = null
        top_activity = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }
    /**
     * app 崩溃重启的配置
     */
    @SuppressLint("RestrictedApi")
    private fun initCrash() {
        CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //背景模式,开启沉浸式
            .enabled(true) //是否启动全局异常捕获
            .showErrorDetails(true) //是否显示错误详细信息
            .showRestartButton(true) //是否显示重启按钮
            .trackActivities(true) //是否跟踪Activity
            .minTimeBetweenCrashesMs(1000) //崩溃的间隔时间(毫秒)
            .restartActivity(StartupActivity::class.java) //重新启动后的activity
//                                                .errorActivity(YourCustomErrorActivity.class) //崩溃后的错误activity
//                                                .eventListener(new YourCustomEventListener()) //崩溃后的错误监听
            .apply()
        CustomActivityOnCrash.install(this)
    }
    fun errorInfo(){
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable -> //获取崩溃时的UNIX时间戳
            val timeMillis = System.currentTimeMillis()
            //将时间戳格式化，建立一个String拼接器
            val stringBuilder: StringBuilder = StringBuilder(
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(
                    Date(timeMillis)
                )
            )
            stringBuilder.append(":\n")
            //获取错误信息
            stringBuilder.append(throwable.message)
            stringBuilder.append("\n")
            //获取堆栈信息
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            stringBuilder.append(sw.toString())

            //这就是完整的错误信息了，你可以拿来上传服务器，或者做成本地文件保存等等等等
            val errorLog = stringBuilder.toString()
            Log.e("测试", errorLog)
            //把获取到的日志写到本地，ErrorText.txt文件名字可以自己定义
            val rootFile: File = getRootFile(this@App)!!
            Log.e("测试", rootFile.toString())
            val file = File(rootFile, "ErrorText.txt")
            var bufferedWriter: BufferedWriter? = null
            try {
                //写入数据
                bufferedWriter = BufferedWriter(FileWriter(file, true))
                bufferedWriter.write(
                    """
                        $errorLog
                        
                        """.trimIndent()
                )
                bufferedWriter.flush()
            } catch (e: FileNotFoundException) {
            } catch (e: IOException) {
            } finally {
                try {
                    bufferedWriter?.close()
                } catch (e: IOException) {
                }
            }
            //最后如何处理这个崩溃，这里使用默认的处理方式让APP停止运行
            defaultHandler.uncaughtException(thread, throwable)
        }
    }

    /**
     * 安卓存储目录
     */
    fun getRootFile(application: Application): File? {
        //判断根目录
        var rootFile: File? = null
        rootFile = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            //Android Q 系统私有空间创建
            ///storage/emulated/0/Android/data/<package name>/files/ -- 应用卸载会删除该目录下所有文件
            application.getExternalFilesDir("")
        } else {
            //应用专属目录,位置/data/data//files
            application.filesDir
        }
        return rootFile
    }

}
