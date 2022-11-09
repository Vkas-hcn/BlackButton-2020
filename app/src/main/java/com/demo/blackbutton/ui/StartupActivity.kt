package com.demo.blackbutton.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.R
import com.demo.blackbutton.ad.AdLoad
import com.demo.blackbutton.app.App
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.*
import com.demo.blackbutton.utils.GetLocalData.addClicksCount
import com.demo.blackbutton.utils.GetLocalData.addShowCount
import com.demo.blackbutton.utils.GetLocalData.getAdId
import com.demo.blackbutton.utils.GetLocalData.isAdExceedLimit
import com.demo.blackbutton.utils.NetworkPing.findTheBestIp
import com.demo.blackbutton.widget.HorizontalProgressView
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.bean.AroundFlowBean
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.tip.ToastUtils
import java.util.*
import com.demo.blackbutton.utils.GetLocalData.weightSorting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.*


/**
 * Startup Page
 */
class StartupActivity : AppCompatActivity(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private var whetherReturnCurrentPage: Int = 0
    private lateinit var horizontalProgressView: HorizontalProgressView
    private val LOG_TAG = "ad-log"

    // 绕流数据
    private lateinit var aroundFlowData: AroundFlowBean
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAds = false
    var isShowingAd = false

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0
    private var openAdIndex: Int = 0

    @DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        setContentView(R.layout.activity_startup)
        supportActionBar?.hide()
        ActivityCollector.addActivity(this, javaClass)
        initParam()
        initView()
    }

    /**
     * initParam
     */
    private fun initParam() {
        whetherReturnCurrentPage = intent.getIntExtra(Constant.RETURN_CURRENT_PAGE, 0)
    }

    private fun initView() {
        horizontalProgressView = findViewById(R.id.pb_start)
        horizontalProgressView.setProgressViewUpdateListener(this)
        horizontalProgressView.startProgressAnimation()
        aroundFlowData = AroundFlowBean()
        getFirebaseData()

    }

    /**
     * 加载广告
     */
    fun loadAdvertisement() {
        if (isAdExceedLimit()) {
            lifecycleScope.launch {
                delay(2000L)
                jumpPage()
            }
        } else {
            weightSorting()
            KLog.d("ad-log", "ad.json=${JsonUtil.toJSONObject(weightSorting())}")
            loadOpenScreenAd()
            loadNativeAds()
        }
    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
        if (BuildConfig.DEBUG) {
            loadAdvertisement()
            return
        } else {
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                ToastUtils.toast("fireBase Connection succeeded")
                MmkvUtils.set(Constant.AROUND_FLOW_DATA, auth.getString("aroundFlowData"))
                MmkvUtils.set(Constant.ADVERTISING_DATA, auth.getString("advertisingData"))
            }.addOnCompleteListener {
                loadAdvertisement()
            }
        }
    }

    /**
     * 加载原生广告
     */
    private fun loadNativeAds() {
        AdLoad.loadHomeNativeAds(applicationContext)
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        KLog.e("TAG", "jumpPage=$whetherReturnCurrentPage")
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (whetherReturnCurrentPage == 0) {
            val intent = Intent(this@StartupActivity, MainActivity::class.java)
            val bestData = findTheBestIp()
            val dataJson = JsonUtil.toJson(bestData)
            MmkvUtils.set(Constant.BEST_SERVICE_DATA, dataJson)
            startActivity(intent)
        }
        if (whetherReturnCurrentPage == 1 || whetherReturnCurrentPage == 2) {
            finish()
        }
    }

    /**
     * 跳转判断
     */
    fun jumpJudgment() {
        if (whetherReturnCurrentPage == 2) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        horizontalProgressView.stopProgressAnimation()
        horizontalProgressView.setProgressViewUpdateListener(null)
        ActivityCollector.removeActivity(this)
    }

    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }

    /**
     * 加载开屏广告
     */
    private fun loadOpenScreenAd() {
        if (appOpenAd != null) {
            lifecycleScope.launch {
                delay(2000L)
                showAdIfAvailable(this@StartupActivity)
            }
            return
        }
        lifecycleScope.launch {
            try {
                withTimeout(10000L) {
                    loadAd(this@StartupActivity)
                    delay(1000L)
                    while (isActive) {
                        showAdIfAvailable(this@StartupActivity)
                        delay(1000L)
                        KLog.e("TAG", "while (isActive)")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }

    /**
     * Load an ad.
     *
     * @param context the context of the activity that loads the ad
     */
    fun loadAd(context: Context) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAds || isAdAvailable()) {
            return
        }
        val id = getAdId(weightSorting().black_open, openAdIndex)
        if (id == "") {
            return
        }
        KLog.d(
            LOG_TAG,
            "open-load-adUnitId=${id};weight=${weightSorting().black_open[openAdIndex].bb_w}"
        )

        isLoadingAds = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
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
                    isLoadingAds = false
                    loadTime = Date().time
                    KLog.d(LOG_TAG, "open-onAdLoaded-Finish")
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAds = false
                    openAdIndex++
                    loadAd(this@StartupActivity)
                    KLog.d(LOG_TAG, "open-onAdFailedToLoad: " + loadAdError.message)
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
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(1)
    }

    fun showAdIfAvailable(activity: Activity) {
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            KLog.e(LOG_TAG, "The app open ad is already showing.")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            KLog.e("TAG", "The app open ad is not ready yet.")
            loadAd(activity)
            return
        }

        KLog.e("TAG", "Will show ad.")
        appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            /** Called when full screen content is dismissed. */
            override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT)
                    .show()
                loadAd(activity)
                KLog.e("TAG", "onAdDismissedFullScreenContent.=${whetherReturnCurrentPage}")
                jumpPage()
            }

            /** Called when fullscreen content failed to show. */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                KLog.e(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                loadAd(activity)
            }

            /** Called when fullscreen content is shown. */
            override fun onAdShowedFullScreenContent() {
                KLog.e("TAG", "onAdShowedFullScreenContent.")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                KLog.d(LOG_TAG, "open---点击open广告")
                addClicksCount()
            }
        }
        isShowingAd = true
        if (ActivityCollector.isActivityExist(StartupActivity::class.java)) {
            appOpenAd!!.show(activity)
            KLog.d(LOG_TAG, "open--show")
            addShowCount()
            lifecycleScope.cancel()
        }
    }

    override fun onStart() {
        super.onStart()
    }
}