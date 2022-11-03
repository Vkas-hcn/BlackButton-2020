package com.demo.blackbutton.ad

import android.content.Context
import android.util.Log
import com.demo.blackbutton.constant.Constant
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.jeremyliao.liveeventbus.LiveEventBus

object AdLoad {
    private var mInterstitialAd: InterstitialAd? = null

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
                    adError.toString().let { Log.d("TAG", "===$it") }
                    LiveEventBus.get<String>(Constant.PLUG_ADVERTISEMENT_CACHE).post(null)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    LiveEventBus.get<InterstitialAd>(Constant.PLUG_ADVERTISEMENT_CACHE).post(interstitialAd)
                }
            })
    }

    /**
     * 加载原生广告
     */
    fun loadNativeAds(context: Context, adUnitId: String):AdLoader {
        return AdLoader.Builder(context, adUnitId)
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    // Handle the failure by logging, altering the UI, and so on.
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // Methods in the NativeAdOptions.Builder class can be
                    // used here to specify individual options settings.
                    .build()
            )
            .build()
    }
}