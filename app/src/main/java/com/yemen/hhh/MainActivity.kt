package com.yemen.hhh

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.CalculationMethod
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yemen.hhh.ui.theme.المسجدTheme
import java.util.*
import android.location.Geocoder
import android.media.MediaPlayer

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        createNotificationChannels()
        
        setContent {
            المسجدTheme {
                // استدعاء الواجهة المشتركة
                App(
                    initialCity = SettingsManager.loadCityConfig(this).toSharedConfig(),
                    initialReminders = SettingsManager.loadReminders(this).toSharedSettings(),
                    onSaveCity = { city: com.yemen.hhh.CityConfig -> 
                        SettingsManager.saveCityConfig(this, city.toAndroidConfig()) 
                    },
                    onSaveReminders = { reminders: com.yemen.hhh.ReminderSettings -> 
                        SettingsManager.saveReminders(this, reminders.toAndroidSettings()) 
                    },
                    onPlayAdhan = { playAdhan() },
                    onStopAdhan = { stopAdhan() },
                    onOpenUrl = { url: String ->
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            Toast.makeText(this, "لا يمكن فتح الرابط", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onGetCurrentLocation = { callback: ((Double, Double, String) -> Unit) ->
                        getCurrentLocation(this, callback)
                    },
                    formatTime = { ms: Long -> 
                        SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(ms))
                    },
                    formatDate = { _: Long ->
                        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date())
                    },
                    formatHijri = { _: Long ->
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

object SettingsManager {
    private const val PREFS_NAME = "prayer_settings"
    
    fun saveCityConfig(context: Context, config: CityConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("city_name", config.name)
            putFloat("lat", config.coords.latitude.toFloat())
            putFloat("lng", config.coords.longitude.toFloat())
            putBoolean("is_yemeni", config.isYemeni)
            putString("method", config.method.name)
            apply()
        }
    }

    fun loadCityConfig(context: Context): CityConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString("city_name", "أمانة العاصمة") ?: "أمانة العاصمة"
        val lat = prefs.getFloat("lat", 15.3500f).toDouble()
        val lng = prefs.getFloat("lng", 44.2000f).toDouble()
        val isYemeni = prefs.getBoolean("is_yemeni", true)
        val methodName = prefs.getString("method", CalculationMethod.MUSLIM_WORLD_LEAGUE.name)
        val method = try { CalculationMethod.valueOf(methodName!!) } catch (e: Exception) { CalculationMethod.MUSLIM_WORLD_LEAGUE }
        return CityConfig(name, Coordinates(lat, lng), isYemeni, method)
    }

    fun saveReminders(context: Context, s: ReminderSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt("min_before", s.minutesBefore)
            putBoolean("use_12h", s.use12Hour)
            putString("adhan_type", s.adhanType)
            apply()
        }
    }

    fun loadReminders(context: Context): ReminderSettings {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSettings(
            minutesBefore = p.getInt("min_before", 10),
            use12Hour = p.getBoolean("use_12h", true),
            adhanType = p.getString("adhan_type", "adhan1") ?: "adhan1"
        )
    }
}

// الكلاسات المحلية للأندرويد
data class CityConfig(val name: String, val coords: Coordinates, val isYemeni: Boolean, val method: CalculationMethod)
data class ReminderSettings(val minutesBefore: Int, val use12Hour: Boolean, val whatsappLink: String = "", val adhanType: String = "", val suhoorReminder: Boolean = false, val suhoorMinutesBefore: Int = 0)

// دوال التحويل
fun ReminderSettings.toSharedSettings() = com.yemen.hhh.ReminderSettings(minutesBefore, use12Hour, whatsappLink, adhanType)
fun com.yemen.hhh.ReminderSettings.toAndroidSettings() = ReminderSettings(minutesBefore, use12Hour, whatsappLink, adhanType)

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
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) name = addresses[0].locality ?: "موقعي"
                    } catch (e: Exception) {}
                    onLocationReceived(location.latitude, location.longitude, name)
                }
            }
    } catch (e: SecurityException) {}
}
