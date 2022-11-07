package com.demo.blackbutton.bean

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class AdsBean(
    val open_ad: List<AdsID> = ArrayList(),
    val native_ad: List<AdsID> = ArrayList(),
    val screen_ad: List<AdsID> = ArrayList(),
    ) : Serializable

@Keep
data class AdsID(
    val adUnitID: String? = null,
    val weight: String? = null,
) : Serializable