package com.demo.blackbutton.bean

import androidx.annotation.Keep
import java.io.Serializable
@Keep
class ProfileBean {
    val safeLocation: MutableList<SafeLocation>? = null
    class SafeLocation {
        var bb_pwd: String? = null
        var bb_method: String? = null
        var bb_port: Int? = null
        var bb_country: String? = null
        var bb_city: String? = null
        var bb_ip: String? = null
        //是否选中
        var cheek_state: Boolean? = false
        //是否是最佳服务器
        var bestServer:Boolean? = false
    }
}
//@Keep
//data class ProfileBean(
//    val around_flow_mode: String? = null,
//    val black_list: List<String> = ArrayList(),
//    val white_list: List<String> = ArrayList(),
//    val safeLocation: MutableList<SafeLocation>? = null,
//
//) : Serializable
//@Keep
//class SafeLocation {
//    var bb_pwd: String? = null
//    var bb_method: String? = null
//    var bb_port: Int? = null
//    var bb_country: String? = null
//    var bb_city: String? = null
//    var bb_ip: String? = null
//    //是否选中
//    var cheek_state: Boolean? = false
//    //是否是最佳服务器
//    var bestServer:Boolean? = false
//}