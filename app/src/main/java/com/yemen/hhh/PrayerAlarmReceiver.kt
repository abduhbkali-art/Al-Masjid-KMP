package com.yemen.hhh

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "الصلاة"
        val isReminder = intent.getBooleanExtra("IS_REMINDER", false)
        
        val settings = SettingsManager.loadReminders(context)
        
        // اختيار القناة المناسبة بناءً على نوع التنبيه
        val channelId = if (isReminder) {
            "CALC_CH" // قناة حساب أوقات الصلاة (للتذكير)
        } else {
            if (settings.adhanType == "adhan2") "ADHAN_CH2" else "ADHAN_CH1" // قنوات الأذان 1 و 2
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // إعداد نية فتح التطبيق كصفحة كاملة
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("SHOW_ADHAN_SCREEN", true)
            putExtra("PRAYER_NAME", prayerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, prayerName.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isReminder) "تذكير بـ $prayerName" else "حان وقت $prayerName"
        val text = if (isReminder) "بقي وقت قليل على الصلاة" else "الله أكبر، الله أكبر"

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        if (!isReminder) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
            
            // تشغيل صوت الأذان
            try {
                val rawId = if (settings.adhanType == "adhan2") R.raw.adhan2 else R.raw.adhan
                val mediaPlayer = MediaPlayer.create(context, rawId)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { it.release() }
            } catch (e: Exception) { e.printStackTrace() }
            
            // محاولة فتح الشاشة تلقائياً
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: Exception) {}
        } else {
            notificationBuilder.setContentIntent(fullScreenPendingIntent)
        }

        notificationManager.notify(prayerName.hashCode(), notificationBuilder.build())
    }
}
