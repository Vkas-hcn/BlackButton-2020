package com.demo.blackbutton.ui.servicelist


import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.demo.blackbutton.R
import com.demo.blackbutton.bean.ProfileBean
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.Utils.FlagConversion


class ServiceListAdapter(data: List<ProfileBean.SafeLocation>?) :
    BaseQuickAdapter<ProfileBean.SafeLocation, BaseViewHolder>(
        R.layout.item_service,
        data as MutableList<ProfileBean.SafeLocation>?
    ) {
    override fun convert(holder: BaseViewHolder, item: ProfileBean.SafeLocation) {
        if (item.bestServer == true) {
            holder.setText(R.id.tv_service_name, Constant.FASTER_SERVER)
            holder.setImageResource(R.id.img_service_icon, FlagConversion(Constant.FASTER_SERVER))
        } else {
            holder.setText(R.id.tv_service_name, item.bb_country + "-" + item.bb_city)
            holder.setImageResource(R.id.img_service_icon, FlagConversion(item.bb_country))
        }
        if (item.cheek_state == true) {
            holder.setImageResource(R.id.img_state, R.mipmap.rd_check)
        } else {
            holder.setImageResource(R.id.img_state, R.mipmap.rd_uncheck)
        }
    }
}

