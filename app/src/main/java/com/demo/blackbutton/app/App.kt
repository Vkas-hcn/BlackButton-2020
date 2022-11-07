package com.demo.blackbutton.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ProcessUtils
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.ui.MainActivity
import com.demo.blackbutton.ui.StartupActivity
import com.demo.blackbutton.utils.*
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.Core
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.*
import java.util.*

class App : Application(), androidx.work.Configuration.Provider by Core,
    Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null
    var AD_UNIT_ID = ""
    private val LOG_TAG = "BlackButton"

    // 是否进入后台
    private var whetherBackground = false
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//        ProcessLifecycleOwner.get().lifecycle.addObserver(ApplicationObserver())

        appOpenAdManager = AppOpenAdManager()
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
    }

    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        KLog.e("TAG", "ON_START")
        if (whetherBackground) {
            jumpPage()
            whetherBackground = false
        } else {
            // Show the ad (if available) when the app moves to foreground.
            currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
        }
    }

    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onSTOPJumpPage() {
        GlobalScope.launch(Dispatchers.Main) {
            delay(3000L)
            whetherBackground = true
        }
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        isRunningForegroundToApp1(this, StartupActivity::class.java)
    }

    @SuppressLint("NewApi")
    fun isRunningForegroundToApp1(context: Context, Class: Class<*>?) {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskInfoList = activityManager.getRunningTasks(20)
        /**枚举进程 */
        for (taskInfo in taskInfoList) {
            //*找到本应用的 task，并将它切换到前台
            if (taskInfo.baseActivity!!.packageName == context.packageName) {
                Log.e("timerTask", "timerTask  pid " + taskInfo.id)
                Log.e("timerTask", "timerTask  processName " + taskInfo.topActivity!!.packageName)
                Log.e("timerTask", "timerTask  getPackageName " + context.packageName)
                activityManager.moveTaskToFront(taskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME)
                val intent = Intent(context, Class)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.putExtra(Constant.RETURN_CURRENT_PAGE, true)

                intent.action = Intent.ACTION_MAIN
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                context.startActivity(intent)
                break
            }
        }
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    /** Inner class that loads and shows app open ads. */
    inner class AppOpenAdManager {

        var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                AD_UNIT_ID,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(LOG_TAG, "onAdLoaded.")
                        Toast.makeText(context, "onAdLoaded", Toast.LENGTH_SHORT).show()
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.message)
                        Toast.makeText(context, "onAdFailedToLoad", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        /** Check if ad was loaded more than n hours ago. */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            // Ad references in the app open beta will time out after four hours, but this time limit
            // may change in future beta versions. For details, see:
            // https://support.google.com/admob/answer/9341964?hl=en
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(1)
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                    }
                }
            )
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
//                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }

            Log.d(LOG_TAG, "Will show ad.")

            appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                /** Called when full screen content is dismissed. */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(LOG_TAG, "onAdDismissedFullScreenContent.")
                    Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT)
                        .show()

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                /** Called when fullscreen content failed to show. */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                    Toast.makeText(
                        activity,
                        "onAdFailedToShowFullScreenContent",
                        Toast.LENGTH_SHORT
                    ).show()

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                /** Called when fullscreen content is shown. */
                override fun onAdShowedFullScreenContent() {
                    Log.d(LOG_TAG, "onAdShowedFullScreenContent.")
                    Toast.makeText(activity, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            isShowingAd = true
            if (ActivityCollector.isActivityExist(StartupActivity::class.java)) {
                appOpenAd!!.show(activity)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }

}
