package com.example.shamsical
import android.app.*
import android.content.*
import android.graphics.*
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import java.time.LocalDate
import java.time.ZoneId

object Notifier {
    private val zone get() = ZoneId.systemDefault()
    fun channels(c: Context) {
        val m = c.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel("loud", "هشدار با صدا", NotificationManager.IMPORTANCE_HIGH))
        m.createNotificationChannel(NotificationChannel("silent", "اعلان بی‌صدا", NotificationManager.IMPORTANCE_LOW))
        m.createNotificationChannel(NotificationChannel("today", "تاریخ امروز", NotificationManager.IMPORTANCE_LOW))
    }
    private fun pi(c: Context, id: Int, i: Intent) = PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun cancel(c: Context, it: Item) { c.getSystemService(AlarmManager::class.java).cancel(pi(c, it.id.toInt(), Intent(c, AlarmReceiver::class.java))) }

    fun schedule(c: Context, it: Item) {
        cancel(c, it)
        if (it.alarm == 0) return
        val at = LocalDate.parse(it.date).atStartOfDay(zone).plusMinutes(((if (it.event) it.start else 480) - it.lead).toLong()).toInstant().toEpochMilli()
        if (at <= System.currentTimeMillis()) return
        val i = Intent(c, AlarmReceiver::class.java).putExtra("t", it.title).putExtra("a", it.alarm).putExtra("id", it.id.toInt())
            .putExtra("when", if (it.event) "${fmt(it.start)}–${fmt(it.end)}" else "")
        val am = c.getSystemService(AlarmManager::class.java); val p = pi(c, it.id.toInt(), i)
        try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) } catch (e: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) }
    }
    fun rescheduleAll(c: Context) = Store.items(c).forEach { schedule(c, it) }

    /** Ongoing notification: small (status bar) icon = day number only, text = full Shamsi + Gregorian date. */
    fun showToday(c: Context) {
        channels(c)
        val d = LocalDate.now(); val j = Jal.toJ(d)
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 70f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        Canvas(bmp).drawText(j.d.toString().fa(), 48f, 72f, pt)
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, "today").setSmallIcon(IconCompat.createWithBitmap(bmp))
            .setContentTitle(fullJ(d)).setContentText(fullG(d)).setOngoing(true).setShowWhen(false).setContentIntent(open).build()
        c.getSystemService(NotificationManager::class.java).notify(1, n)
        val next = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 1000
        c.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, next, pi(c, 1, Intent(c, SysReceiver::class.java).setAction("REFRESH")))
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Notifier.channels(c)
        val silent = i.getIntExtra("a", 1) == 2
        val n = NotificationCompat.Builder(c, if (silent) "silent" else "loud")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(i.getStringExtra("t")).setContentText(i.getStringExtra("when"))
            .setPriority(if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH).setSilent(silent).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)).build()
        c.getSystemService(NotificationManager::class.java).notify(i.getIntExtra("id", 2), n)
    }
}

class SysReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != "REFRESH") Notifier.rescheduleAll(c)
        Notifier.showToday(c)
    }
}
