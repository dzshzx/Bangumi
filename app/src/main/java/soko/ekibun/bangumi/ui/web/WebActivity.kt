package soko.ekibun.bangumi.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_web.*
import kotlinx.android.synthetic.main.appbar_layout.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.CollapsibleAppBarHelper
import soko.ekibun.bangumi.util.AppUtil
import java.net.URI

/**
 * 内置浏览器Activity
 */
class WebActivity : BaseActivity() {
    private val isAuth by lazy { intent.getBooleanExtra(IS_AUTH, false) }
    private val openUrl by lazy {
        (intent.getStringExtra(OPEN_URL) ?: {
            val url = intent.data?.toString()
            if (jumpUrl(this, url, "")?.let { startActivity(it) } != null) finish()
            url ?: ""
        }()).replace(Regex("""^https?://(bgm\.tv|bangumi\.tv|chii\.in)"""), Bangumi.SERVER)
    }

    private var filePathsCallback: ValueCallback<Array<Uri>>? = null

    private val mOnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        var view = webview
        while (view.childWebView != null)
            view = view.childWebView!!
        item_swipe.isEnabled = view.scrollY == 0
    }

    override fun onStop() {
        super.onStop()
        item_swipe.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
    }

    override fun onStart() {
        super.onStart()
        item_swipe.viewTreeObserver.addOnScrollChangedListener(mOnScrollChangedListener)
    }

    private val collapsibleAppBarHelper by lazy { CollapsibleAppBarHelper(app_bar as AppBarLayout) }
    override fun setTitle(title: CharSequence?) {
        collapsibleAppBarHelper.setTitle(title.toString(), supportActionBar?.subtitle?.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsibleAppBarHelper.updateStatus(1f)
        collapsibleAppBarHelper.appbarCollapsible(CollapsibleAppBarHelper.CollapseStatus.COLLAPSED)

        val paddingBottom = item_swipe.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            item_swipe.setPadding(
                item_swipe.paddingLeft,
                item_swipe.paddingTop,
                item_swipe.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }

        item_swipe.setOnRefreshListener {
            var view = webview
            while (view.childWebView != null)
                view = view.childWebView!!
            view.reload()
        }

        webview_progress.max = 100
        webview.onProgressChanged = { _: WebView, newProgress: Int ->
            webview_progress.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            item_swipe.isRefreshing = newProgress != 100
            webview_progress.progress = newProgress
        }

        if (!isAuth) {
            title = ""
            webview.loadUrl(openUrl)
            webview.onShowFileChooser =
                { valueCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams? ->
                    filePathsCallback = valueCallback
                    fileChooserParams?.createIntent()?.let {
                        startActivityForResult(it, FILECHOOSER_RESULTCODE)
                    }
                    true
                }
            webview.onReceivedTitle = { _: WebView?, title: String? ->
                if (title != null) this@WebActivity.title = title
            }
            webview.shouldOverrideUrlLoading = { view: WebView, request: WebResourceRequest ->
                val url = request.url.toString()
                if (jumpUrl(this, url, "")?.let { startActivity(it) } != null) {
                    true
                } else if (!url.startsWith("http") || !isBgmPage(url)) {
                    try {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, request.url), url))
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                } else {
                    val bgmUrl = url.replace(Regex("""^https?://(bgm\.tv|bangumi\.tv|chii\.in)"""), Bangumi.SERVER)
                    if (bgmUrl != url) {
                        view.post { view.loadUrl(bgmUrl) }
                        false
                    } else false
                }
            }
        } else {
            title = getString(R.string.login)
            val authUrl = "${Bangumi.SERVER}/login"
            webview.shouldOverrideUrlLoading = { _: WebView, request: WebResourceRequest ->
                val url = request.url.toString()
                if (url.trim('/') == Bangumi.SERVER) {
                    webview.loadUrl("about:blank")
                    setResult(Activity.RESULT_OK, Intent())
                    finish()
                    false
                } else if (url != authUrl && !url.contains("${Bangumi.SERVER}/FollowTheRabbit")) {
                    Log.v("redirect", url)
                    webview.loadUrl(authUrl)
                    true
                } else false
            }
            webview.loadUrl(authUrl)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!isAuth)
            menuInflater.inflate(R.menu.action_web, menu)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var view = webview
        while (view.childWebView != null)
            view = view.childWebView!!

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            when {
                view.canGoBack() -> view.goBack()
                view.parentWebView != null -> view.close()
                else -> {
                    if (isAuth) setResult(Activity.RESULT_CANCELED, null)
                    finish()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            val result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            filePathsCallback?.onReceiveValue(result)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var view = webview
        while (view.childWebView != null)
            view = view.childWebView!!
        when (item.itemId) {
            android.R.id.home -> {
                if (isAuth) setResult(Activity.RESULT_CANCELED, null)
                finish()
            }
            R.id.action_open -> AppUtil.openBrowser(this, view.url)
            R.id.action_share -> AppUtil.shareString(this, view.url)
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val REQUEST_AUTH = 1
        const val FILECHOOSER_RESULTCODE = 2
        const val IS_AUTH = "auth"
        const val OPEN_URL = "openUrl"
        /**
         * 启动验证
         */
        fun startActivityForAuth(context: Activity) {
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra(IS_AUTH, true)
            context.startActivityForResult(intent, REQUEST_AUTH)
        }

        fun startActivity(context: Context, page: String?) {
            context.startActivity(parseIntent(context, page))
        }

        fun launchUrl(context: Context, url: String?, openUrl: String) {
            context.startActivity(parseUrlIntent(context, url, openUrl))
        }

        private fun parseIntent(context: Context, page: String?): Intent? {
            if (page.isNullOrEmpty()) return null
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra(OPEN_URL, page)
            return intent
        }

        fun parseUrlIntent(context: Context, url: String?, openUrl: String): Intent? {
            return jumpUrl(context, url, openUrl) ?: {
                if (url?.startsWith("http") == false || !isBgmPage(url ?: "")) try {
                    Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(url)), url)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } else null
            }() ?: parseIntent(context, url)
        }

        private fun host(url: String) = try {
            URI.create(url).host
        } catch (e: Exception) {
            url
        }

        /**
         * 判断是否是bgm页面
         */
        fun isBgmPage(url: String): Boolean {
            val host = host(url)
            if (host.isNullOrEmpty()) return false
            bgmHosts.forEach {
                if (host.contains(it)) return true
            }
            return false
        }

        private fun getRakuen(page: String?): String? {
            val url = page?.split("#")?.get(0) ?: return page
            var regex = Regex("""/m/topic/([^/]*)/([0-9]*)$""")
            var model = regex.find(url)?.groupValues?.get(1) ?: ""
            var id = regex.find(url)?.groupValues?.get(2)?.toIntOrNull()
            if (id != null) return "${Bangumi.SERVER}/rakuen/topic/$model/$id"

            regex = Regex("""/([^/]*)/topic/([0-9]*)$""")
            model = regex.find(url)?.groupValues?.get(1) ?: ""
            id = regex.find(url)?.groupValues?.get(2)?.toIntOrNull()
            if (id != null) return "${Bangumi.SERVER}/rakuen/topic/$model/$id"

            return url
        }

        private val bgmHosts = arrayOf("bgm.tv", "bangumi.tv", "chii.in", "tinygrail.com")
        /**
         * 跳转到对应Activity
         */
        fun jumpUrl(context: Context, page: String?, openUrl: String): Intent? {
            val url = page?.split("#")?.get(0) // 去掉post
            val rakuen = getRakuen(url)
            if (url == null || url.isNullOrEmpty() ||
                !isBgmPage(url) ||                  // 不是bgm页面
                rakuen == getRakuen(openUrl) ||     // 不与openUrl相同
                host(url).contains("doujin") // 忽略天窗联盟
            ) return null
            val post = Regex("""#post_([0-9]+)$""").find(page)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            //blog
            val blogId = Regex("""/blog/(\d+)""").find(rakuen ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            if (blogId > 0)
                return TopicActivity.parseIntent(context, Topic("blog", blogId), post)
            //Topic
            val modelId = Regex("""/rakuen/topic/([^/]+)/(\d+)""").find(rakuen ?: "")?.groupValues
            if (modelId != null)
                return TopicActivity.parseIntent(context, Topic(modelId[1], modelId[2].toInt()), post)
            //Subject
            val regex = Regex("""/subject/([0-9]*)$""")
            val id = regex.find(url)?.groupValues?.get(1)?.toIntOrNull()
            if (id != null)
                return SubjectActivity.parseIntent(context, Subject(id))
            return null
        }
    }
}
