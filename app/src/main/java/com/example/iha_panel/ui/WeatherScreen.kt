package com.example.iha_panel.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight

@Composable
fun WeatherScreen(vm: WeatherViewModel = viewModel()) {

    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { granted ->
        Log.d("Location", "Permission granted: $granted")
        if (granted) {
            fetchLocation(context) { lat, lon -> vm.load(lat, lon) }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val s = state) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }

            is UiState.Error -> {
                Text(text = s.message, color = Color.Red)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { vm.load(41.0, 29.0) }) {
                    Text("Tekrar Dene")
                }
            }

            is UiState.Success -> {
                val (statusText, cardColor) = when (s.status) {
                    FlightStatus.GO -> "✓ GO" to Color(0xFF2E7D32)
                    FlightStatus.CAUTION -> "! CAUTION" to Color(0xFFF9A825)
                    FlightStatus.NO_GO -> "✕ NO-GO" to Color(0xFFC62828)
                }
                val textColor = if (s.status == FlightStatus.CAUTION) Color.Black else Color.White

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = statusText,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = s.reason,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = String.format(Locale.ROOT, "Location: %.2f, %.2f", s.lat, s.lon),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Wind Speed: ${s.weather.windSpeed} km/h")
                Text(text = "Visibility: ${s.weather.visibility} m")
                Text(text = "Precipitation: ${s.weather.precipitation} mm")
                Text(text = "Temperature: ${s.weather.temperature} °C")
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocation(context: Context, onResult: (Double, Double) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        CancellationTokenSource().token
    )
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            } else {
                Log.d("Location", "Location is null, keeping fallback")
            }
        }
        .addOnFailureListener { e ->
            Log.d("Location", "Failed: ${e.message}")
        }
}