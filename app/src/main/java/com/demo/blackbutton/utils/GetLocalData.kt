package com.demo.blackbutton.utils

import com.demo.blackbutton.bean.AdsBean
import com.demo.blackbutton.bean.AdsID
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.example.testdemo.utils.KLog
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV

object GetLocalData {
    private val LOG_TAG = "ad-log"
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("BlackButton", MMKV.MULTI_PROCESS_MODE)
    }

    /**
     * 获取广告数据
     */
    fun getLocalAdData(): AdsBean {
        return if (Utils.isNullOrEmpty(mmkv.decodeString(Constant.ADVERTISING_DATA))) {
            getAdJsonData()
        } else {
            JsonUtil.fromJson(
                mmkv.decodeString(Constant.ADVERTISING_DATA),
                object : TypeToken<AdsBean?>() {}.type
            )
        }
    }


    /**
     *权重排序
     */
    fun weightSorting(): AdsBean {
        val adBean = AdsBean()
        val blackOpen = getLocalAdData().black_open.sortedWith(compareByDescending { it.bb_w })
        val blackHome = getLocalAdData().black_home.sortedWith(compareByDescending { it.bb_w })
        val blackResult = getLocalAdData().black_result.sortedWith(compareByDescending { it.bb_w })
        val blackConnect =
            getLocalAdData().black_connect.sortedWith(compareByDescending { it.bb_w })
        val blackBack = getLocalAdData().black_back.sortedWith(compareByDescending { it.bb_w })
        adBean.black_open = blackOpen as MutableList<AdsID>
        adBean.black_home = blackHome as MutableList<AdsID>
        adBean.black_result = blackResult as MutableList<AdsID>
        adBean.black_connect = blackConnect as MutableList<AdsID>
        adBean.black_back = blackBack as MutableList<AdsID>
        return adBean
    }

    /**
     * 获取广告id
     */
    fun getAdId(ads: List<AdsID>, adsIndex: Int): String {
        return if (adsIndex < ads.size) {
            ads[adsIndex].bb_id.toString()
        } else {
            ads[0].bb_id.toString()
        }
    }

    /**
     * @return 解析json文件
     */
    private fun getAdJsonData(): AdsBean {
        return JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert("ad.json"),
            object : TypeToken<AdsBean?>() {}.type
        )
    }

    /**
     * 是否超出广告上限
     */
    fun isAdExceedLimit(): Boolean {
        val bean = getLocalAdData()
        val clicksCount = mmkv.decodeInt(Constant.CLICKS_COUNT, 0)
        val showCount = mmkv.decodeInt(Constant.SHOW_COUNT, 0)
        KLog.e("TAG","clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e("TAG","clicksCount > bean.bb_c_num!!=${clicksCount > bean.bb_c_num!!}")

        if (clicksCount > bean.bb_c_num!! || showCount > bean.bb_s_num!!) {
            return true
        }
        return false
    }

    /**
     * 增加点击次数
     */
    fun addClicksCount() {
        var clicksCount = mmkv.decodeInt(Constant.CLICKS_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_COUNT, clicksCount)
        KLog.d(LOG_TAG, "addClicksCount=${clicksCount}")
    }

    /**
     * 增加展示次数
     */
    fun addShowCount() {
        var showCount = mmkv.decodeInt(Constant.SHOW_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_COUNT, showCount)
    }

}