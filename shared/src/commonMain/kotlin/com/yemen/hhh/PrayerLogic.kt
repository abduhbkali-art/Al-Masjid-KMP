package com.yemen.hhh

import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import kotlinx.datetime.*

data class CityConfig(
    val name: String, 
    val coords: Coordinates, 
    val isYemeni: Boolean = false,
    val method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
)

data class PrayerInfo(val name: String, val timeMs: Long)
data class CityTimes(val cityName: String, val prayers: List<PrayerInfo>)

data class ReminderSettings(
    val minutesBefore: Int = 10,
    val use12Hour: Boolean = true,
    val whatsappLink: String = "https://wa.me/967779873668",
    val adhanType: String = "adhan1",
    val suhoorReminder: Boolean = true,
    val suhoorMinutesBefore: Int = 60
)

fun isLocationInYemen(lat: Double, lng: Double): Boolean {
    return lat in 12.0..19.0 && lng in 42.0..55.0
}

fun getGlobalMethod(coords: Coordinates): CalculationMethod {
    val lat = coords.latitude
    val lng = coords.longitude
    return when {
        lat in 16.0..32.0 && lng in 34.0..55.0 -> CalculationMethod.UMM_AL_QURA
        lat in 22.0..26.0 && lng in 51.0..57.0 -> CalculationMethod.DUBAI
        lat in 28.0..31.0 && lng in 46.0..49.0 -> CalculationMethod.KUWAIT
        lat in 24.0..27.0 && lng in 50.0..52.0 -> CalculationMethod.QATAR
        lat in 22.0..32.0 && lng in 25.0..35.0 -> CalculationMethod.EGYPTIAN
        lat in 24.0..50.0 && lng in -125.0..-66.0 -> CalculationMethod.NORTH_AMERICA
        else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
    }
}

val YEMEN_CITIES = listOf(
    CityConfig("أمانة العاصمة", Coordinates(15.3500, 44.2000), isYemeni = true),
    CityConfig("محافظة صنعاء", Coordinates(15.3547, 44.2065), isYemeni = true),
    CityConfig("عدن", Coordinates(12.7855, 45.0186), isYemeni = true),
    CityConfig("تعز", Coordinates(13.5783, 44.0133), isYemeni = true),
    CityConfig("الحديدة", Coordinates(14.7979, 42.9530), isYemeni = true),
    CityConfig("إب", Coordinates(13.9667, 44.1833), isYemeni = true),
    CityConfig("حضرموت (المكلا)", Coordinates(14.5422, 49.1242), isYemeni = true),
    CityConfig("ذمار", Coordinates(14.5431, 44.4111), isYemeni = true),
    CityConfig("لحج", Coordinates(13.0583, 44.8833), isYemeni = true),
    CityConfig("مأرب", Coordinates(15.4591, 45.3253), isYemeni = true),
    CityConfig("صعدة", Coordinates(16.9405, 43.7635), isYemeni = true),
    CityConfig("حجة", Coordinates(15.6942, 43.6042), isYemeni = true),
    CityConfig("عمران", Coordinates(15.6592, 43.9439), isYemeni = true),
    CityConfig("المحويت", Coordinates(15.4701, 43.5448), isYemeni = true),
    CityConfig("الضالع", Coordinates(13.6957, 44.7314), isYemeni = true),
    CityConfig("البيضاء", Coordinates(13.9853, 45.5739), isYemeni = true),
    CityConfig("شبوة", Coordinates(14.5377, 46.8319), isYemeni = true),
    CityConfig("أبين", Coordinates(13.1287, 45.3803), isYemeni = true),
    CityConfig("المهرة (الغيظة)", Coordinates(16.2078, 52.1761), isYemeni = true),
    CityConfig("سقطرى", Coordinates(12.6000, 53.9833), isYemeni = true),
    CityConfig("الجوف", Coordinates(16.1770, 44.7751), isYemeni = true),
    CityConfig("ريمة", Coordinates(14.6288, 43.7126), isYemeni = true)
)

val WORLD_CITIES = listOf(
    CityConfig("مكة المكرمة", Coordinates(21.4225, 39.8262), method = CalculationMethod.UMM_AL_QURA),
    CityConfig("المدينة المنورة", Coordinates(24.4672, 39.6068), method = CalculationMethod.UMM_AL_QURA),
    CityConfig("دبي", Coordinates(25.2048, 55.2708), method = CalculationMethod.DUBAI),
    CityConfig("القاهرة", Coordinates(30.0444, 31.2357), method = CalculationMethod.EGYPTIAN),
    CityConfig("لندن", Coordinates(51.5074, -0.1278)),
    CityConfig("باريس", Coordinates(48.8566, 2.3522)),
    CityConfig("نيويورك", Coordinates(40.7128, -74.0060), method = CalculationMethod.NORTH_AMERICA)
)

fun calculateTimesForCity(config: CityConfig, epochMillis: Long): CityTimes {
    val method = if (config.isYemeni) CalculationMethod.MUSLIM_WORLD_LEAGUE else getGlobalMethod(config.coords)
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    
    val dateComponents = DateComponents(dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth)
    val prayerTimes = PrayerTimes(config.coords, dateComponents, method.parameters)
    
    // ملاحظة: مكتبة Adhan ترجع أوقاتاً كـ Date، سنقوم بتحويلها لـ Long لتعمل في القسم المشترك
    // هذا الجزء سيتطلب تعديلاً طفيفاً حسب نوع الـ Date المستخدم في مكتبة Adhan نسخة الـ KMP
    
    val prayers = listOf(
        PrayerInfo("الفجر", prayerTimes.fajr?.time ?: 0),
        PrayerInfo("الشروق", prayerTimes.sunrise?.time ?: 0),
        PrayerInfo("الظهر", prayerTimes.dhuhr?.time ?: 0),
        PrayerInfo("العصر", prayerTimes.asr?.time ?: 0),
        PrayerInfo("المغرب", prayerTimes.maghrib?.time ?: 0),
        PrayerInfo("العشاء", prayerTimes.isha?.time ?: 0)
    )
    
    // تطبيق التعديلات اليمنية
    val finalPrayers = if (config.isYemeni) {
        prayers.map { p ->
            when(p.name) {
                "الشروق" -> p.copy(timeMs = p.timeMs + 14 * 60000)
                "الظهر" -> p.copy(timeMs = p.timeMs + 10 * 60000)
                "المغرب" -> p.copy(timeMs = p.timeMs + 10 * 60000)
                "العصر" -> {
                    val dhuhrTime = prayers.find { it.name == "الظهر" }?.timeMs ?: p.timeMs
                    p.copy(timeMs = dhuhrTime + 3 * 3600000 + 10 * 60000)
                }
                else -> p
            }
        }
    } else prayers

    return CityTimes(config.name, finalPrayers)
}
