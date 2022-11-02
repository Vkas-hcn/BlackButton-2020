package com.demo.blackbutton.utils

import android.content.Context
import android.net.ConnectivityManager
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

object NetworkPing {
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
    }
    private val job = Job()

    val scope = CoroutineScope(job)

    private val TAG = "PING-SJSX:"
    private var flag = true

    /**
     * pingIP
     */
    fun pingIP(ip: String?): String {
        val command = "ping -c 1 -W 1500 $ip"
        val proc = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        var time = ""
        when (proc.waitFor()) {
            0 -> {
                val result = StringBuilder()
                while (true) {
                    val line = reader.readLine() ?: break
                    result.append(line).append("\n")
                }
                result.toString().let {
                    time = it.substring(it.indexOf("time=") + 5)
                }
            }
            else -> {
                // 没有ping通
                // 网络权限,ip地址,命令有误 等
                time = "0"
            }
        }

        return time
    }

    /**
     * @param count ping这个ip几次 默认1次
     * @param outTime ping完之后多久超时 默认3秒
     * @param ip 默认ping百度的地址
     * @param needPingMesssage 是否需要ping的过程中的消息
     * @param pingMessage 返回需要ping的消息 如果想要消息则首先打开 [needPingMesssage]
     * @param pingSuccess 返回ping的状态
     */
    fun ping(
        count: Int = 1,
        outTime: Int = 1,
        ip: String = "www.baidu.com",
        whileTime: Long = 1500,
        pingMessage: (String) -> Unit = { _ -> }
    ) {
        scope.launch {
            val command = "ping -c $count -W $outTime $ip"
            while (true) {
                // 每[whileTime]s去 ping一次地址
                delay(whileTime)
                val proc = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                when (proc.waitFor()) {
                    0 -> {
                        // 等价 pingSuccess(true)
                        val result = StringBuilder()
                        while (true) {
                            val line = reader.readLine() ?: break
                            result.append(line).append("\n")
                        }
                        result.toString().let {
                            pingMessage.invoke(it.substring(it.indexOf("time=") + 5))
                            pingCancle()
                        }

                    }
                    else -> {
                        // 只要是没有ping通,肯定是有原因
                        // 网络权限,ip地址,命令有误 等
//                        pingSuccess.invoke(false)
                        pingMessage.invoke("-1")
                        pingCancle()
                    }
                }
            }

        }
    }

    // 关闭当前的协程
    fun pingCancle() {
        scope.cancel()
    }

    /**
     * 找到最佳ip
     */
    fun findTheBestIp(): ProfileBean.SafeLocation {
        val profileBean: ProfileBean =
            if (Utils.isNullOrEmpty(mmkv.decodeString(Constant.PROFILE_DATA))) {
                getProfileJsonData()
            } else {
                JsonUtil.fromJson(
                    mmkv.decodeString(Constant.PROFILE_DATA),
                    object : TypeToken<ProfileBean?>() {}.type
                )
            }
        profileBean.safeLocation?.shuffled()?.take(1)?.forEach {
            it.bestServer = true
            return it
        }
        profileBean.safeLocation!![0].bestServer = true
        return profileBean.safeLocation[0]
    }

    /**
     * @return 解析json文件
     */
    private fun getProfileJsonData(): ProfileBean {
        return JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert("serviceJson.json"),
            object : TypeToken<ProfileBean?>() {}.type
        )
    }

    @Throws(Exception::class)
    fun ping1(ipAddress: String): String? {
        var line: String? = null
        try {
            val pro = Runtime.getRuntime().exec("ping -c 1 -w 1 $ipAddress")
            val buf = BufferedReader(
                InputStreamReader(
                    pro.inputStream
                )
            )
            while (buf.readLine().also { line = it } != null) {
                return line
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
        return ""
    }

    /**
     * check NetworkAvailable
     * @param context
     * @return
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        val manager = context!!.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val info = manager.activeNetworkInfo
        return !(null == info || !info.isAvailable)
    }
}