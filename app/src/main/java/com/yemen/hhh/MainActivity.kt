package com.yemen.hhh

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.batoulapps.adhan.Coordinates
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yemen.hhh.ui.theme.المسجدTheme
import java.util.*
import android.location.Geocoder
import android.media.MediaPlayer
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        createNotificationChannels()
        
        setContent {
            المسجدTheme {
                App(
                    initialCity = SettingsManager.loadCityConfig(this).toSharedConfig(),
                    initialReminders = SettingsManager.loadReminders(this).toSharedSettings(),
                    onSaveCity = { SettingsManager.saveCityConfig(this, it.toAndroidConfig()) },
                    onSaveReminders = { SettingsManager.saveReminders(this, it.toAndroidSettings()) },
                    onPlayAdhan = { playAdhan() },
                    onStopAdhan = { stopAdhan() },
                    onOpenUrl = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onGetCurrentLocation = { callback ->
                        getCurrentLocation(this, callback)
                    },
                    formatTime = { ms -> 
                        SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(ms))
                    },
                    formatDate = { ms ->
                        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date())
                    },
                    formatHijri = { ms ->
                        android.icu.text.SimpleDateFormat("d MMMM yyyy", android.icu.util.ULocale("ar@calendar=islamic-umalqura")).format(android.icu.util.Calendar.getInstance()) + " هـ"
                    }
                )
            }
        }
    }

    private fun playAdhan() {
        if (mediaPlayer?.isPlaying == true) return
        stopAdhan()
        mediaPlayer = MediaPlayer.create(this, R.raw.adhan)
        mediaPlayer?.start()
    }

    private fun stopAdhan() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("PRAYER_CH", "Notifications", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }
    }
}

// دوال مساعدة للتحويل بين الأنواع المشتركة والخاصة بالأندرويد
fun ReminderSettings.toSharedSettings() = com.yemen.hhh.ReminderSettings(minutesBefore, use12Hour, whatsappLink, adhanType, suhoorReminder, suhoorMinutesBefore)
fun com.yemen.hhh.ReminderSettings.toAndroidSettings() = ReminderSettings(minutesBefore, use12Hour, whatsappLink, adhanType, suhoorReminder, suhoorMinutesBefore)

fun CityConfig.toSharedConfig() = com.yemen.hhh.CityConfig(name, coords, isYemeni, method)
fun com.yemen.hhh.CityConfig.toAndroidConfig() = CityConfig(name, coords, isYemeni, method)

private fun getCurrentLocation(context: Context, onLocationReceived: (Double, Double, String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale("ar"))
                    var name = "موقعي الحالي"
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) name = addresses[0].locality ?: "موقعي"
                    } catch (e: Exception) {}
                    onLocationReceived(location.latitude, location.longitude, name)
                }
            }
    } catch (e: SecurityException) {}
}
