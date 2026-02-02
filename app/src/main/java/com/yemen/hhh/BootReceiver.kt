package com.yemen.hhh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // استرجاع الإعدادات والمدينة المخزنة عند إعادة تشغيل الجهاز
            val cityConfig = SettingsManager.loadCityConfig(context)
            val reminders = SettingsManager.loadReminders(context)
            val cityTimes = calculateTimesForCity(cityConfig)
            
            // تمرير المعامل الثالث 'reminders' الناقص لإصلاح الخطأ
            schedulePrayerAlarms(context, cityTimes, reminders)
        }
    }
}
