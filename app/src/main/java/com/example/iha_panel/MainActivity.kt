package com.example.iha_panel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iha_panel.domain.UavPlatform
import com.example.iha_panel.ui.FlightStatus
import com.example.iha_panel.ui.WeatherScreen
import com.example.iha_panel.ui.WeatherUiState
import com.example.iha_panel.ui.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen(
    state: WeatherUiState,
    onPlatformSelect: (UavPlatform) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Başlık
        Text(
            text = "BAYKAR FLIGHT OPS DECISION SUPPORT",
            color = Color(0xFF90A4AE),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Seçim Çipleri (Yatay Kaydırılabilir)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UavPlatform.entries.forEach { platform ->
                val isSelected = platform == state.selectedPlatform
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelect(platform) },
                    label = {
                        Text(
                            text = platform.platformName,
                            color = if (isSelected) Color.Black else Color(0xFFECEFF1),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Uçuş Durum Kartı (GO / CAUTION / NO-GO)
        val cardColor = when (state.flightStatus) {
            FlightStatus.GO -> Color(0xFF2E7D32)
            FlightStatus.CAUTION -> Color(0xFFE65100)
            FlightStatus.NO_GO -> Color(0xFFC62828)
        }

        val statusText = when (state.flightStatus) {
            FlightStatus.GO -> "✔ GO"
            FlightStatus.CAUTION -> "⚠ CAUTION"
            FlightStatus.NO_GO -> "✖ NO-GO"
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.evaluationReason,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seçili Platform Limit Özeti
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
                    text = "Tip: ${state.selectedPlatform.platformType}",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp
                )
                Text(
                    text = "Rüzgar Limiti: ${state.selectedPlatform.maxWindLimitKmh.toInt()} km/h",
                    color = Color(0xFF00ADB5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetri Değerleri
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

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Son Güncelleme: ${state.lastUpdated}",
            color = Color(0xFF607D8B),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222831)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Telemetriyi Yenile", color = Color.White)
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
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
            Text(text = title, color = Color(0xFF90A4AE), fontSize = 11.sp)
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