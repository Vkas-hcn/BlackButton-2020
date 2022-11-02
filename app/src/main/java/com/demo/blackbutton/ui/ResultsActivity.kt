package com.demo.blackbutton.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.demo.blackbutton.R
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.DensityUtils
import com.demo.blackbutton.utils.StatusBarUtils

class ResultsActivity: AppCompatActivity() {
    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var ivRight: ImageView
    private var connectionStatus:Boolean =false
    private lateinit var imgConnectInfo: ImageView
    private lateinit var tvConnectInfo: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_results)
        supportActionBar?.hide()
        initView()
    }

    private fun initView() {
        frameLayoutTitle =findViewById(R.id.bar_results)
        frameLayoutTitle.setPadding(0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat())+50,0,0)
        blackTitle = findViewById(R.id.ivBack)
        imgTitle =findViewById(R.id.img_title)
        tvTitle = findViewById(R.id.tv_title)
        ivRight = findViewById(R.id.ivRight)
        imgConnectInfo= findViewById(R.id.img_connect_info)
        tvConnectInfo= findViewById(R.id.tv_connect_info)

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
        connectionStatus = intent.getBooleanExtra(Constant.CONNECTION_STATUS,false)
        if(connectionStatus){
            imgConnectInfo.setImageResource(R.mipmap.ic_connected)
            tvConnectInfo.text = getString(R.string.connected_succeeded)
        }else{
            imgConnectInfo.setImageResource(R.mipmap.ic_disconnected)
            tvConnectInfo.text = getString(R.string.disconnected_succeeded)
        }
    }
}