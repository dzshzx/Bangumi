package soko.ekibun.bangumi.ui.subject

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.xiaofeng.flowlayoutmanager.FlowLayoutManager
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.android.synthetic.main.activity_subject.view.*
import kotlinx.android.synthetic.main.subject_blog.*
import kotlinx.android.synthetic.main.subject_buttons.*
import kotlinx.android.synthetic.main.subject_character.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import kotlinx.android.synthetic.main.subject_episode.view.*
import kotlinx.android.synthetic.main.subject_topic.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.PlayerBridge

class SubjectView(private val context: SubjectActivity) {

    val episodeAdapter = SmallEpisodeAdapter()
    val episodeDetailAdapter = EpisodeAdapter()
    val linkedSubjectsAdapter = LinkedSubjectAdapter()
    val recommendSubjectsAdapter = LinkedSubjectAdapter()
    val characterAdapter = CharacterAdapter()
    val tagAdapter = TagAdapter()
    val topicAdapter = TopicAdapter()
    val blogAdapter = BlogAdapter()
    val sitesAdapter = SitesAdapter()
    val commentAdapter = CommentAdapter()
    val seasonAdapter = SeasonAdapter()
    val seasonLayoutManager = LinearLayoutManager(context)

    val detail: LinearLayout = context.subject_detail


    var appBarOffset = -1
    val scroll2Top = {
        if (appBarOffset != 0 || if (context.episode_detail_list.visibility == View.VISIBLE) context.episode_detail_list.canScrollVertically(-1) else context.comment_list.canScrollVertically(-1)) {
            context.app_bar.setExpanded(true, true)
            (context.comment_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            (context.episode_detail_list.layoutManager as StaggeredGridLayoutManager).scrollToPositionWithOffset(0, 0)
            true
        } else false
    }

    init {
        val marginEnd = (context.item_buttons.layoutParams as CollapsingToolbarLayout.LayoutParams).marginEnd
        (context.title_expand.layoutParams as ConstraintLayout.LayoutParams).marginEnd = 3 * marginEnd

        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (appBarOffset == verticalOffset) return@OnOffsetChangedListener
            val ratio = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            appBarOffset = verticalOffset
            context.item_scrim.alpha = ratio
            context.item_subject.alpha = 1 - ratio
            context.item_buttons.translationY = -(context.toolbar.height - context.item_buttons.height * 9 / 8) * ratio / 2 - context.item_buttons.height / 16
            context.title_collapse.alpha = 1 - (1 - ratio) * (1 - ratio) * (1 - ratio)
            context.title_expand.alpha = 1 - ratio
            context.item_buttons.translationX = -2.2f * marginEnd * ratio
            context.app_bar.elevation = Math.max(0f, 12 * (ratio - 0.95f) / 0.05f)

            context.episode_detail_list.invalidate()
        })

        context.season_list.adapter = seasonAdapter
        seasonLayoutManager.orientation = RecyclerView.HORIZONTAL
        context.season_list.layoutManager = seasonLayoutManager
        context.season_list.isNestedScrollingEnabled = false

        context.episode_list.adapter = episodeAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        context.episode_list.layoutManager = layoutManager
        context.episode_list.isNestedScrollingEnabled = false
        val swipeTouchListener = View.OnTouchListener { v, _ ->
            if ((v as? RecyclerView)?.canScrollHorizontally(1) == true || (v as? RecyclerView)?.canScrollHorizontally(-1) == true)
                context.shouldCancelActivity = false
            false
        }
        context.episode_list.setOnTouchListener(swipeTouchListener)
        context.season_list.setOnTouchListener(swipeTouchListener)
        context.commend_list.setOnTouchListener(swipeTouchListener)
        context.linked_list.setOnTouchListener(swipeTouchListener)
        context.character_list.setOnTouchListener(swipeTouchListener)
        context.tag_list.setOnTouchListener(swipeTouchListener)

        val touchListener = episodeDetailAdapter.setUpWithRecyclerView(context.episode_detail_list)
        touchListener.nestScrollDistance = {
            context.app_bar.totalScrollRange + appBarOffset
        }
        context.episode_detail_list.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        context.episode_detail_list.nestedScrollDistance = {
            -appBarOffset
        }
        context.episode_detail_list.nestedScrollRange = {
            context.app_bar.totalScrollRange
        }

        context.item_close.setOnClickListener {
            closeEpisodeDetail()
        }
        context.episode_detail.setOnClickListener {
            showEpisodeDetail(true)
        }

        context.linked_list.adapter = linkedSubjectsAdapter
        val subjectLayoutManager = LinearLayoutManager(context)
        subjectLayoutManager.orientation = RecyclerView.HORIZONTAL
        context.linked_list.layoutManager = subjectLayoutManager
        context.linked_list.isNestedScrollingEnabled = false


        context.commend_list.adapter = recommendSubjectsAdapter
        val subjectLayoutManager2 = LinearLayoutManager(context)
        subjectLayoutManager2.orientation = RecyclerView.HORIZONTAL
        context.commend_list.layoutManager = subjectLayoutManager2
        context.commend_list.isNestedScrollingEnabled = false

        context.character_list.adapter = characterAdapter
        val characterLayoutManager = LinearLayoutManager(context)
        characterLayoutManager.orientation = RecyclerView.HORIZONTAL
        context.character_list.layoutManager = characterLayoutManager
        context.character_list.isNestedScrollingEnabled = false

        context.topic_list.adapter = topicAdapter
        context.topic_list.layoutManager = LinearLayoutManager(context)
        context.topic_list.isNestedScrollingEnabled = false

        context.blog_list.adapter = blogAdapter
        context.blog_list.layoutManager = LinearLayoutManager(context)
        context.blog_list.isNestedScrollingEnabled = false

        context.site_list.adapter = sitesAdapter
        val flowLayoutManager = FlowLayoutManager()
        flowLayoutManager.isAutoMeasureEnabled = true
        context.site_list.layoutManager = flowLayoutManager
        context.site_list.isNestedScrollingEnabled = false

        context.tag_list.adapter = tagAdapter
        val tagLayoutManager = LinearLayoutManager(context)
        tagLayoutManager.orientation = RecyclerView.HORIZONTAL
        context.tag_list.layoutManager = tagLayoutManager
        context.tag_list.isNestedScrollingEnabled = false

        context.comment_list.adapter = commentAdapter
        context.comment_list.layoutManager = LinearLayoutManager(context)

        detail.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        context.root_layout.removeView(detail)
        commentAdapter.setHeaderView(detail)
    }

    fun closeEpisodeDetail() {
        val eps = episodeDetailAdapter.data.filter { it.isSelected }
        if (eps.isEmpty())
            showEpisodeDetail(false)
        else {
            for (ep in eps) ep.isSelected = false
            episodeDetailAdapter.updateSelection()
            episodeDetailAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: Subject) {
        if (context.isDestroyed) return

        context.title_collapse.text = subject.displayName
        context.title_expand.text = context.title_collapse.text
        context.title_expand.post {
            val layoutParams = (context.title_collapse.layoutParams as ConstraintLayout.LayoutParams)
            layoutParams.marginEnd = 3 * (context.item_buttons.layoutParams as CollapsingToolbarLayout.LayoutParams).marginEnd + context.item_buttons.width
            context.title_collapse.layoutParams = layoutParams
            (context.item_subject.layoutParams as CollapsingToolbarLayout.LayoutParams).topMargin = context.toolbar_container.height
        }
        context.item_info.text = (if (subject.category.isNullOrEmpty()) context.getString(Subject.getTypeRes(subject.type)) else subject.category) + " · " + subject.name
        context.item_subject_title.visibility = View.GONE
        val saleDate = subject.infobox?.firstOrNull { it.first in arrayOf("发售日期", "发售日", "发行日期") }
        val artist = subject.infobox?.firstOrNull { it.first.substringBefore(" ") in arrayOf("动画制作", "作者", "开发", "游戏制作", "艺术家") }
                ?: subject.infobox?.firstOrNull { it.first.substringBefore(" ") in arrayOf("导演", "发行") }
        context.item_air_time.text = if (saleDate != null) "${saleDate.first}：${Jsoup.parse(saleDate.second).body().text()}"
        else "放送日期：${subject.air_date
                ?: ""} ${if (artist != null) CalendarAdapter.weekList[subject.air_weekday] else ""}"
        context.item_air_week.text = if (artist != null) "${artist.first}：${Jsoup.parse(artist.second).body().text()}" else "更新时间：${CalendarAdapter.weekList[subject.air_weekday]}"
        detail.item_detail.text = if (subject.summary.isNullOrEmpty()) subject.name else subject.summary

        context.item_play.visibility = if ((PlayerBridge.checkActivity(context) || PlayerBridge.checkActivity(context)) && subject.type in listOf(Subject.TYPE_ANIME, Subject.TYPE_REAL)) View.VISIBLE else View.GONE

        subject.rating?.let {
            context.detail_score.text = if (it.score == 0f) "-" else String.format("%.1f", it.score)
            context.detail_friend_score.text = if (it.friend_score == 0f) "-" else String.format("%.1f", it.friend_score)
            context.detail_score_count.text = "×${if (it.total > 1000) "${it.total / 1000}k" else it.total.toString()}"
            context.item_friend_score_label.text = context.getString(R.string.friend_score)
        }
        GlideUtil.with(context.item_cover)
                ?.load(subject.images?.getImage(context))
                ?.apply(RequestOptions.placeholderOf(context.item_cover.drawable))
                ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                ?.into(context.item_cover)
        context.item_cover.setOnClickListener {
            val popWindow = PopupWindow(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
            val photoView = DragPhotoView(it.context)
            popWindow.contentView = photoView
            GlideUtil.with(photoView)?.load(subject.images?.large)
                    ?.apply(RequestOptions.placeholderOf(context.item_cover.drawable))
                    ?.into(photoView.glideTarget)
            photoView.mTapListener = {
                popWindow.dismiss()
            }
            photoView.mExitListener = {
                popWindow.dismiss()
            }
            photoView.mLongClickListener = {
                val systemUiVisibility = popWindow.contentView.systemUiVisibility
                val dialog = AlertDialog.Builder(context)
                        .setItems(arrayOf(context.getString(R.string.share)))
                        { _, _ ->
                            AppUtil.shareDrawable(context, photoView.drawable)
                        }.setOnDismissListener {
                            popWindow.contentView.systemUiVisibility = systemUiVisibility
                        }.create()
                dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                dialog.show()
            }
            popWindow.isClippingEnabled = false
            popWindow.animationStyle = R.style.AppTheme_FadeInOut
            popWindow.showAtLocation(it, Gravity.CENTER, 0, 0)
            popWindow.contentView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        GlideUtil.with(context.item_cover_blur)
                ?.load(subject.images?.getImage(context))
                ?.apply(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                ?.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                ?.into(context.item_cover_blur)
        subject.eps?.let { subject.eps = updateEpisode(it) }
        detail.item_topics.visibility = if (subject.topic?.isNotEmpty() == true) View.VISIBLE else View.GONE
        topicAdapter.setNewData(subject.topic)
        detail.item_blogs.visibility = if (subject.blog?.isNotEmpty() == true) View.VISIBLE else View.GONE
        blogAdapter.setNewData(subject.blog)
        detail.item_character.visibility = if (subject.crt?.isNotEmpty() == true) View.VISIBLE else View.GONE
        characterAdapter.setNewData(subject.crt)
        detail.item_linked.visibility = if (subject.linked?.isNotEmpty() == true) View.VISIBLE else View.GONE
        linkedSubjectsAdapter.setNewData(subject.linked)
        detail.item_commend.visibility = if (subject.recommend?.isNotEmpty() == true) View.VISIBLE else View.GONE
        recommendSubjectsAdapter.setNewData(subject.recommend)

        tagAdapter.setNewData(subject.tags)
        tagAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, "${Bangumi.SERVER}/${Subject.getTypeName(subject.type)}/tag/${tagAdapter.data[position].first}")
        }

        context.item_subject.setOnClickListener {
            if (scroll2Top()) return@setOnClickListener
            InfoboxDialog.showDialog(context, subject)
        }
        detail.item_detail.setOnClickListener {
            InfoboxDialog.showDialog(context, subject)
        }

        detail.item_progress.visibility = if (HttpUtil.formhash.isNotEmpty() && subject.collect?.status == Collection.TYPE_DO && subject.type in listOf(Subject.TYPE_ANIME, Subject.TYPE_REAL, Subject.TYPE_BOOK)) View.VISIBLE else View.GONE
        detail.item_progress_info.text = context.getString(R.string.phrase_progress,
                (if (subject.vol_count != 0) context.getString(R.string.parse_sort_vol, "${subject.vol_status}${if (subject.vol_count == 0) "" else "/${subject.vol_count}"}") + " " else "") +
                        context.getString(R.string.parse_sort_ep, "${subject.ep_status}${if (subject.eps_count == 0) "" else "/${subject.eps_count}"}"))
    }

    private var subjectEpisode: List<Episode> = ArrayList()
    fun updateEpisode(episodes: List<Episode>): List<Episode> {
        if (episodes.none { it.id != 0 }) return subjectEpisode
        val mainEps = episodes.filter { it.type == Episode.TYPE_MAIN || it.type == Episode.TYPE_MUSIC }
        val eps = mainEps.filter { it.status in listOf(Episode.STATUS_AIR) }
        detail.episode_detail.text = if (eps.size == mainEps.size) context.getString(R.string.phrase_full_eps, eps.size) else
            eps.lastOrNull()?.parseSort(context)?.let { context.getString(R.string.parse_update_to, it) }
                    ?: context.getString(R.string.hint_air_nothing)

        subjectEpisode = episodes.plus(subjectEpisode).distinctBy { it.id }.sortedBy { it.sort }
        val maps = LinkedHashMap<String, List<Episode>>()
        subjectEpisode.forEach {
            val key = it.category ?: context.getString(Episode.getTypeRes(it.type))
            maps[key] = (maps[key] ?: ArrayList()).plus(it)
        }
        val lastEpisodeSize = episodeDetailAdapter.data.size
        episodeAdapter.setNewData(null)
        episodeDetailAdapter.setNewData(null)
        maps.forEach {
            episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(true, it.key))
            it.value.forEach { ep ->
                if (ep.status in listOf(Episode.STATUS_AIR))
                    episodeAdapter.addData(ep)
                episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(ep))
            }
        }
        if (!scrolled || episodeDetailAdapter.data.size != lastEpisodeSize) {
            scrolled = true

            var lastView = 0
            episodeAdapter.data.forEachIndexed { index, episode ->
                if (episode.progress != null)
                    lastView = index
            }
            val layoutManager = (detail.episode_list.layoutManager as LinearLayoutManager)
            layoutManager.scrollToPositionWithOffset(lastView, 0)
            layoutManager.stackFromEnd = false
        }
        detail.item_episodes.visibility = if (episodeDetailAdapter.data.isEmpty()) View.GONE else View.VISIBLE

        return subjectEpisode
    }

    private var scrolled = false

    private fun showEpisodeDetail(show: Boolean) {
        context.episode_detail_list_header.visibility = if (show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list_header.animation = AnimationUtils.loadAnimation(context, if (show) R.anim.move_in else R.anim.move_out)
        context.episode_detail_list.visibility = if (show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list.animation = AnimationUtils.loadAnimation(context, if (show) R.anim.move_in else R.anim.move_out)
    }
}