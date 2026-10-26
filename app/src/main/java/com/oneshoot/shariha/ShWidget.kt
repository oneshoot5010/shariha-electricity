package com.oneshoot.shariha

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ShWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        for (i in ids) m.updateAppWidget(i, build(c))
    }

    companion object {
        fun build(c: Context): RemoteViews {
            val p = c.getSharedPreferences("w", Context.MODE_PRIVATE)
            val e = p.getFloat("e", 0f)
            val v = RemoteViews(c.packageName, R.layout.widget)
            if (e > 0f) {
                val b = maxOf(0f, p.getFloat("b", 0f) - e * (System.currentTimeMillis() - p.getLong("t", 0L)) / 86400000f)
                v.setTextViewText(R.id.wb, "%.0f ج".format(b))
                v.setTextViewText(R.id.wd, "%.0f يوم".format(b / e))
            } else {
                v.setTextViewText(R.id.wb, "⚡")
                v.setTextViewText(R.id.wd, "افتح التطبيق")
            }
            v.setOnClickPendingIntent(R.id.wr, PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            return v
        }

        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            for (i in m.getAppWidgetIds(ComponentName(c, ShWidget::class.java))) m.updateAppWidget(i, build(c))
        }
    }
}
