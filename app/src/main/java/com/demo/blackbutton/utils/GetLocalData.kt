package com.demo.blackbutton.utils

import com.demo.blackbutton.bean.AdsBean
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV

object GetLocalData {
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
                object : TypeToken<ProfileBean?>() {}.type
            )
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
}