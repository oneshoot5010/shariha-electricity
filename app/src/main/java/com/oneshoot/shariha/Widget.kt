package com.oneshoot.shariha

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class Widget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        for (i in ids) m.updateAppWidget(i, view(c))
    }

    companion object {
        fun view(c: Context): RemoteViews {
            val p = c.getSharedPreferences("w", 0)
            val v = RemoteViews(c.packageName, R.layout.widget)
            val e = p.getFloat("e", 0f)
            if (e > 0f) {
                val b = maxOf(0.0, p.getFloat("b", 0f) - e * (System.currentTimeMillis() - p.getLong("t", 0L)) / 864e5)
                v.setTextViewText(R.id.wb, "%.0f ج".format(b))
                v.setTextViewText(R.id.wd, "%.0f يوم".format(b / e))
            } else {
                v.setTextViewText(R.id.wb, "Wattly")
                v.setTextViewText(R.id.wd, "سجّل شحنتين")
            }
            v.setOnClickPendingIntent(R.id.wr, PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            return v
        }

        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            for (i in m.getAppWidgetIds(ComponentName(c, Widget::class.java))) m.updateAppWidget(i, view(c))
        }
    }
}
