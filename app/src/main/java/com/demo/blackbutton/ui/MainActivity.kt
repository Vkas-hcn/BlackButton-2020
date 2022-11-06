package com.demo.blackbutton.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.demo.blackbutton.R
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.ui.agreement.AgreementWebView
import com.demo.blackbutton.ui.servicelist.ServiceListActivity
import com.demo.blackbutton.utils.*
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
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.common.ClickUtils
import com.xuexiang.xutil.tip.ToastUtils
import java.text.DecimalFormat
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener, ClickUtils.OnClick2ExitListener {
    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

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

    // 是否能跳转
    private var canIJump = false
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private lateinit var bestServiceData: ProfileBean.SafeLocation
    private var isFrontDesk = false
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(resources.displayMetrics) {
            density = heightPixels / 780.0F
            densityDpi = (160 * density).toInt()
        }
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_main)
        initParam()
        initView()
        clickEvent()
        initConnectionServer()
        initLiveBus()
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            imgSwitch.pauseAnimation()
            ToastUtils.toast(R.string.insufficient_permissions)
        } else {
            Core.startService()
        }
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
        // 更新计时器
        LiveEventBus
            .get(Constant.TIMER_DATA, Int::class.java)
            .observeForever {
                timerUi(it)
            }
    }

    /**
     *
     */
    @SuppressLint("SetTextI18n")
    private fun timerUi(it :Int) {
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
                intent.putExtra(Constant.CURRENT_IP, bestServiceData.bb_ip)
                intent.putExtra(Constant.WHETHER_BEST_SERVER, bestServiceData.bestServer)

                startActivity(intent)
            }
        }
        radioGroup.setOnClickListener {
            vpnSwitch()
//            NetworkPing.cancel()
        }
        clSwitch.setOnClickListener {
            vpnSwitch()

        }
    }

    /**
     * vpnSwitch
     */
    private fun vpnSwitch() {
        canIJump = true
        imgSwitch.playAnimation()
        MmkvUtils.set(Constant.SLIDING, true)
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000L)
            startVpn()
        }
//        mInterstitialAd?.show(this)
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
        vpnSwitch()
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
        if (NetworkPing.isNetworkAvailable(this)) {
            connect.launch(null)
        } else {
            Looper.prepare()
            imgSwitch.pauseAnimation()
            ToastUtils.toast("The current device has no network")
            Looper.loop()
        }
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
        if (canIJump) {
            val intent = Intent(this@MainActivity, ResultsActivity::class.java)
            intent.putExtra(Constant.CONNECTION_STATUS, flag)
            startActivity(intent)
            canIJump = false
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

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
        isFrontDesk = true
    }

    override fun onResume() {
        super.onResume()
        isFrontDesk = true
    }

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
//        DataStore.publicStore.unregisterChangeListener(this)
//        connection.disconnect(this)
//        currentNativeAd?.destroy()
    }

    override fun onPause() {
        super.onPause()
        isFrontDesk = false
    }

    override fun onRetry() {
        finish()
    }

    override fun onExit() {
//        XUtil.get().exitApp()
    }
}