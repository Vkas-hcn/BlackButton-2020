package com.demo.blackbutton.ui.servicelist

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.blackbutton.R
import com.demo.blackbutton.ad.AdLoad
import com.demo.blackbutton.app.App
import com.demo.blackbutton.base.BaseActivity
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.ui.MainActivity
import com.demo.blackbutton.ui.StartupActivity
import com.demo.blackbutton.utils.*
import com.demo.blackbutton.utils.DensityUtils.px2dp
import com.demo.blackbutton.utils.GetLocalData.addClicksCount
import com.demo.blackbutton.utils.GetLocalData.addShowCount
import com.demo.blackbutton.utils.GetLocalData.isAdExceedLimit
import com.demo.blackbutton.utils.ResourceUtils.readStringFromAssert
import com.demo.blackbutton.utils.Utils.addTheBestRoute
import com.demo.blackbutton.utils.Utils.isNullOrEmpty
import com.example.testdemo.utils.KLog
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.common.ClickUtils
import kotlinx.coroutines.cancel
import kotlin.system.exitProcess


class ServiceListActivity : BaseActivity() {
    private val LOG_TAG = "ad-log"

    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var ivRight: ImageView
    private lateinit var serviceListAdapter: ServiceListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var profileBean: ProfileBean
    private lateinit var safeLocation: MutableList<ProfileBean.SafeLocation>
    private lateinit var checkSafeLocation: ProfileBean.SafeLocation
    private lateinit var adRequest: AdRequest

    //选中IP
    private var selectIp: String? = null

    //whetherConnected
    private var whetherConnected = false
    private lateinit var tvConnect: TextView
    private var whetherBestServer = false
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
    }

    //返回主页
    private val returnHomePage = MutableLiveData<Boolean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_service_list)
        ActivityCollector.addActivity(this, javaClass)
        initParam()
        initLiveBus()
        initRecyclerView()
        loadScreenAdvertisement()
    }

    /**
     * initParam
     */
    private fun initParam() {
        selectIp = intent.getStringExtra(Constant.CURRENT_IP)
        whetherConnected = intent.getBooleanExtra(Constant.WHETHER_CONNECTED, false)
        whetherBestServer = intent.getBooleanExtra(Constant.WHETHER_BEST_SERVER, false)
    }

    private fun initRecyclerView() {
        frameLayoutTitle = findViewById(R.id.bar_service_list)
        frameLayoutTitle.setPadding(
            0,
            px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 50,
            0,
            0
        )
        blackTitle = findViewById(R.id.ivBack)
        imgTitle = findViewById(R.id.img_title)
        tvTitle = findViewById(R.id.tv_title)
        ivRight = findViewById(R.id.ivRight)
        tvConnect = findViewById(R.id.tv_connect)
        recyclerView = findViewById(R.id.rv_service_list)
        imgTitle.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        ivRight.visibility = View.GONE
        blackTitle.setImageResource(R.mipmap.ic_black)

        storageServerData()
        if (!whetherBestServer) {
            safeLocation.forEach {
                it.cheek_state = it.bb_ip == selectIp
                if (it.cheek_state == true) {
                    checkSafeLocation = it
                }
            }
            safeLocation[0].cheek_state = false
            checkSafeLocation.cheek_state = true
        }
        serviceListAdapter = ServiceListAdapter(safeLocation)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = serviceListAdapter
        clickEvent()
    }

    private fun initLiveBus() {
        returnHomePage.observe(this) {
            ActivityCollector.removeActivity(this)
        }
//        LiveEventBus
//            .get(Constant.BACK_PLUG_ADVERTISEMENT_CACHE, InterstitialAd::class.java)
//            .observeForever {
//                if (it == null) {
//                    screenAdIndex++
//                    AdLoad.backInterstitialAd = null
//                    KLog.e("TAG4", "back插屏加载失败")
//                    loadScreenAdvertisement()
//                } else {
//                    screenAdIndex = 0
//                    plugInAdvertisementCallback()
//                }
//            }
    }

    /**
     * 获取选中数据
     */
    private fun getSelectedData(position: Int) {
        safeLocation.forEachIndexed { index, _ ->
            safeLocation[index].cheek_state = position == index
            if (safeLocation[index].cheek_state == true) {
                checkSafeLocation = safeLocation[index]
            }
        }
    }

    /**
     * 点击事件
     */
    private fun clickEvent() {
        serviceListAdapter.setOnItemClickListener { _, _, position ->
            getSelectedData(position)
            serviceListAdapter.notifyDataSetChanged()
        }
        blackTitle.setOnClickListener {
            displayAdvertisementOrReturn()
        }
        tvConnect.setOnClickListener {
            if (whetherConnected) {
                disconnectDialogBox()
            } else {
                LiveEventBus.get<ProfileBean.SafeLocation>(Constant.SERVER_INFORMATION)
                    .post(checkSafeLocation)
                finish()
            }
        }
    }

    /**
     * 断开连接对话框
     */
    private fun disconnectDialogBox() {
        val dialog: android.app.AlertDialog? = android.app.AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<ProfileBean.SafeLocation>(Constant.SERVER_INFORMATION)
                    .post(checkSafeLocation)
            }.create()
        dialog?.show()

    }

    /**
     * 存储服务器数据
     */
    private fun storageServerData() {
        safeLocation = ArrayList()
        profileBean = ProfileBean()
        checkSafeLocation = ProfileBean.SafeLocation()
        profileBean = if (isNullOrEmpty(mmkv.decodeString(Constant.PROFILE_DATA))) {
            getMenuJsonData("serviceJson.json")
        } else {
            JsonUtil.fromJson(
                mmkv.decodeString(Constant.PROFILE_DATA),
                object : TypeToken<ProfileBean?>() {}.type
            )
        }
        safeLocation = profileBean.safeLocation!!
        safeLocation.add(0, addTheBestRoute(NetworkPing.findTheBestIp()))
        checkSafeLocation = safeLocation[0]
    }

    /**
     * @return 解析json文件
     */
    private fun getMenuJsonData(jsonName: String): ProfileBean {
        return JsonUtil.fromJson(
            readStringFromAssert(jsonName),
            object : TypeToken<ProfileBean?>() {}.type
        )
    }

    private fun loadScreenAdvertisement() {
        App.isAppOpenSameDay()
        if (isAdExceedLimit()) {
            return
        }
        KLog.e("TAG4", "AdLoad.backInterstitialAd=${AdLoad.backInterstitialAd}")
        KLog.e("TAG4", "AdLoad.whetherShowBackAd=${AdLoad.whetherShowBackAd}")
        if (AdLoad.backInterstitialAd != null || AdLoad.whetherShowBackAd) {
            return
        }
        adRequest = AdRequest.Builder().build()

        AdLoad.loadBackScreenAdvertisement(this, adRequest)
    }

    /**
     * 插屏广告回调
     */
    private fun plugInAdvertisementCallback() {
        AdLoad.backInterstitialAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(LOG_TAG, "Ad was clicked.")
                    addClicksCount()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    val intent = Intent(this@ServiceListActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    KLog.d(LOG_TAG, "关闭back插屏")
                    AdLoad.backInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    AdLoad.backInterstitialAd = null
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    KLog.d(LOG_TAG, "back----show")
                    AdLoad.backInterstitialAd = null
                    AdLoad.whetherShowBackAd = false
                    addShowCount()
                }
            }
    }

    /**
     * 展示广告或返回
     */
    private fun displayAdvertisementOrReturn() {
        if (AdLoad.backInterstitialAd != null) {
            plugInAdvertisementCallback()
            AdLoad.backInterstitialAd?.show(this)
        } else {
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            displayAdvertisementOrReturn()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityCollector.removeActivity(this)
    }
}