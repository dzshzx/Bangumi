package soko.ekibun.bangumi.ui.main.fragment.calendar

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import kotlinx.android.synthetic.main.item_calendar.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.tinygrail.bean.OnAir
import soko.ekibun.bangumi.util.ResourceUtil
import java.util.*

class CalendarAdapter(data: MutableList<CalendarSection>? = null) :
        BaseSectionQuickAdapter<CalendarAdapter.CalendarSection, BaseViewHolder>
        (R.layout.item_calendar, R.layout.item_calendar, data) {
    override fun convert(helper: BaseViewHolder, item: CalendarSection) {
        helper.setText(R.id.item_title, if(item.t.subject.name_cn.isNullOrEmpty()) item.t.subject.name else item.t.subject.name_cn)
        helper.setText(R.id.item_name_jp, if(item.t.episode?.name_cn.isNullOrEmpty()) item.t.episode?.name?:"" else item.t.episode?.name_cn)
        helper.addOnClickListener(R.id.item_layout)
        Glide.with(helper.itemView)
                .load(item.t.subject.images?.common)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
        helper.itemView.item_time.text = ""

        val past = pastTime(item.date, item.time)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context, if(past) R.attr.colorPrimary else android.R.attr.textColorSecondary)
        helper.itemView.item_name_jp.setTextColor(color)
        helper.itemView.item_time.alpha = if(past) 0.6f else 1.0f
    }

    override fun convertHead(helper: BaseViewHolder, item: CalendarSection) {
        convert(helper, item)
        helper.setText(R.id.item_time, item.time)
    }

    class CalendarSection(isHeader: Boolean, subject: OnAir, var date: Int, var time: String) : SectionEntity<OnAir>(isHeader, ""){
        init{
            t = subject
        }
    }

    companion object {
        val weekJp = listOf("", "月", "火", "水", "木", "金", "土", "日")
        val weekSmall = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")

        fun pastTime(date: Int, time: String): Boolean{
            val match = Regex("""([0-9]*):([0-9]*)""").find(time)
            val hour=match?.groupValues?.get(1)?.toIntOrNull()?:0
            val minute=match?.groupValues?.get(2)?.toIntOrNull()?:0
            val cal = Calendar.getInstance()
            val nowInt = getCalendarInt(cal)
            val hourNow = cal.get(Calendar.HOUR_OF_DAY)
            val minuteNow = cal.get(Calendar.MINUTE)
            return nowInt > date || (nowInt == date && (hour<hourNow || (hour == hourNow && minute <= minuteNow)))
        }

        fun getIntCalendar(date: Int):Calendar{
            val cal = Calendar.getInstance()
            cal.set(date/10000, date/100%100-1, date%100)
            return cal
        }

        fun getCalendarInt(now: Calendar):Int{
            return now.get(Calendar.YEAR)*10000 + (now.get(Calendar.MONTH)+1) * 100 + now.get(Calendar.DATE)
        }

        fun getWeek(now: Calendar): Int{
            val isFirstSunday = now.firstDayOfWeek == Calendar.SUNDAY
            var weekDay = now.get(Calendar.DAY_OF_WEEK)
            if (isFirstSunday) {
                weekDay -= 1
                if (weekDay == 0) {
                    weekDay = 7
                }
            }
            return weekDay
        }

        fun getNowInt():Int{
            return getCalendarInt(Calendar.getInstance())
        }

        fun currentWeek():Int{
            val now = Calendar.getInstance()
            return getWeek(now)
        }
    }
}