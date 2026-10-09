package com.example.iha_panel.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iha_panel.domain.AviationMath
import com.example.iha_panel.domain.AviationSafetyStandards
import com.example.iha_panel.domain.EvaluationResult
import com.example.iha_panel.domain.FlightStatus
import com.example.iha_panel.domain.MissionBriefing
import com.example.iha_panel.domain.MissionLocation
import com.example.iha_panel.domain.MissionLocations
import com.example.iha_panel.domain.MissionPointRole
import com.example.iha_panel.domain.PointBriefing
import com.example.iha_panel.domain.UavPlatform
import com.example.iha_panel.domain.WeatherSnapshot
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke

private val Teal = Color(0xFF00ADB5)
private val Panel = Color(0xFF1E2530)
private val PanelBorder = Color(0xFF2C3545)

private fun statusColor(status: FlightStatus): Color = when (status) {
    FlightStatus.GO -> Color(0xFF2E7D32)
    FlightStatus.CAUTION -> Color(0xFFF57F17)
    FlightStatus.NO_GO -> Color(0xFFC62828)
}

private fun statusText(status: FlightStatus): String = when (status) {
    FlightStatus.GO -> "✔ GO"
    FlightStatus.CAUTION -> "⚠ CAUTION"
    FlightStatus.NO_GO -> "✖ NO-GO"
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun WeatherScreen(vm: WeatherViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val loadFromGps: () -> Unit = {
        fetchLocation(context) { lat, lon, fallback -> vm.loadWeatherData(lat, lon, fallback) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            loadFromGps()
        } else {
            vm.loadWeatherData(MissionLocations.ISTANBUL.lat, MissionLocations.ISTANBUL.lon, true)
        }
    }

    val requestLocation: () -> Unit = {
        if (hasLocationPermission(context)) {
            loadFromGps()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        if (vm.shouldRequestInitialLocation()) requestLocation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "İHA UÇUŞ HAVA DURUMU BRİFİNGİ",
            color = Color(0xFF90A4AE),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlatformChips(selected = state.selectedPlatform, onSelect = vm::onPlatformSelected)

        Spacer(modifier = Modifier.height(12.dp))

        RunwayHeadingField(
            text = state.runwayHeadingText,
            invalid = state.runwayHeadingInvalid,
            onChange = vm::onRunwayHeadingChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        val briefing = state.briefing
        when {
            state.isLoading -> LoadingBox()

            briefing == null -> ErrorCard(
                message = state.errorMessage ?: "Hava verisi alınamadı.",
                onRetry = vm::retry
            )

            else -> {
                MissionDecisionCard(briefing)
                Spacer(modifier = Modifier.height(14.dp))

                PointSelector(
                    briefing = briefing,
                    selectedRole = state.selectedRole,
                    onSelect = vm::onRoleSelected
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (state.selectedRole) {
                    MissionPointRole.LAUNCH -> LocationLabel("Konum: ${state.launchLocation.name}")
                    MissionPointRole.AREA -> LocationPicker(
                        label = "Görev sahası",
                        selected = state.areaLocation,
                        onSelect = vm::onAreaSelected
                    )
                    MissionPointRole.RECOVERY -> LocationPicker(
                        label = "İniş / alternatif",
                        selected = state.recoveryLocation,
                        onSelect = vm::onRecoverySelected
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val point = briefing.points.firstOrNull { it.role == state.selectedRole }
                if (point == null) {
                    Text(
                        text = "Bu nokta için veri alınamadı. Karar verilemiyor.",
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp
                    )
                } else {
                    PointDetails(
                        point = point,
                        platform = state.selectedPlatform,
                        runwayHeadingDeg = state.runwayHeadingDeg
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Eşikler demo amaçlıdır, gerçek operasyonel limitler değildir. " +
                "Veri: Open-Meteo model tahmini (METAR/TAF veya resmî bülten yerine geçmez).",
            color = Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (state.hasData) {
            Text(
                text = "Son gözlem: ${state.lastObservation} (model verisi)",
                color = Color.DarkGray,
                fontSize = 11.sp
            )
            Text(
                text = "Son yenileme: ${state.lastRefresh}",
                color = Color.DarkGray,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = requestLocation,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Panel),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (state.isLoading) "Telemetri Alınıyor..." else "Telemetriyi Yenile (GPS)",
                color = if (state.isLoading) Color.Gray else Color.White
            )
        }
    }
}

@Composable
private fun PlatformChips(selected: UavPlatform, onSelect: (UavPlatform) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UavPlatform.entries.forEach { platform ->
            val isSelected = platform == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(platform) },
                label = {
                    Text(
                        text = platform.platformName,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Teal,
                    containerColor = Panel
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Teal else PanelBorder
                )
            )
        }
    }
}

@Composable
private fun RunwayHeadingField(text: String, invalid: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text("Pist yönü (°, gerçek kuzey) – opsiyonel", fontSize = 12.sp) },
        supportingText = {
            Text(
                if (invalid) "0–360 arası bir değer girin" else "Yan rüzgâr hesabı için; boşsa değerlendirilmez",
                fontSize = 11.sp
            )
        },
        isError = invalid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Teal)
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✕ VERİ ALINAMADI", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Uçuş kararı verilemiyor.",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Tekrar dene") }
        }
    }
}

@Composable
private fun MissionDecisionCard(briefing: MissionBriefing) {
    val available = briefing.decisionAvailable
    val color = if (available) statusColor(briefing.status) else Color(0xFF455A64)
    val title = if (available) statusText(briefing.status) else "? KARAR VERİLEMİYOR"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = briefing.reason,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Karar penceresi: şimdi + önümüzdeki 3 saat • ${briefing.points.size} nokta",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun PointSelector(
    briefing: MissionBriefing,
    selectedRole: MissionPointRole,
    onSelect: (MissionPointRole) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MissionPointRole.entries.forEach { role ->
            val point = briefing.points.firstOrNull { it.role == role }
            val dot = point?.let { statusColor(it.worstStatus) } ?: Color(0xFF455A64)
            val isSelected = role == selectedRole
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(role) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(dot, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = role.label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Teal,
                    containerColor = Panel
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LocationLabel(text: String) {
    Text(text = text, color = Color.LightGray, fontSize = 12.sp)
}

@Composable
private fun LocationPicker(
    label: String,
    selected: MissionLocation?,
    onSelect: (MissionLocation?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Butonun üstündeki yönerge
        Text(
            text = "$label konumunu seçin",
            color = Color(0xFF6B7280),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                border = BorderStroke(1.dp, Panel),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Panel)
            ) {
                Text(
                    text = "${selected?.name ?: "Kalkışla aynı"} ▾",
                    color = Panel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Kalkışla aynı") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
                MissionLocations.presets.forEach { loc ->
                    DropdownMenuItem(
                        text = { Text(loc.name) },
                        onClick = {
                            onSelect(loc)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PointDetails(point: PointBriefing, platform: UavPlatform, runwayHeadingDeg: Int?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${point.role.label} • ${point.locationName}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FindingsCard(point.now)
        Spacer(modifier = Modifier.height(12.dp))

        OutlookRow(point)
        Spacer(modifier = Modifier.height(12.dp))

        PlatformLimitRow(platform)
        Spacer(modifier = Modifier.height(12.dp))

        MetricsGrid(point.weather, runwayHeadingDeg)
    }
}

@Composable
private fun FindingsCard(result: EvaluationResult) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ŞİMDİKİ DURUM: ${statusText(result.status)}",
                color = statusColor(result.status).copy(alpha = 1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (result.findings.isEmpty()) {
                Text(text = result.reason, color = Color.LightGray, fontSize = 12.sp)
            } else {
                result.findings.forEach { f ->
                    Text(
                        text = "● ${f.factor.label}: ${f.message}",
                        color = if (f.status == FlightStatus.NO_GO) Color(0xFFEF5350) else Color(0xFFFFB74D),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }

            if (result.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                result.notes.forEach { n ->
                    Text(text = "ℹ $n", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun OutlookRow(point: PointBriefing) {
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "ÖNÜMÜZDEKİ SAATLER", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        if (point.outlook.isEmpty()) {
            Text(text = "Tahmin verisi yok", color = Color.Gray, fontSize = 12.sp)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                point.outlook.forEach { slot ->
                    Surface(
                        color = statusColor(slot.result.status),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = clock.format(Date(slot.timeEpochSec * 1000)),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = statusText(slot.result.status),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            // Pencerenin en kötü saatinin nedeni (şimdiden daha kötüyse)
            val worstSlot = point.outlook.maxByOrNull { it.result.status.ordinal }
            if (worstSlot != null && worstSlot.result.status != FlightStatus.GO) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${clock.format(Date(worstSlot.timeEpochSec * 1000))}: ${worstSlot.result.reason}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PlatformLimitRow(platform: UavPlatform) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = platform.platformType, color = Color.Gray, fontSize = 11.sp)
            Text(
                text = "Demo limit: rüzgâr ${platform.maxWindLimitKmh.toInt()} / hamle ${platform.maxGustKmh.toInt()} km/h",
                color = Teal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricsGrid(w: WeatherSnapshot, runwayHeadingDeg: Int?) {
    val crosswind = runwayHeadingDeg?.let {
        AviationMath.crosswindKmh(maxOf(w.windSpeedKmh, w.windGustKmh), w.windDirectionDeg, it)
    }
    val ceilingText = when {
        w.cloudCoverLowPct == null -> "Veri yok"
        w.cloudCoverLowPct < AviationSafetyStandards.CEILING_COVER_PCT -> "Tavan yok"
        else -> "~${AviationMath.estimatedCeilingM(w.temperatureC, w.dewPointC).roundToInt()} m"
    }
    val da = AviationMath.densityAltitudeM(w.pressureHpa, w.temperatureC)

    val cells = listOf(
        "Rüzgâr" to "${w.windSpeedKmh.roundToInt()} km/h • ${w.windDirectionDeg.roundToInt()}°",
        "Hamle" to "${w.windGustKmh.roundToInt()} km/h",
        "İrtifa Rüzgârı" to (w.windAloftKmh?.let { "${it.roundToInt()} km/h" } ?: "Veri yok"),
        "Yan Rüzgâr" to (crosswind?.let { "${it.roundToInt()} km/h" } ?: "Pist yönü yok"),
        "Görüş Mesafesi" to "${w.visibilityM.roundToInt()} m",
        "Bulut Tabanı" to ceilingText,
        "Yağış" to String.format(Locale.US, "%.1f mm", w.precipitationMm),
        "Hadise" to AviationMath.describeWeatherCode(w.weatherCode),
        "Sıcaklık / Çiğ N." to "${w.temperatureC.roundToInt()} / ${w.dewPointC.roundToInt()} °C",
        "Yoğunluk İrtifası" to "${da.roundToInt()} m",
        "Basınç" to "${w.pressureHpa.roundToInt()} hPa",
        "Aydınlık" to if (w.isDay) "Gündüz" else "Gece"
    )

    cells.chunked(2).forEachIndexed { index, row ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(row[0].first, row[0].second, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(row[1].first, row[1].second, Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(85.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocation(context: Context, onResult: (Double, Double, Boolean) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val fallback = MissionLocations.ISTANBUL

    client.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null) {
            Log.d("LocationGPS", "Alınan Konum: ${location.latitude}, ${location.longitude}")
            onResult(location.latitude, location.longitude, false)
        } else {
            client.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    Log.d("LocationGPS", "Son Bilinen: ${lastLoc.latitude}, ${lastLoc.longitude}")
                    onResult(lastLoc.latitude, lastLoc.longitude, false)
                } else {
                    Log.d("LocationGPS", "Konum bulunamadı, fallback İstanbul")
                    onResult(fallback.lat, fallback.lon, true)
                }
            }
        }
    }.addOnFailureListener { e ->
        Log.e("LocationGPS", "Hata: ${e.message}")
        onResult(fallback.lat, fallback.lon, true)
    }
}
