package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 收藏对话框
 */
class EditSubjectDialog(context: Context) : BaseDialog(context, R.layout.dialog_edit_subject) {
    override val title: String = context.getString(R.string.dialog_subject_edit_title)

    companion object {
        /**
         * 显示对话框
         */
        fun showDialog(context: Context, subject: Subject, status: Collection, callback: () -> Unit) {
            val dialog = EditSubjectDialog(context)
            dialog.subject = subject
            dialog.collection = status
            dialog.setOnDismissListener { callback() }
            dialog.show()
        }
    }

    lateinit var subject: Subject
    lateinit var collection: Collection
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View) {
        val collectionStatusNames = context.resources.getStringArray(Collection.getStatusNamesRes(subject.type))
        view.radio_wish.text = collectionStatusNames[0]
        view.radio_collect.text = collectionStatusNames[1]
        view.radio_do.text = collectionStatusNames[2]
        view.radio_hold.text = collectionStatusNames[3]
        view.radio_dropped.text = collectionStatusNames[4]

        val selectMap = mapOf(
            Collection.STATUS_WISH to R.id.radio_wish,
            Collection.STATUS_COLLECT to R.id.radio_collect,
                Collection.STATUS_DO to R.id.radio_do,
                Collection.STATUS_ON_HOLD to R.id.radio_hold,
                Collection.STATUS_DROPPED to R.id.radio_dropped)
        view.findViewById<RadioButton>(selectMap[collection.status] ?: R.id.radio_collect)?.isChecked = true
        view.item_rating.rating = collection.rating.toFloat()
        view.item_tags.setText(collection.tag?.joinToString(" "))
        view.item_comment.setText(collection.comment)

        val hasTag = { tag: String ->
            view.item_tags.editableText.split(" ").contains(tag)
        }
        val myTagAdapter = TagAdapter(null, hasTag)
        myTagAdapter.setNewData(collection.myTag?.map { it to 0 }?.toMutableList())
        view.item_my_tag_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        view.item_my_tag_list.adapter = myTagAdapter
        myTagAdapter.setOnItemClickListener { _, _, position ->
            val tag = myTagAdapter.data[position].first
            if (!hasTag(tag)) view.item_tags.setText("${view.item_tags.editableText.trim()} $tag".trim())
            else view.item_tags.setText(view.item_tags.editableText.trim().toString().replace(tag, "").replace("  ", " "))
        }
        val userTagAdapter = TagAdapter(null, hasTag)
        userTagAdapter.setNewData(subject.tags?.toMutableList())
        view.item_user_tag_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        view.item_user_tag_list.adapter = userTagAdapter
        userTagAdapter.setOnItemClickListener { _, _, position ->
            val tag = userTagAdapter.data[position].first
            if (!hasTag(tag)) view.item_tags.setText("${view.item_tags.editableText.trim()} $tag".trim())
            else view.item_tags.setText(view.item_tags.editableText.trim().toString().replace(tag, "").replace("  ", " "))
        }
        view.item_tags.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                myTagAdapter.notifyDataSetChanged()
                userTagAdapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { /* no-op */
            }
        })

        view.item_remove.visibility = if (HttpUtil.formhash.isEmpty()) View.INVISIBLE else View.VISIBLE
        view.item_remove.setOnClickListener {
            AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
                    .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                        Collection.remove(subject).enqueue(ApiHelper.buildCallback({
                            if (it) subject.collect = Collection()
                            dismiss()
                        }, {}))
                    }.show()
        }
        view.item_submit.setOnClickListener {
            Collection.updateStatus(subject, Collection(
                    status = selectMap.toList().first { it.second == view.item_subject_status.checkedRadioButtonId }.first,
                    rating = view.item_rating.rating.toInt(),
                    comment = view.item_comment.text.toString(),
                    private = if (view.item_private.isChecked) 1 else 0,
                    tag = view.item_tags.editableText.split(" ")
            )).enqueue(ApiHelper.buildCallback({
                subject.collect = it
                dismiss()
            }, {}))
        }

        val paddingBottom = view.item_buttons.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.setPadding(
                view.item_buttons.paddingLeft,
                view.item_buttons.paddingTop,
                view.item_buttons.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
    }
}