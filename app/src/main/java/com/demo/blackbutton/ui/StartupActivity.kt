package com.demo.blackbutton.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.R
import com.demo.blackbutton.ad.AdLoad
import com.demo.blackbutton.app.App
import com.demo.blackbutton.base.BaseActivity
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.*
import com.demo.blackbutton.utils.GetLocalData.addClicksCount
import com.demo.blackbutton.utils.GetLocalData.addShowCount
import com.demo.blackbutton.utils.GetLocalData.isAdExceedLimit
import com.demo.blackbutton.utils.NetworkPing.findTheBestIp
import com.demo.blackbutton.widget.HorizontalProgressView
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.bean.AroundFlowBean
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.xuexiang.xutil.tip.ToastUtils
import java.util.*
import com.demo.blackbutton.utils.GetLocalData.weightSorting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import kotlinx.coroutines.*


/**
 * Startup Page
 */
class StartupActivity : BaseActivity(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private val LOG_TAG = "ad-log"

    companion object {
        var whetherReturnCurrentPage: Boolean = false
    }

    private lateinit var horizontalProgressView: HorizontalProgressView
    private val jumpPageLiveData = MutableLiveData<Boolean>()
    private val appOpenAdShow = MutableLiveData<Boolean>()

    // 绕流数据
    private lateinit var aroundFlowData: AroundFlowBean
//    private var appOpenAd: AppOpenAd? = null
//    private var isLoadingAds = false
//    var isShowingAd = false
//
//    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
//    private var loadTime: Long = 0
//    private var openAdIndex: Int = 0

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val data = intent?.getBooleanExtra(Constant.RETURN_CURRENT_PAGE, false)
        KLog.e("TAG", "Data====$data")
    }

    /**
     * initParam
     */
    private fun initParam() {
        whetherReturnCurrentPage = intent.getBooleanExtra(Constant.RETURN_CURRENT_PAGE, false)
    }

    private fun initView() {
        horizontalProgressView = findViewById(R.id.pb_start)
        horizontalProgressView.setProgressViewUpdateListener(this)
        horizontalProgressView.startProgressAnimation()
        aroundFlowData = AroundFlowBean()
        jumpPageLiveData.observe(this) {
            KLog.e("TAG", "jumpPageLiveData")
            jumpPage()
        }
        appOpenAdShow.observe(this) {
            AdLoad.isShowingAd = true
            AdLoad.appOpenAd!!.show(this)
            KLog.d(LOG_TAG, "open--show")
            addShowCount()
            lifecycleScope.cancel()
        }
        getFirebaseData()

    }

    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        App.isAppOpenSameDay()
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
                MmkvUtils.set(Constant.PROFILE_DATA, auth.getString("profileData"))
                MmkvUtils.set(Constant.PROFILE_DATA_FAST, auth.getString("profileData_fast"))
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
        if (!whetherReturnCurrentPage) {
            val intent = Intent(this@StartupActivity, MainActivity::class.java)
            val bestData = findTheBestIp()
            val dataJson = JsonUtil.toJson(bestData)
            MmkvUtils.set(Constant.BEST_SERVICE_DATA, dataJson)
            startActivity(intent)
        }
        finish()
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
        KLog.d(LOG_TAG, "loadOpenScreenAd-加载开屏广告=${AdLoad.appOpenAd}")
        if (AdLoad.appOpenAd != null) {
            lifecycleScope.launch {
                delay(2000L)
                showAdIfAvailable(this@StartupActivity)
            }
            return
        }
        lifecycleScope.launch {
            try {
                withTimeout(10000L) {
                    AdLoad.loadOpenAds(this@StartupActivity)
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

    private fun showAdIfAvailable(activity: Activity) {
        // If the app open ad is already showing, do not show the ad again.
        if (AdLoad.isShowingAd) {
            KLog.e(LOG_TAG, "The app open ad is already showing.")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!AdLoad.isAdAvailable()) {
            KLog.e("TAG", "The app open ad is not ready yet.")
            AdLoad.loadOpenAds(activity)
            return
        }

        KLog.e("TAG", "Will show ad.")
        AdLoad.appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            /** Called when full screen content is dismissed. */
            override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false.
                AdLoad.appOpenAd = null
                AdLoad.isShowingAd = false
                Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT)
                    .show()
                KLog.e("TAG", "onAdDismissedFullScreenContent.=${whetherReturnCurrentPage}")
                if (!App.whetherBackground) {
                    jumpPageLiveData.postValue(true)
                }
            }

            /** Called when fullscreen content failed to show. */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdLoad.appOpenAd = null
                AdLoad.isShowingAd = false
                KLog.e("TAG", "onAdFailedToShowFullScreenContent: " + adError.message)
            }

            /** Called when fullscreen content is shown. */
            override fun onAdShowedFullScreenContent() {
                AdLoad.appOpenAd = null
                KLog.e("TAG", "onAdShowedFullScreenContent.")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                KLog.d(LOG_TAG, "open---点击open广告")
                addClicksCount()
            }
        }
        appOpenAdShow.postValue(true)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //屏蔽禁用返回键的功能
        return keyCode == KeyEvent.KEYCODE_BACK
    }
}