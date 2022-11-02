package com.demo.blackbutton.ui.agreement

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.demo.blackbutton.R
import com.demo.blackbutton.constant.Constant
import com.demo.blackbutton.utils.DensityUtils
import com.demo.blackbutton.utils.StatusBarUtils


class AgreementWebView : AppCompatActivity() {
    private lateinit var webView: WebView

    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var ivRight: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_webview)
        supportActionBar?.hide()
        initView()
        initWebView()
    }

    private fun initView() {
        webView = findViewById(R.id.agreement_web)
        frameLayoutTitle = findViewById(R.id.bar_webview_list)
        frameLayoutTitle.setPadding(
            0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 50, 0, 0
        )
        blackTitle = frameLayoutTitle.findViewById(R.id.ivBack)
        imgTitle = frameLayoutTitle.findViewById(R.id.img_title)
        tvTitle = frameLayoutTitle.findViewById(R.id.tv_title)
        ivRight = frameLayoutTitle.findViewById(R.id.ivRight)
        imgTitle.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        ivRight.visibility = View.GONE
        tvTitle.text = getString(R.string.privacy_agreement)
        blackTitle.setImageResource(R.mipmap.ic_black)
        blackTitle.setOnClickListener {
            finish()
        }
    }

    private fun initWebView() {
        webView.loadUrl(Constant.PRIVACY_AGREEMENT)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            }

            override fun onPageFinished(view: WebView, url: String) {
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val hit = view.hitTestResult
                //hit.getExtra()为null或者hit.getType() == 0都表示即将加载的URL会发生重定向，需要做拦截处理
                if (TextUtils.isEmpty(hit.extra) || hit.type == 0) {
                }
                //加载的url是http/https协议地址
                return if (request.url.scheme!!.startsWith("http://") || request.url.scheme!!.startsWith(
                        "https://"
                    )
                ) {
                    view.loadUrl(request.url.toString())
                    false
                } else {
                    //加载的url是自定义协议地址
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))
                        this@AgreementWebView.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
            }
        }
    }

    //点击返回上一页面而不是退出浏览器
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        webView.clearHistory()
        (webView.parent as ViewGroup).removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}