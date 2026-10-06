package com.example.iha_panel.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iha_panel.domain.UavPlatform
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@Composable
fun WeatherScreen(vm: WeatherViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            fetchLocation(context) { lat, lon -> vm.loadWeatherData(lat, lon) }
        } else {
            vm.loadWeatherData(41.0082, 28.9784)
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fetchLocation(context) { lat, lon -> vm.loadWeatherData(lat, lon) }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        PlatformChips(
            selected = state.selectedPlatform,
            onSelect = vm::onPlatformSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> LoadingBox()

            state.errorMessage != null -> ErrorCard(
                message = state.errorMessage ?: "",
                onRetry = vm::retry
            )

            state.hasData -> {
                DecisionCard(state.flightStatus, state.evaluationReason)
                Spacer(modifier = Modifier.height(14.dp))
                PlatformLimitRow(state.selectedPlatform)
                Spacer(modifier = Modifier.height(16.dp))
                MetricsGrid(state)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Eşikler demo amaçlıdır, gerçek operasyonel limitler değildir",
            color = Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (state.hasData) {
            Text(
                text = "Son Güncelleme: ${state.lastUpdated}",
                color = Color.DarkGray,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    fetchLocation(context) { lat, lon -> vm.loadWeatherData(lat, lon) }
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2530)),
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
                    selectedContainerColor = Color(0xFF00ADB5),
                    containerColor = Color(0xFF1E2530)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color(0xFF00ADB5) else Color(0xFF2C3545)
                )
            )
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF00ADB5))
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
            Button(onClick = onRetry) {
                Text("Tekrar dene")
            }
        }
    }
}

@Composable
private fun DecisionCard(status: FlightStatus, reason: String) {
    val cardColor = when (status) {
        FlightStatus.GO -> Color(0xFF2E7D32)
        FlightStatus.CAUTION -> Color(0xFFF57F17)
        FlightStatus.NO_GO -> Color(0xFFC62828)
    }
    val statusText = when (status) {
        FlightStatus.GO -> "✔ GO"
        FlightStatus.CAUTION -> "⚠ CAUTION"
        FlightStatus.NO_GO -> "✖ NO-GO"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reason,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlatformLimitRow(platform: UavPlatform) {
    Surface(
        color = Color(0xFF1E2530),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = platform.platformType,
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = "Demo rüzgar limiti: ${platform.maxWindLimitKmh.toInt()} km/h",
                color = Color(0xFF00ADB5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricsGrid(state: WeatherUiState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard("Rüzgar Hızı", "${state.windSpeed} km/h", Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        MetricCard("Sıcaklık", "${state.temperature} °C", Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard("Görüş Mesafesi", "${state.visibility.toInt()} m", Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        MetricCard("Yağış Miktarı", "${state.precipitation} mm", Modifier.weight(1f))
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(85.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
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
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocation(context: Context, onResult: (Double, Double) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)

    client.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null) {
            Log.d("LocationGPS", "Alınan Konum: ${location.latitude}, ${location.longitude}")
            onResult(location.latitude, location.longitude)
        } else {
            client.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    Log.d("LocationGPS", "Son Bilinen: ${lastLoc.latitude}, ${lastLoc.longitude}")
                    onResult(lastLoc.latitude, lastLoc.longitude)
                } else {
                    Log.d("LocationGPS", "Konum bulunamadı, fallback İstanbul")
                    onResult(41.0082, 28.9784)
                }
            }
        }
    }.addOnFailureListener { e ->
        Log.e("LocationGPS", "Hata: ${e.message}")
        onResult(41.0082, 28.9784)
    }
}