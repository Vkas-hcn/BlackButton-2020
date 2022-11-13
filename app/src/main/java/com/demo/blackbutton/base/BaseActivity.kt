package com.demo.blackbutton.base

import android.os.Bundle
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.demo.blackbutton.utils.ActivityCollector
import com.example.testdemo.utils.KLog

open class BaseActivity : AppCompatActivity() {
    var count = 0
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCollector.addActivity(this, javaClass)
    }

    override fun onStart() {
        count++
        super.onStart()
    }

    override fun onStop() {
        count--
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}