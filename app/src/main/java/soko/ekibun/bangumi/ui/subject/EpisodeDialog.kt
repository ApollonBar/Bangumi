package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import kotlinx.android.synthetic.main.base_dialog.view.*
import kotlinx.android.synthetic.main.dialog_epsode.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 剧集对话框
 * @property eps List<Episode>
 * @property episode Episode?
 * @property callback Function2<[@kotlin.ParameterName] List<Episode>, [@kotlin.ParameterName] String, Unit>?
 * @property adapter SitesAdapter
 * @property info OnAirInfo?
 * @property title String
 */
class EpisodeDialog : BaseDialog(R.layout.base_dialog) {
    companion object {
        const val WATCH_TO = "watch_to"

        /**
         * 显示对话框
         * @param fragmentManager FragmentManager
         * @param episode Episode
         * @param eps List<Episode>
         * @param onAirInfo OnAirInfo?
         * @param callback Function2<[@kotlin.ParameterName] List<Episode>, [@kotlin.ParameterName] String, Unit>
         * @return EpisodeDialog
         */
        fun showDialog(
            fragmentManager: FragmentManager,
            episode: Episode,
            eps: List<Episode>,
            onAirInfo: OnAirInfo?,
            callback: (eps: List<Episode>, status: String, onComplete: (Boolean) -> Unit) -> Unit
        ): EpisodeDialog {
            val dialog = EpisodeDialog()
            dialog.eps = eps
            dialog.episode = episode
            dialog.info = onAirInfo
            dialog.callback = callback
            dialog.show(fragmentManager, "episode")
            return dialog
        }
    }

    var eps: List<Episode> = ArrayList()
    var episode: Episode? = null
    var callback: ((eps: List<Episode>, status: String, onComplete: (Boolean) -> Unit) -> Unit)? = null
    val adapter = SitesAdapter()
    var info: OnAirInfo? = null
        set(value) {
            field = value
            adapter.setNewInstance(value?.eps?.find { it.id == episode?.id }?.sites?.toMutableList())
        }
    override val title: String get() = episode?.parseSort() + " " + if (episode?.name_cn.isNullOrEmpty()) episode?.name else episode?.name_cn

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View) {
        view.dialog_title.setOnClickListener {
            WebActivity.launchUrl(view.context, "${Bangumi.SERVER}/m/topic/ep/${episode?.id}", "")
        }
        LayoutInflater.from(view.context).inflate(R.layout.dialog_epsode, view.layout_content)
        val episode = episode ?: return
        view.item_episode_desc.text = (if (episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if (episode.airdate.isNullOrEmpty()) "" else view.context.getString(
                    R.string.phrase_air_date,
                    episode.airdate
                ) + "\n") +
                (if (episode.duration.isNullOrEmpty()) "" else view.context.getString(
                    R.string.phrase_duration,
                    episode.duration
                ) + "\n") +
                view.context.getString(R.string.phrase_comment, episode.comment)
        val emptyTextView = TextView(context)
        val dp4 = (view.context.resources.displayMetrics.density * 4 + 0.5f).toInt()
        emptyTextView.setPadding(dp4, dp4, dp4, dp4)
        emptyTextView.setText(R.string.hint_no_play_source)
        adapter.setEmptyView(emptyTextView)
        adapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(view.context, adapter.data[position].url(), "")
        }
        view.item_site_list.adapter = adapter
        view.post { info = info }
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        linearLayoutManager.orientation = RecyclerView.HORIZONTAL
        view.item_site_list.layoutManager = linearLayoutManager
        var currentStatus = when (episode.progress) {
            Episode.PROGRESS_QUEUE -> R.id.radio_queue
            Episode.PROGRESS_WATCH -> R.id.radio_watch
            Episode.PROGRESS_DROP -> R.id.radio_drop
            else -> R.id.radio_remove
        }
        view.item_episode_status.check(currentStatus)

        if (episode.type != Episode.TYPE_MUSIC) {
            view.item_episode_status.setOnCheckedChangeListener { v, checkedId ->
                if (currentStatus == checkedId) return@setOnCheckedChangeListener
                val circularProgressDrawable = CircularProgressDrawable(view.context)
                val selectView = v.findViewById<RadioButton>(checkedId)
                val textSize = selectView.textSize.toInt()
                circularProgressDrawable.setColorSchemeColors(Color.WHITE)
                circularProgressDrawable.strokeWidth = textSize / 8f
                circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
                circularProgressDrawable.progressRotation = 0.75f
                circularProgressDrawable.setBounds(0, 0, textSize, textSize)
                circularProgressDrawable.start()
                selectView.setCompoundDrawables(circularProgressDrawable, null, null, null)

                callback?.invoke(
                    if (checkedId == R.id.radio_watch_to) eps else listOf(episode), when (checkedId) {
                        R.id.radio_watch_to -> WATCH_TO
                        R.id.radio_watch -> Episode.PROGRESS_WATCH
                        R.id.radio_queue -> Episode.PROGRESS_QUEUE
                        R.id.radio_drop -> Episode.PROGRESS_DROP
                        else -> Episode.PROGRESS_REMOVE
                    }
                ) {
                    selectView.setCompoundDrawables(null, null, null, null)
                    circularProgressDrawable.stop()
                    if (!it) v.check(currentStatus)
                    else currentStatus = checkedId
                }
            }
        } else {
            view.item_episode_status.visibility = View.GONE
        }

        val paddingBottom = view.item_site_list.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_site_list.setPadding(
                view.item_site_list.paddingLeft,
                view.item_site_list.paddingTop,
                view.item_site_list.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
    }
}
