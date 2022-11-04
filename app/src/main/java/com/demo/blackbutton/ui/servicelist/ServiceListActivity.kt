package com.demo.blackbutton.ui.servicelist

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.blackbutton.R
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.DensityUtils.px2dp
import com.demo.blackbutton.utils.JsonUtil
import com.demo.blackbutton.utils.NetworkPing
import com.demo.blackbutton.utils.ResourceUtils.readStringFromAssert
import com.demo.blackbutton.utils.StatusBarUtils
import com.demo.blackbutton.utils.Utils.addTheBestRoute
import com.demo.blackbutton.utils.Utils.isNullOrEmpty
import com.example.testdemo.utils.KLog
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV


class ServiceListActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_service_list)
        initParam()
        initRecyclerView()
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
            safeLocation[0].cheek_state =false
            checkSafeLocation.cheek_state =true
        }
        serviceListAdapter = ServiceListAdapter(safeLocation)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = serviceListAdapter
        clickEvent()
    }
    /**
     * 获取选中数据
     */
    private fun getSelectedData(position:Int){
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
            finish()
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
                LiveEventBus.get<ProfileBean.SafeLocation>(Constant.SERVER_INFORMATION)
                    .post(checkSafeLocation)
                finish()
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
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResumeJumpPage() {
        KLog.e("TAG","ON_RESUME----->")
    }
}