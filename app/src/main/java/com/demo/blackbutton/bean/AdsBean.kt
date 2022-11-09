package com.demo.blackbutton.bean

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class AdsBean(
    var black_open: MutableList<AdsID> = ArrayList(),
    var black_home: MutableList<AdsID> = ArrayList(),
    var black_result: MutableList<AdsID> = ArrayList(),
    var black_connect: MutableList<AdsID> = ArrayList(),
    var black_back: MutableList<AdsID> = ArrayList(),
    var bb_s_num: Int? = null,
    var bb_c_num: Int? = null,
) : Serializable

@Keep
data class AdsID(
    val bb_t: String? = null,
    val bb_pl: String? = null,
    val bb_id: String? = null,
    val bb_w: Int? = null,
) : Serializable