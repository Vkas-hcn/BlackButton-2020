package com.demo.blackbutton.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.demo.blackbutton.app.App
import com.example.testdemo.utils.KLog
import com.demo.blackbutton.utils.ActivityCollector
import com.demo.blackbutton.utils.ActivityCollector.getActivity


class ApplicationObserver : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> KLog.d("event", "onCreate")
            Lifecycle.Event.ON_START -> {
                val application = App()
                application.onMoveToForeground()
                KLog.d("event", "onStart")
            }
            Lifecycle.Event.ON_RESUME -> {
                val startupActivity: StartupActivity? = getActivity(
                    StartupActivity().javaClass
                ) as StartupActivity?

                KLog.d("event", "onResume=")
            }
            Lifecycle.Event.ON_PAUSE -> KLog.d("event", "onPause")
            Lifecycle.Event.ON_STOP -> KLog.d("event", "onStop")
            Lifecycle.Event.ON_DESTROY -> KLog.d("event", "onDestory")
            Lifecycle.Event.ON_ANY->KLog.d("event","onAny")
        }
    }
}