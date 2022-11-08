package com.demo.blackbutton.ad

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.demo.blackbutton.app.App
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.MmkvUtils
import com.demo.blackbutton.utils.NetworkPing
import com.demo.blackbutton.utils.ObjectSaveUtils.saveObject
import com.example.testdemo.utils.KLog
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.*

object AdLoad {
    private var mInterstitialAd: InterstitialAd? = null
    private val job = Job()
    private const val LOG_TAG = "ad-log"

    /**
     * 存储插屏广告
     */
    fun storeScreenAdvertisement() {

    }

    /**
     * 加载插屏广告
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
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    LiveEventBus.get<InterstitialAd>(Constant.PLUG_ADVERTISEMENT_CACHE).post(interstitialAd)
                    KLog.d(LOG_TAG, "connect---onAdLoaded-finish")
                }
            })
    }
    /**
     * 加载返回插屏广告
     */
    fun loadBackScreenAdvertisement(
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
                    adError.toString().let { KLog.d(LOG_TAG, "back---FailedToLoad=$it") }
                    LiveEventBus.get<String>(Constant.BACK_PLUG_ADVERTISEMENT_CACHE).post(null)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    LiveEventBus.get<InterstitialAd>(Constant.BACK_PLUG_ADVERTISEMENT_CACHE).post(interstitialAd)
                    KLog.d(LOG_TAG, "back---onAdLoaded-finish")
                }
            })
    }
    lateinit var homeNativeAds: AdLoader.Builder
    lateinit var resultNativeAds: AdLoader.Builder

    /**
     * 加载home原生广告
     */
    fun loadHomeNativeAds(context: Context, adUnitId: String) {
        KLog.d(LOG_TAG,"home---onAdLoaded")
        homeNativeAds = AdLoader.Builder(context, adUnitId)
    }
    /**
     * 加载result原生广告
     */
    fun loadResultNativeAds(context: Context, adUnitId: String) {
        KLog.d(LOG_TAG,"result---onAdLoaded")
        resultNativeAds = AdLoader.Builder(context, adUnitId)
    }
    /**
     * 请求开屏广告
     */
    fun requestScreenOpenAd(){
        val application = App()

        var openAdLaunch = CoroutineScope(job)
        var num =0
        openAdLaunch.launch {
            try {
                while (isActive) {
                    num++
                }
            }finally {
            }

        }
    }
}