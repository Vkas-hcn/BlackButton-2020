package com.demo.blackbutton.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.demo.blackbutton.R
import com.demo.blackbutton.ad.AdLoad
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.DensityUtils
import com.demo.blackbutton.utils.GetLocalData
import com.demo.blackbutton.utils.StatusBarUtils
import com.example.testdemo.utils.KLog
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.xuexiang.xutil.common.ClickUtils

class ResultsActivity : AppCompatActivity(){
    private  val LOG_TAG = "ad-log"
    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var ivRight: ImageView
    private var connectionStatus: Boolean = false
    private lateinit var imgConnectInfo: ImageView
    private lateinit var tvConnectInfo: TextView

    private lateinit var ad_frame: FrameLayout
    private lateinit var imgAdFrame: ImageView
    var currentNativeAd: NativeAd? = null
    private lateinit var mNativeAds: AdLoader.Builder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_results)
        supportActionBar?.hide()
        initView()
        initNativeAds()
    }

    private fun initView() {
        frameLayoutTitle = findViewById(R.id.bar_results)
        frameLayoutTitle.setPadding(
            0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 50, 0, 0
        )
        blackTitle = findViewById(R.id.ivBack)
        imgTitle = findViewById(R.id.img_title)
        tvTitle = findViewById(R.id.tv_title)
        ivRight = findViewById(R.id.ivRight)
        imgConnectInfo = findViewById(R.id.img_connect_info)
        tvConnectInfo = findViewById(R.id.tv_connect_info)
        ad_frame = findViewById(R.id.ad_frame_results)
        imgAdFrame = findViewById(R.id.img_ad_frame_results)
        imgTitle.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        ivRight.visibility = View.GONE
        blackTitle.setImageResource(R.mipmap.ic_black)
        blackTitle.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        connectionStatus = intent.getBooleanExtra(Constant.CONNECTION_STATUS, false)
        if (connectionStatus) {
            imgConnectInfo.setImageResource(R.mipmap.ic_connected)
            tvConnectInfo.text = getString(R.string.connected_succeeded)
        } else {
            imgConnectInfo.setImageResource(R.mipmap.ic_disconnected)
            tvConnectInfo.text = getString(R.string.disconnected_succeeded)
        }
    }
    /**
     * 初始化原生广告
     */
    private fun initNativeAds() {
        mNativeAds = AdLoad.resultNativeAds
        mNativeAds.forNativeAd { nativeAd ->
            var activityDestroyed = false
            activityDestroyed = isDestroyed
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val adView = layoutInflater
                .inflate(R.layout.layout_ad_results, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            ad_frame.removeAllViews()
            ad_frame.addView(adView)
            adSlotSwitching(true)
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        mNativeAds.withNativeAdOptions(adOptions)
        val adLoader = mNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                KLog.d(LOG_TAG, "result---onAdLoaded-Failed=$error")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(LOG_TAG, "result---onAdLoaded-finish")

            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
        KLog.d(LOG_TAG, "result----show")
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }?.setMediaContent(it) }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)

    }

    /**
     * 广告位切换
     */
    private fun adSlotSwitching(flag: Boolean) {
        if (flag) {
            ad_frame.visibility = View.VISIBLE
            imgAdFrame.visibility = View.GONE
        } else {
            ad_frame.visibility = View.GONE
            imgAdFrame.visibility = View.VISIBLE
        }
    }

}