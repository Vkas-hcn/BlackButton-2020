package com.demo.blackbutton.ad

import android.content.Context
import com.demo.blackbutton.app.App
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.ui.StartupActivity
import com.demo.blackbutton.utils.ActivityCollector
import com.demo.blackbutton.utils.GetLocalData
import com.demo.blackbutton.utils.GetLocalData.addClicksCount
import com.demo.blackbutton.utils.GetLocalData.getAdId
import com.demo.blackbutton.utils.GetLocalData.weightSorting
import com.example.testdemo.utils.KLog
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.*
import java.util.*

object AdLoad {
    var connectInterstitialAd: InterstitialAd? = null
    var backInterstitialAd: InterstitialAd? = null
    private const val LOG_TAG = "ad-log"
    private var nativeHomeAdIndex: Int = 0
    private var nativeResultAdIndex: Int = 0
    // 是否刷新广告
    var isRefreshAd: Boolean = false

    var homeNativeAd: NativeAd? = null
    var resultNativeAd: NativeAd? = null

    //是否显示了home广告
    var whetherShowHomeAd = false
    //是否显示了Result广告
    var whetherShowResultAd = false

    //是否显示了back广告
    var whetherShowBackAd = false
    private var screenAdIndex: Int = 0
    //是否显示了connect广告
    var whetherShowConnectAd = false

    var appOpenAd: AppOpenAd? = null
    var isLoadingAds = false
    var isShowingAd = false
    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
     var loadTime: Long = 0
     var openAdIndex: Int = 0
    /**
     * 加载首页插屏广告
     */
    fun loadScreenAdvertisement(
        context: Context,
        adUnitId: String,
        adRequest: AdRequest
    ) {
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(LOG_TAG, "connect---FailedToLoad=$it") }
                    LiveEventBus.get<String>(Constant.PLUG_ADVERTISEMENT_CACHE).post(null)
                    connectInterstitialAd =null
                    whetherShowConnectAd = true
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    connectInterstitialAd = interstitialAd
                    LiveEventBus.get<InterstitialAd>(Constant.PLUG_ADVERTISEMENT_CACHE)
                        .post(interstitialAd)
                    whetherShowConnectAd = true
                    KLog.d(LOG_TAG, "connect---onAdLoaded-finish")
                }
            })
    }

    /**
     * 加载返回插屏广告
     */
    fun loadBackScreenAdvertisement(
        context: Context,
        adRequest: AdRequest
    ) {
        KLog.d(LOG_TAG,"返回页插屏广告----$whetherShowBackAd")
        if(whetherShowBackAd){
            KLog.d(LOG_TAG,"返回页插屏广告还未展示")
            return
        }
        val id = getAdId(weightSorting().black_back, screenAdIndex)
        if (id == "") {
            return
        }
        KLog.d(
            LOG_TAG,
            "back---adUnitId=${id};weight=${weightSorting().black_back[screenAdIndex].bb_w}"
        )
        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(LOG_TAG, "back---FailedToLoad=$it") }
//                    LiveEventBus.get<String>(Constant.BACK_PLUG_ADVERTISEMENT_CACHE).post(null)
                    backInterstitialAd =null
                    screenAdIndex++
                    loadBackScreenAdvertisement(context,adRequest)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    screenAdIndex = 0
                    whetherShowBackAd = true
                    backInterstitialAd = interstitialAd
//                    LiveEventBus.get<InterstitialAd>(Constant.BACK_PLUG_ADVERTISEMENT_CACHE)
//                        .post(interstitialAd)
                    KLog.d(LOG_TAG, "back---onAdLoaded-finish")
                }
            })
    }

    /**
     * 加载home原生广告
     */
    fun loadHomeNativeAds(context: Context,refresh:Boolean=false) {
        if (whetherShowHomeAd) {
            KLog.d(LOG_TAG, "home原生广告-还没有展示")
            return
        }
        KLog.d(
            LOG_TAG,
            "home--adUnitId=${
                getAdId(
                    weightSorting().black_home,
                    nativeHomeAdIndex
                )
            };weight=${weightSorting().black_home[nativeHomeAdIndex].bb_w}"
        )
        val homeNativeAds = AdLoader.Builder(
            context.applicationContext,
            getAdId(weightSorting().black_home, nativeHomeAdIndex)
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        homeNativeAds.withNativeAdOptions(adOptions)
        homeNativeAds.forNativeAd {
            whetherShowHomeAd = true
            homeNativeAd = it
            if(refresh){
                LiveEventBus.get<Boolean>(Constant.HOME_NATIVE_REFRESH)
                    .post(true)
            }
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                homeNativeAd = null
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                KLog.d(LOG_TAG, "home---加载home原生广告失败=$error")
                if (nativeHomeAdIndex < weightSorting().black_home.size - 1) {
                    nativeHomeAdIndex++
                    loadHomeNativeAds(context,refresh)
                }
                isRefreshAd =false
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(LOG_TAG, "home---加载home原生广告成功")
                isRefreshAd =false
                nativeHomeAdIndex = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(LOG_TAG, "home---点击home原生广告")
                addClicksCount()

            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 加载result原生广告
     */
    fun loadResultNativeAds(context: Context) {
        if (whetherShowResultAd) {
            KLog.d(LOG_TAG, "Result原生广告-还没有展示")
            return
        }
        KLog.d(LOG_TAG, "result---onAdLoaded=$nativeResultAdIndex")
        KLog.d(
            LOG_TAG,
            "result--adUnitId=${
                getAdId(
                    weightSorting().black_result,
                    nativeResultAdIndex
                )
            };weight=${weightSorting().black_result[nativeResultAdIndex].bb_w}"
        )
        val resultNativeAds =
            AdLoader.Builder(context, getAdId(weightSorting().black_result, nativeResultAdIndex))
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()
        resultNativeAds.withNativeAdOptions(adOptions)
        resultNativeAds.forNativeAd {
            resultNativeAd = it
        }
        resultNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                resultNativeAd = null
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                KLog.d(LOG_TAG, "result---加载result原生广告失败=$error")
                if (nativeResultAdIndex < weightSorting().black_result.size - 1) {
                    nativeResultAdIndex++
                    loadResultNativeAds(context)
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                nativeResultAdIndex = 0
                KLog.d(LOG_TAG, "result---加载result原生广告成功")
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(LOG_TAG, "result---点击原生广告")
                addClicksCount()

            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 加载open开屏广告
     */
    fun loadOpenAds(context: Context) {
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
                    openAdIndex = 0
                    KLog.d(LOG_TAG, "open-onAdLoaded-Finish")
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAds = false
                    if (openAdIndex < weightSorting().black_open.size - 1) {
                        openAdIndex++
                        loadOpenAds(context)
                    }
                    KLog.d(LOG_TAG, "open-onAdFailedToLoad: " + loadAdError.message)
                }
            }
        )
    }
    /** Check if ad was loaded more than 1 hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour
    }

    /** Check if ad exists and can be shown. */
    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

}