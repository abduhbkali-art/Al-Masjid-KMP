package com.yemen.hhh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    initialCity: CityConfig,
    initialReminders: ReminderSettings,
    onSaveCity: (CityConfig) -> Unit,
    onSaveReminders: (ReminderSettings) -> Unit,
    onPlayAdhan: () -> Unit,
    onStopAdhan: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onGetCurrentLocation: ((Double, Double, String) -> Unit) -> Unit,
    formatTime: (Long) -> String,
    formatDate: (Long) -> String,
    formatHijri: (Long) -> String
) {
    var currentCity by remember { mutableStateOf(initialCity) }
    var reminders by remember { mutableStateOf(initialReminders) }
    
    var showCityDialog by remember { mutableStateOf(false) }
    var isAdhanPlaying by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAthkarDialog by remember { mutableStateOf(false) }
    var showTasbihDialog by remember { mutableStateOf(false) }

    // سيتم استدعاء الحسابات المشتركة هنا
    val currentTime = remember { mutableStateOf(0L) } // يحتاج لتحديث كل ثانية
    val cityTimes = remember(currentCity) { calculateTimesForCity(currentCity, 0L) } // 0L كمثال للوقت الحالي

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("تطبيق المسجد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("رؤية 2026 الذكية", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White),
                actions = {
                    IconButton(onClick = { 
                        onGetCurrentLocation { lat, lng, name ->
                            currentCity = CityConfig(name, Coordinates(lat, lng), isYemeni = isLocationInYemen(lat, lng))
                            onSaveCity(currentCity)
                        }
                    }) { Icon(Icons.Default.MyLocation, "موقعي", tint = Color.White) }
                    
                    IconButton(onClick = { onOpenUrl(reminders.whatsappLink) }) { 
                        Icon(Icons.AutoMirrored.Filled.Chat, "تواصل", tint = Color.White) 
                    }
                    
                    IconButton(onClick = { 
                        if (isAdhanPlaying) onStopAdhan() else onPlayAdhan()
                        isAdhanPlaying = !isAdhanPlaying
                    }) {
                        Icon(if (isAdhanPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, "الأذان", tint = Color.White)
                    }
                    IconButton(onClick = { showReminderDialog = true }) { Icon(Icons.Default.Settings, "الإعدادات", tint = Color.White) }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), Color.White))
        )) {
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الكروت الأساسية
                DateDisplayCard(formatDate(0L), formatHijri(0L))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showCityDialog = true }, 
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("المدينة المختارة", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(currentCity.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                CityPrayerCard(cityTimes, reminders.use12Hour, formatTime)
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ModernActionButton(Icons.Default.AutoAwesome, "الأذكار") { showAthkarDialog = true }
                    ModernActionButton(Icons.Default.RadioButtonChecked, "المسبحة") { showTasbihDialog = true }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                SmartQuoteCard()
            }
        }
    }

    // الحوارات (Dialogs)
    if (showCityDialog) CitySelectionDialog(onDismiss = { showCityDialog = false }, onSelect = { currentCity = it; onSaveCity(it); showCityDialog = false })
    if (showAthkarDialog) AthkarDialog { showAthkarDialog = false }
    if (showTasbihDialog) TasbihDialog { showTasbihDialog = false }
    if (showReminderDialog) ReminderSettingsDialog(reminders, { showReminderDialog = false }, { reminders = it; onSaveReminders(it); showReminderDialog = false })
}

@Composable
fun DateDisplayCard(dateStr: String, hijriStr: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hijriStr, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(dateStr, fontSize = 14.sp)
        }
    }
}

@Composable
fun CityPrayerCard(city: CityTimes, use12h: Boolean, formatTime: (Long) -> String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            city.prayers.forEach { p ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.name)
                    Text(formatTime(p.timeMs), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SmartQuoteCard() {
    val quotes = listOf("الصلاة عماد الدين", "أرحنا بها يا بلال", "من لزم الاستغفار جعل الله له من كل هم فرجاً")
    val quote = remember { quotes.random() }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(quote, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun ModernActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, modifier = Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(modifier = Modifier.height(8.dp)); Text(label, fontSize = 13.sp)
    }
}

@Composable
fun CitySelectionDialog(onDismiss: () -> Unit, onSelect: (CityConfig) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("اختر موقعك", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item { Text("اليمن السعيد", modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.secondary) }
                    items(YEMEN_CITIES) { city -> 
                        Surface(onClick = { onSelect(city) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(city.name, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AthkarDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("الأذكار", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("إغلاق") }
            }
        }
    }
}

@Composable
fun TasbihDialog(onDismiss: () -> Unit) {
    var count by remember { mutableStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), fontSize = 48.sp, fontWeight = FontWeight.Black)
                Button(onClick = { count++ }) { Text("تسبيح") }
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        }
    }
}

@Composable
fun ReminderSettingsDialog(initial: ReminderSettings, onDismiss: () -> Unit, onSave: (ReminderSettings) -> Unit) {
    var s by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("الإعدادات", fontWeight = FontWeight.Bold)
                // هنا يمكن إضافة أزرار التحكم
                Button(onClick = { onSave(s) }, modifier = Modifier.fillMaxWidth()) { Text("حفظ") }
            }
        }
    }
}
