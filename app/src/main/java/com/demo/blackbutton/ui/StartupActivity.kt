package com.demo.blackbutton.ui

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.demo.blackbutton.BuildConfig
import com.demo.blackbutton.R
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.JsonUtil
import com.demo.blackbutton.utils.MmkvUtils
import com.demo.blackbutton.utils.NetworkPing.findTheBestIp
import com.demo.blackbutton.utils.StatusBarUtils
import com.demo.blackbutton.widget.HorizontalProgressView
import com.github.shadowsocks.bean.AroundFlowBean
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Startup Page
 */
class StartupActivity : AppCompatActivity(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private lateinit var horizontalProgressView: HorizontalProgressView

    // 绕流数据
    private lateinit var aroundFlowData: AroundFlowBean
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        setContentView(R.layout.activity_startup)
        supportActionBar?.hide()
        initView()
    }

    private fun initView() {
        horizontalProgressView = findViewById(R.id.pb_start)
        horizontalProgressView.setProgressViewUpdateListener(this)
        horizontalProgressView.startProgressAnimation()
        aroundFlowData = AroundFlowBean()
        getFirebaseData()
    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
//        if (BuildConfig.DEBUG) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(2000L)
                jumpPage()
            }
//            return
//        } else {
//            val auth = Firebase.remoteConfig
//            auth.fetchAndActivate().addOnSuccessListener {
//                ToastUtils.toast("fireBase Connection succeeded")
//                MmkvUtils.set(Constant.AROUND_FLOW_DATA, auth.getString("aroundFlowData"))
//                MmkvUtils.set(Constant.PROFILE_DATA, auth.getString("profileData"))
//            }.addOnCompleteListener {
//                lifecycleScope.launch(Dispatchers.Main) {
//                    delay(2000L)
//                    jumpPage()
//                }
//            }
//        }
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

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        horizontalProgressView.stopProgressAnimation()
        horizontalProgressView.setProgressViewUpdateListener(null)
    }

    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }
}