package com.demo.blackbutton.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.demo.blackbutton.R
import com.demo.blackbutton.ad.AdLoad
import com.demo.blackbutton.app.App
import com.demo.blackbutton.base.BaseActivity
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.ui.agreement.AgreementWebView
import com.demo.blackbutton.ui.servicelist.ServiceListActivity
import com.demo.blackbutton.utils.*
import com.demo.blackbutton.utils.ActivityCollector.getActivity
import com.demo.blackbutton.utils.GetLocalData.addClicksCount
import com.demo.blackbutton.utils.GetLocalData.addShowCount
import com.demo.blackbutton.utils.GetLocalData.isAdExceedLimit
import com.demo.blackbutton.utils.ObjectSaveUtils.getObject
import com.demo.blackbutton.utils.Utils.FlagConversion
import com.demo.blackbutton.widget.SlidingMenu
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.common.ClickUtils
import com.xuexiang.xutil.tip.ToastUtils
import java.text.DecimalFormat
import kotlinx.coroutines.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher


class MainActivity : BaseActivity(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    private val LOG_TAG = "ad-log"
    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var rightTitle: ImageView
    private lateinit var navigation: ImageView
    private lateinit var imgSwitch: LottieAnimationView
    private lateinit var txtConnect: TextView
    private lateinit var imgCountry: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var slidingMenu: SlidingMenu
    private lateinit var laHomeMenu: RelativeLayout
    private lateinit var tvContact: TextView
    private lateinit var tvAgreement: TextView
    private lateinit var tvShare: TextView
    private lateinit var radioGroup: LinearLayout
    private lateinit var radioButton0: TextView
    private lateinit var radioButton1: TextView
    private lateinit var clSwitch: ConstraintLayout
    private lateinit var txtTimer: TextView

    private val startVpn = MutableLiveData<Boolean>()

    //后台回来刷新
    private val backstageRefresh = MutableLiveData<Boolean>()

    // 后台关闭广告
    private var backgroundCloseAd = false

    //connect插屏show
    private val interstitialAdShow = MutableLiveData<Boolean>()

    // connectInterstitialAd show
    private val connectInterstitialAdShow = MutableLiveData<Boolean>()

    // 是否在resume状态
    private var resumeState = false
    private var interstitialJob: Job? = null

    //关闭插屏广告
    private var closeScreenAd = false

    // 是否能跳转
    private var canIJump = false
    private lateinit var adRequest: AdRequest
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private lateinit var bestServiceData: ProfileBean.SafeLocation
    private var isFrontDesk = false
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
    }
    private lateinit var ad_frame: FrameLayout
    private lateinit var imgAdFrame: ImageView
    var currentNativeAd: NativeAd? = null
    private var screenAdIndex: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(resources.displayMetrics) {
            density = heightPixels / 780.0F
            densityDpi = (160 * density).toInt()
        }
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_main)
        ActivityCollector.addActivity(this, javaClass)

        backstageRefresh.observe(this) {
            KLog.e("isBack", "执行刷新home原生方法=$it")
            initNativeAds()
        }
        interstitialAdShow.observe(this) {
            KLog.e("TAG3", "vpnSwitch7=${AdLoad.connectInterstitialAd}")

            connectInterstitialAdShow.postValue(true)
        }
        connectInterstitialAdShow.observe(this) {
            KLog.e("TAG3", "vpnSwitch8=${AdLoad.connectInterstitialAd}")
            lifecycleScope.launch {
                delay(1000)
                KLog.e("TAG3", "vpnSwitch9=${resumeState}")
                if (resumeState) {
                    AdLoad.connectInterstitialAd?.show(this@MainActivity)
                }
            }

        }
        startVpn.observe(this) {
            KLog.e("TAG3", "startVpn----1--->${App.isBackData}")
            if (!App.isBackData) {
                KLog.e("TAG3", "startVpn--2----->")
                startVpn()
            }
        }
        initParam()
        initView()
        initScreenAd()
        clickEvent()
        initConnectionServer()
        initLiveBus()
    }

    /**
     * initParam
     */
    private fun initParam() {
        bestServiceData = JsonUtil.fromJson(
            mmkv.decodeString(Constant.BEST_SERVICE_DATA),
            object : TypeToken<ProfileBean.SafeLocation?>() {}.type
        )
    }

    private fun initLiveBus() {
        LiveEventBus
            .get(Constant.SERVER_INFORMATION, ProfileBean.SafeLocation::class.java)
            .observeForever {
                updateServer(it)
            }
        LiveEventBus
            .get(Constant.PLUG_ADVERTISEMENT_CACHE, InterstitialAd::class.java)
            .observeForever {
                if (it == null) {
                    screenAdIndex++
                    AdLoad.connectInterstitialAd = null
                    loadScreenAdvertisement(adRequest)

                } else {
                    screenAdIndex = 0
                    plugInAdvertisementCallback()
                }
            }
        // 更新计时器
        LiveEventBus
            .get(Constant.TIMER_DATA, Int::class.java)
            .observeForever {
                timerUi(it)
            }
        LiveEventBus
            .get(Constant.PLUG_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observe(this) {
                KLog.e("TAG3", "home插屏展示-LiveEventBus")
                startVpn()
            }

    }

    private fun initScreenAd() {
        if (AdLoad.connectInterstitialAd != null) {
            return
        }
        App.isAppOpenSameDay()
        if (!isAdExceedLimit()) {
            adRequest = AdRequest.Builder().build()
            loadScreenAdvertisement(adRequest)
        }
    }

    /**
     *home native
     */
    private fun initNativeAds() {
        KLog.e("TAG3","initNativeAds-1111-AdLoad.homeNativeAd=${AdLoad.homeNativeAd}")

        App.isAppOpenSameDay()
        if (isAdExceedLimit()) {
            return
        }
        KLog.e("TAG3","initNativeAds-2222-AdLoad.homeNativeAd=${AdLoad.homeNativeAd}")
        AdLoad.homeNativeAd.let {
            if (it != null) {
                val activityDestroyed: Boolean = isDestroyed
                if (activityDestroyed || isFinishing || isChangingConfigurations) {
                    it.destroy()
                    return
                }
                currentNativeAd?.destroy()
                currentNativeAd = it
                val adView = layoutInflater
                    .inflate(R.layout.layout_ad_view, null) as NativeAdView
                populateNativeAdView(it, adView)
                ad_frame.removeAllViews()
                ad_frame.addView(adView)
                adSlotSwitching(true)
                KLog.d(LOG_TAG, "home----show")
                addShowCount()
                AdLoad.whetherShowHomeAd = false
                AdLoad.homeNativeAd = null
                //重新缓存
                AdLoad.loadHomeNativeAds(applicationContext)
            }
        }
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
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }

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

    private fun loadScreenAdvertisement(adRequest: AdRequest) {
        val id = GetLocalData.getAdId(GetLocalData.weightSorting().black_connect, screenAdIndex)
        if (id == "") {
            return
        }
        KLog.d(
            LOG_TAG,
            "connect---adUnitId=${id};weight=${GetLocalData.weightSorting().black_connect[screenAdIndex].bb_w}"
        )
        AdLoad.loadScreenAdvertisement(this, id, adRequest)
    }

    @SuppressLint("SetTextI18n")
    private fun timerUi(it: Int) {
        val hh: String = DecimalFormat("00").format(it / 3600)
        val mm: String = DecimalFormat("00").format(it % 3600 / 60)
        val ss: String = DecimalFormat("00").format(it % 60)
        txtTimer.text = "$hh:$mm:$ss"
    }

    private fun initView() {
        frameLayoutTitle = findViewById(R.id.main_title)
        frameLayoutTitle.setPadding(
            0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 50, 0, 0
        )
        imgSwitch = findViewById(R.id.img_switch)
        txtConnect = findViewById(R.id.txt_connect)
        imgCountry = findViewById(R.id.img_country)
        tvLocation = findViewById(R.id.tv_location)
        radioGroup = findViewById(R.id.radio_group)
        radioButton0 = findViewById(R.id.radio_button0)
        radioButton1 = findViewById(R.id.radio_button1)
        rightTitle = frameLayoutTitle.findViewById(R.id.ivRight)
        navigation = frameLayoutTitle.findViewById(R.id.ivBack)
        slidingMenu = findViewById(R.id.slidingMenu)
        laHomeMenu = findViewById(R.id.la_home_menu)
        tvContact = laHomeMenu.findViewById(R.id.tv_contact)
        tvAgreement = laHomeMenu.findViewById(R.id.tv_agreement)
        tvShare = laHomeMenu.findViewById(R.id.tv_share)
        clSwitch = findViewById(R.id.cl_switch)
        ad_frame = findViewById(R.id.ad_frame)
        imgAdFrame = findViewById(R.id.img_ad_frame)
        txtTimer = findViewById(R.id.txt_timer)
    }

    /**
     * 点击事件
     */
    private fun clickEvent() {
        navigation.setOnClickListener {
            if (!imgSwitch.isAnimating) {
                slidingMenu.open()
            }
        }
        tvContact.setOnClickListener {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            startActivity(Intent.createChooser(intent, "Please select mail application"))
        }
        tvAgreement.setOnClickListener {
            val intent = Intent(this@MainActivity, AgreementWebView::class.java)
            startActivity(intent)
        }
        tvShare.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, Constant.SHARE_ADDRESS + this.packageName)
            intent.type = "text/plain"
            startActivity(intent)
        }
        rightTitle.setOnClickListener {
            if (!imgSwitch.isAnimating) {
                val intent = Intent(this@MainActivity, ServiceListActivity::class.java)
                if (state.name == "Connected") {
                    intent.putExtra(Constant.WHETHER_CONNECTED, true)
                } else {
                    intent.putExtra(Constant.WHETHER_CONNECTED, false)
                }
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP//它可以关掉所要到的界面中间的activity
                intent.putExtra(Constant.CURRENT_IP, bestServiceData.bb_ip)
                intent.putExtra(Constant.WHETHER_BEST_SERVER, bestServiceData.bestServer)
                startActivity(intent)
            }
        }
        clSwitch.setOnClickListener {
            if (!imgSwitch.isAnimating) {
                vpnSwitchType()
            }
        }
        radioGroup.setOnClickListener {
            if (!imgSwitch.isAnimating) {
                vpnSwitchType()
            }
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            imgSwitch.pauseAnimation()
            ToastUtils.toast(R.string.insufficient_permissions)
        } else {
            if (NetworkPing.isNetworkAvailable(this)) {
                App.isAppOpenSameDay()
                if (isAdExceedLimit()) {
                    vpnSwitchNoAd()
                } else {
                    vpnSwitch()
                }
            } else {
                imgSwitch.pauseAnimation()
                ToastUtils.toast("The current device has no network")
            }
        }
    }

    private fun vpnSwitchType() {
        connect.launch(null)
    }

    /**
     * 加载原生广告
     */
    private fun loadNativeAds() {
        if (AdLoad.resultNativeAd != null) {
            return
        }
        App.isAppOpenSameDay()
        if (!isAdExceedLimit()) {
            AdLoad.loadResultNativeAds(applicationContext)
        }
    }

    /**
     * vpnSwitch
     */
    private fun vpnSwitch() {
        initScreenAd()
        loadNativeAds()
        canIJump = true
        imgSwitch.playAnimation()
        MmkvUtils.set(Constant.SLIDING, true)
        KLog.e("TAG3", "vpnSwitch1")
        interstitialJob = lifecycleScope.launch {
            try {
                KLog.e("TAG3", "vpnSwitch2")
                withTimeout(10000L) {
                    delay(1000)
                    while (isActive) {
                        KLog.e("TAG3", "vpnSwitch3")
                        delay(1000L)
                        KLog.e("TAG3", "vpnSwitch4")
                        if (AdLoad.connectInterstitialAd != null) {
                            KLog.e("TAG3", "vpnSwitch5")
                            interstitialAdShow.postValue(true)
                            interstitialJob?.cancel()
                            interstitialJob = null
                        }
                        KLog.e("TAG3", "while (vpnSwitch)")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TAG3", "vpnSwitch6")
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                isFrontDesk = true
                startVpn()
            }
        }
    }

    /**
     * vpnSwitch(无广告)
     */
    private fun vpnSwitchNoAd() {
        canIJump = true
        imgSwitch.playAnimation()
        MmkvUtils.set(Constant.SLIDING, true)
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000L)
            startVpn()
        }
    }

    /**
     * 开关状态
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setSwitchStatus() {
        if (state.name == "Connected") {
            radioButton0.setTextColor(getColor(R.color.white))
            radioButton0.background = resources.getDrawable(R.drawable.radio_bg_check)

            radioButton1.setTextColor(getColor(R.color.white))
            radioButton1.background = null
        } else {
            radioButton1.setTextColor(getColor(R.color.white))
            radioButton1.background = resources.getDrawable(R.drawable.radio_bg_check)

            radioButton0.setTextColor(getColor(R.color.white))
            radioButton0.background = null
        }
    }

    /**
     * 初始连接服务器
     */
    private fun initConnectionServer() {
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        ProfileManager.getProfile(DataStore.profileId).let {
            settingsIcon(bestServiceData)
            if (it != null) {
                ProfileManager.updateProfile(setServerData(it, bestServiceData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setServerData(profile, bestServiceData))
            }
        }
        DataStore.profileId = 1L
    }


    /**
     * 更新服务器
     */
    private fun updateServer(safeLocation: ProfileBean.SafeLocation) {
        settingsIcon(safeLocation)
        bestServiceData = safeLocation
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setServerData(it, safeLocation)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        isFrontDesk = true
        vpnSwitchType()
    }

    /**
     * 设置服务器数据
     */
    private fun setServerData(profile: Profile, bestData: ProfileBean.SafeLocation): Profile {
        profile.name = bestData.bb_country + "-" + bestData.bb_city
        profile.host = bestData.bb_ip.toString()
        profile.remotePort = bestData.bb_port!!
        profile.password = bestData.bb_pwd!!
        profile.method = bestData.bb_method!!
        return profile
    }

    /**
     * 设置图标
     */
    @SuppressLint("SetTextI18n")
    private fun settingsIcon(profileBean: ProfileBean.SafeLocation) {
        if (profileBean.bestServer == true) {
            tvLocation.text = Constant.FASTER_SERVER
            imgCountry.setImageResource(FlagConversion(Constant.FASTER_SERVER))
        } else {
            tvLocation.text = profileBean.bb_country + "-" + profileBean.bb_city
            imgCountry.setImageResource(FlagConversion(profileBean.bb_country))
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        if (state.canStop) {
            disConnectToTheVpnService()
        } else {
            connectToTheVpnService()
        }
    }

    /**
     * 断开vpn服务
     */
    private fun disConnectToTheVpnService() {
        Core.stopService()
        jumpToTheResultPage(false)
    }

    /**
     * 连接vpn服务
     */
    private fun connectToTheVpnService() {
        Core.startService()
    }

    override fun onServiceDisconnected() = changeState(BaseService.State.Idle)
    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        setConnectionStatusText(state.name)
        this.state = state
        MmkvUtils.set(Constant.TIMING_DATA, state.name)
        setSwitchStatus()
        stateListener?.invoke(state)
    }

    /**
     * 设置连接状态文本
     */
    @SuppressLint("SetTextI18n")
    private fun setConnectionStatusText(state: String) {
        when (state) {
            "Connecting" -> {
                txtConnect.text = "Connecting..."
            }
            "Connected" -> {
                // 连接成功
                connectionSucceeded()
                txtConnect.text = "Connected"
            }
            "Stopping" -> {
                txtConnect.text = "Stopping"
            }
            "Stopped" -> {
                connectionStop()
                txtConnect.text = "Connect"
            }
            else -> {
                txtConnect.text = "Configuring"
            }
        }

    }

    /**
     * 连接成功
     */
    private fun connectionSucceeded() {
        NetworkPing.start()
        jumpToTheResultPage(true)
    }

    /**
     * 连接停止
     */
    private fun connectionStop() {
        NetworkPing.cancel()
        KLog.e("TAG3", "链接停止")
        txtTimer.text = "00:00:00"
    }

    /**
     * 跳转结果页
     */
    private fun jumpToTheResultPage(flag: Boolean) {
        imgSwitch.pauseAnimation()
        MmkvUtils.set(Constant.SLIDING, false)
        if (!isFrontDesk) {
            setSwitchStatus()
            return
        }
        if (canIJump && !App.isBackData) {
            val intent = Intent(this@MainActivity, ResultsActivity::class.java)
            intent.putExtra(Constant.CONNECTION_STATUS, flag)
            startActivity(intent)
            canIJump = false
            closeScreenAd = false
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) =
        changeState(state)

    override fun onServiceConnected(service: IShadowsocksService) = run {
        KLog.e("TAG", "changeState: --->${service.state}")
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    /**
     * 插屏广告回调
     */
    private fun plugInAdvertisementCallback() {
        AdLoad.connectInterstitialAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(LOG_TAG, "Ad was clicked.")
                    addClicksCount()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    closeScreenAd = true
                    KLog.e("TAG3", "关闭home页插屏广告${App.isBackData}")

                    isFrontDesk = true
                    backgroundCloseAd = App.isBackData
                    if (!App.isBackData) {
                        KLog.e("TAG3", "LiveEventBus=${App.isBackData}")
                        LiveEventBus.get<Boolean>(Constant.PLUG_ADVERTISEMENT_SHOW)
                            .post(true)
                    }
                    AdLoad.connectInterstitialAd = null
                    loadScreenAdvertisement(adRequest)
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.e("TAG", "Ad failed to show fullscreen content.")
                    AdLoad.connectInterstitialAd = null
                    loadScreenAdvertisement(adRequest)
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    AdLoad.connectInterstitialAd = null
                    // Called when ad is shown.
                    AdLoad.whetherShowConnectAd = false
                    KLog.d(LOG_TAG, "connect----show")
                    addShowCount()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        KLog.e("TAG3", "MainActivity-onStart")
        initNativeAds()
        connection.bandwidthTimeout = 500
        isFrontDesk = true
        if (StartupActivity.whetherReturnCurrentPage && backgroundCloseAd) {
            StartupActivity.whetherReturnCurrentPage = false
            backgroundCloseAd = false
            KLog.e("TAG2", "vpnSwitchNoAd")
            vpnSwitchNoAd()
        }
    }

    override fun onResume() {
        super.onResume()
        KLog.e("TAG3", "MainActivity-onResume=${closeScreenAd};${App.isBackData}")
        isFrontDesk = true
        resumeState = true
    }

    override fun onPause() {
        super.onPause()
        KLog.e("TAG3", "MainActivity-onPause")
        imgSwitch.pauseAnimation()
        resumeState = false
    }

    override fun onStop() {
        super.onStop()
        KLog.e("TAG3", "MainActivity-onStop")
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        KLog.e("TAG3", "MainActivity-onDestroy")
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        ActivityCollector.removeActivity(this)
        currentNativeAd?.destroy()
    }

    /**
     * 菜单、返回键响应
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            getActivity(StartupActivity::class.java)?.let {
                ActivityCollector.removeActivity(it)
            }
            finish()
        }
        return true
    }

    override fun onRestart() {
        super.onRestart()
        KLog.e("TAG", "MainActivity-onRestart${App.isBackData}")
        if (App.isBackData) {
            initNativeAds()
        }
    }
}