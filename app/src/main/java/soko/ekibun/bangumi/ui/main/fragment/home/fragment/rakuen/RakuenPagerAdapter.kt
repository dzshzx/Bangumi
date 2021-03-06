package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.ui.topic.TopicActivity

/**
 * 超展开PagerAdapter
 */
class RakuenPagerAdapter(
    context: Context,
    val fragment: RakuenFragment,
    private val pager: androidx.viewpager.widget.ViewPager
) : androidx.viewpager.widget.PagerAdapter() {
    private val tabList = context.resources.getStringArray(R.array.topic_list)
    var selectedFilter = R.id.topic_filter_all

    init {
        pager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { /* no-op */
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { /* no-op */
            }

            override fun onPageSelected(position: Int) {
                loadTopicList(position)
            }
        })
    }

    @SuppressLint("UseSparseArrays")
    private val items = HashMap<Int, Pair<RakuenAdapter, androidx.swiperefreshlayout.widget.SwipeRefreshLayout>>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position) {
            val swipeRefreshLayout = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(container.context)
            val recyclerView = androidx.recyclerview.widget.RecyclerView(container.context)
            recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

            val adapter = RakuenAdapter()
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setOnItemClickListener { _, v, position ->
                TopicActivity.startActivity(v.context, adapter.data[position])
                //WebActivity.launchUrl(v.context, adapter.data[position].url)
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener { loadTopicList(position) }
            Pair(adapter, swipeRefreshLayout)
        }
        container.addView(item.second)
        if ((item.second.tag as? androidx.recyclerview.widget.RecyclerView)?.tag == null)
            loadTopicList(position)
        return item.second
    }

    /**
     * 重置
     */
    fun reset(position: Int) {
        val item = items[position] ?: return
        topicCall[position]?.cancel()
        item.first.isUseEmpty(false)
        item.first.setNewData(null)
    }

    @SuppressLint("UseSparseArrays")
    private var topicCall = HashMap<Int, Call<List<Topic>>>()

    /**
     * 加载帖子列表
     */
    fun loadTopicList(position: Int = pager.currentItem) {
        val item = items[position] ?: return
        item.first.isUseEmpty(false)
        topicCall[position]?.cancel()
        topicCall[position] = Topic.getList(if (position == 1) when (selectedFilter) {
            R.id.topic_filter_join -> "my_group"
            R.id.topic_filter_post -> "my_group&filter=topic"
            R.id.topic_filter_reply -> "my_group&filter=reply"
            else -> "group"
        } else listOf("", "group", "subject", "ep", "mono")[position])
        item.second.isRefreshing = true
        topicCall[position]?.enqueue(ApiHelper.buildCallback({
            item.first.isUseEmpty(true)
            item.first.setNewData(it)
            (item.second.tag as? androidx.recyclerview.widget.RecyclerView)?.tag = true
        }, {
            item.second.isRefreshing = false
        }))
    }

    override fun getPageTitle(pos: Int): CharSequence {
        return tabList[pos]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return tabList.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}