package com.demo.blackbutton.utils

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.app.ActivityManager
import android.content.Context
import com.demo.blackbutton.constant.Constant





object ActivityCollector {
    /**
     * 存放activity的列表
     */
    var activities: HashMap<Class<*>, Activity>? = LinkedHashMap()
    /**
     * 是否在前台
     */
    /**
     * 添加Activity
     *
     * @param activity
     */
    fun addActivity(activity: Activity, clz: Class<*>) {
        activities!![clz] = activity
    }

    /**
     * 判断一个Activity 是否存在
     *
     * @param clz
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun <T : Activity?> isActivityExist(clz: Class<T>): Boolean {
        val res: Boolean
        val activity: Activity? = getActivity(clz)
        res = if (activity == null) {
            false
        } else {
            !(activity.isFinishing() || activity.isDestroyed)
        }
        return res
    }

    /**
     * 获得指定activity
     *
     * @param clazz Activity 的类对象
     * @return
     */
    fun <T : Activity?> getActivity(clazz: Class<T>): Activity? {
        return activities!![clazz]
    }

    /**
     * 移除activity,代替finish
     *
     * @param activity
     */
    fun removeActivity(activity: Activity) {
        if (activities!!.containsValue(activity)) {
            activities!!.remove(activity.javaClass)
        }
    }

    /**
     *
     * 判断某开屏activity是否处于栈顶
     * @return  true在栈顶 false不在栈顶
     */
     fun isActivityTop( context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val name = manager.getRunningTasks(1)[0].topActivity!!.className
        return name == Constant.ADVERTISING_PACKAGE
    }
    fun getActivityName(activity: Activity): String {
        val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningTasks(1)[0].topActivity!!.className
    }

    /**
     * 获得栈中最顶层的Activity
     *
     * @param context
     * @return
     */
    fun getTopActivity(context: Context): String? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTaskInfos = manager.getRunningTasks(1)
        return if (runningTaskInfos != null) {
            runningTaskInfos[0].topActivity.toString()
        } else null
    }
}