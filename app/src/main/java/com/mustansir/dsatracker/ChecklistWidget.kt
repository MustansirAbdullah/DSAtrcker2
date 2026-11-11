package com.mustansir.dsatracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ChecklistWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, mgr, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val i = intent.getIntExtra(EXTRA_INDEX, -1)
            if (i in Curriculum.items.indices) {
                Store.toggle(context, i)
                refresh(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.mustansir.dsatracker.TOGGLE"
        const val EXTRA_INDEX = "index"
        private val ITEM_IDS = intArrayOf(R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4)

        fun refresh(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, ChecklistWidget::class.java))
            ids.forEach { update(c, mgr, it) }
        }

        private fun update(c: Context, mgr: AppWidgetManager, id: Int) {
            val v = RemoteViews(c.packageName, R.layout.widget)
            val w = Store.week(c)
            v.setTextViewText(R.id.title, "Week ${w + 1}: ${Curriculum.weeks[w]}")
            v.setTextViewText(R.id.progress, "${Store.doneCount(c)}/${Curriculum.items.size} done")

            Curriculum.items.forEachIndexed { i, label ->
                val done = Store.checked(c, i)
                v.setTextViewText(ITEM_IDS[i], (if (done) "☑ " else "☐ ") + label)
                v.setTextColor(ITEM_IDS[i], if (done) 0xFF6BCB77.toInt() else 0xFFFFFFFF.toInt())
                val intent = Intent(c, ChecklistWidget::class.java)
                    .setAction(ACTION_TOGGLE)
                    .putExtra(EXTRA_INDEX, i)
                v.setOnClickPendingIntent(
                    ITEM_IDS[i],
                    PendingIntent.getBroadcast(
                        c, i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            val open = PendingIntent.getActivity(
                c, 100, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.title, open)
            mgr.updateAppWidget(id, v)
        }
    }
}
