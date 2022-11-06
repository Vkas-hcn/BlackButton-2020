package com.demo.blackbutton.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.R
import com.demo.blackbutton.app.App
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.JsonUtil
import com.demo.blackbutton.utils.MmkvUtils
import com.demo.blackbutton.utils.NetworkPing.findTheBestIp
import com.demo.blackbutton.utils.StatusBarUtils
import com.demo.blackbutton.widget.HorizontalProgressView
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.bean.AroundFlowBean
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.tip.ToastUtils
import java.util.*
import com.demo.blackbutton.utils.ActivityCollector
import com.demo.blackbutton.utils.ActivityCollector.isActivityExist


/**
 * Startup Page
 */
class StartupActivity : AppCompatActivity(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private var whetherReturnCurrentPage: Boolean = false
    private lateinit var horizontalProgressView: HorizontalProgressView
    private var secondsRemaining: Long = 0L
    private val LOG_TAG = "BlackButton"
    private lateinit var countDownTimer: CountDownTimer

    // 绕流数据
    private lateinit var aroundFlowData: AroundFlowBean
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        setContentView(R.layout.activity_startup)
        supportActionBar?.hide()
        ActivityCollector.addActivity(this, javaClass)
        initLiveBus()
        initView()
        initParam()
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
        getFirebaseData()
    }

    private fun initLiveBus() {
        LiveEventBus
            .get(Constant.OPEN_AD, Boolean::class.java)
            .observeForever {
                if (isActivityExist(this.javaClass)) {
                    countDownTimer.cancel()
                    val application = application as? App
                    application?.showAdIfAvailable(
                        this@StartupActivity,
                        object : App.OnShowAdCompleteListener {
                            override fun onShowAdComplete() {
                                if (whetherReturnCurrentPage) {
                                    finish()
                                } else {
                                    jumpPage()
                                }
                            }
                        })
                }
            }
    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
        if (BuildConfig.DEBUG) {
            createTimer(10L)
            return
        } else {
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                ToastUtils.toast("fireBase Connection succeeded")
                MmkvUtils.set(Constant.AROUND_FLOW_DATA, auth.getString("aroundFlowData"))
                MmkvUtils.set(Constant.PROFILE_DATA, auth.getString("profileData"))
            }.addOnCompleteListener {
//                lifecycleScope.launch(Dispatchers.Main) {
//                    delay(2000L)
//                    jumpPage()
//                }
                createTimer(10L)
            }
        }
    }


    /**
     * Create the countdown timer, which counts down to zero and show the app open ad.
     *
     * @param seconds the number of seconds that the timer counts down from
     */
    private fun createTimer(seconds: Long) {
        val application = application as? App
        application?.AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"
        application?.AppOpenAdManager()?.appOpenAd.let {
            if (it != null) {
                it.show(this)
                return
            }
        }

        KLog.e("TAG", "AD_UNIT_ID")
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000 + 1
            }

            override fun onFinish() {
                secondsRemaining = 0

                // If the application is not an instance of MyApplication, log an error message and
                // start the MainActivity without showing the app open ad.
                if (application == null) {
                    Log.e(LOG_TAG, "Failed to cast application to MyApplication.")
                    jumpPage()
                    return
                }

                // Show the app open ad.
                application.showAdIfAvailable(
                    this@StartupActivity,
                    object : App.OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            if (whetherReturnCurrentPage) {
                                finish()
                            } else {
                                jumpPage()
                            }
                        }
                    })
            }
        }
        countDownTimer.start()
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        val intent = Intent(this@StartupActivity, MainActivity::class.java)
        val bestData = findTheBestIp()
        val dataJson = JsonUtil.toJson(bestData)
        MmkvUtils.set(Constant.BEST_SERVICE_DATA, dataJson)
        startActivity(intent)
        finish()
    }

    //    /**
//     * 回到当前页面
//     */
//    private fun backToTheCurrentPage() {
//        val intent = Intent(this@StartupActivity, MainActivity::class.java)
//        val bestData = findTheBestIp()
//        val dataJson = JsonUtil.toJson(bestData)
//        MmkvUtils.set(Constant.BEST_SERVICE_DATA, dataJson)
//        startActivity(intent)
//        finish()
//    }
    override fun onStop() {
        super.onStop()
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
}