
package com.example.locationsimulator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue =
            applicationContext.packageName

        setContent {
            MaterialTheme {
                LocationSimulatorScreen()
            }
        }
    }
}

@Composable
fun LocationSimulatorScreen() {

    val context = LocalContext.current

    var latitude by remember {
        mutableStateOf("-6.1754")
    }

    var longitude by remember {
        mutableStateOf("106.8272")
    }

    var selectedLatitude by remember {
        mutableDoubleStateOf(-6.1754)
    }

    var selectedLongitude by remember {
        mutableDoubleStateOf(106.8272)
    }

    var status by remember {
        mutableStateOf("Masukkan koordinat lokasi")
    }

    var simulationRequested by remember {
        mutableStateOf(false)
    }

    var pendingLatitude by remember {
        mutableDoubleStateOf(-6.1754)
    }

    var pendingLongitude by remember {
        mutableDoubleStateOf(106.8272)
    }

    val isSimulationRunning by
    SimulationState.isRunning.collectAsState()

    var showSuccessPopup by remember {
        mutableStateOf(false)
    }

    var awaitingConfirmation by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        isSimulationRunning,
        awaitingConfirmation
    ) {
        if (isSimulationRunning && awaitingConfirmation) {
            showSuccessPopup = true
            awaitingConfirmation = false
            status = "GPS Simulation Active"
        }
    }

    // Popup meme CIHUYY
    if (showSuccessPopup) {
        AlertDialog(
            onDismissRequest = {
                showSuccessPopup = false
            },
            title = {
                Text("Dah aktif ya boi")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.cihuyy
                        ),
                        contentDescription = "Meme CIHUYY",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSimulationRunning) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (isSimulationRunning) {
                                    "● GPS Simulation Active"
                                } else {
                                    "○ GPS Simulation Inactive"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isSimulationRunning) {
                                    "Simulasi lokasi sedang berjalan"
                                } else {
                                    "Simulasi lokasi sedang berhenti"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        text = "Lokasi virtual berhasil diaktifkan!"
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Lat: $pendingLatitude, Lon: $pendingLongitude"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessPopup = false
                    }
                ) {
                    Text("CIHUYY, LANJUT!")
                }
            }
        )
    }

    fun startSimulationService() {
        val intent = Intent(
            context,
            MockLocationService::class.java
        ).apply {
            action = MockLocationService.ACTION_START

            putExtra(
                MockLocationService.EXTRA_LATITUDE,
                pendingLatitude
            )

            putExtra(
                MockLocationService.EXTRA_LONGITUDE,
                pendingLongitude
            )
        }

        try {
            awaitingConfirmation = true

            ContextCompat.startForegroundService(
                context,
                intent
            )

            status = "Menunggu konfirmasi GPS aktif..."
        } catch (e: Exception) {
            awaitingConfirmation = false
            status = "Gagal memulai layanan: ${e.message}"
        }
    }

    // Meminta izin lokasi
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            if (fineGranted && simulationRequested) {
                startSimulationService()
            } else {
                status = "Izin lokasi presisi diperlukan"
            }

            simulationRequested = false
        }

    // Meminta izin notifikasi Android 13+
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            status = if (granted) {
                "Izin notifikasi diberikan."
            } else {
                "Izin notifikasi ditolak. Notifikasi mungkin tidak tampil."
            }
        }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "Location Simulator",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = latitude,
            onValueChange = {
                latitude = it
            },
            label = {
                Text("Latitude")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = longitude,
            onValueChange = {
                longitude = it
            },
            label = {
                Text("Longitude")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // Menampilkan lokasi pada peta
        Button(
            onClick = {
                val lat = latitude.toDoubleOrNull()
                val lon = longitude.toDoubleOrNull()

                if (
                    lat != null &&
                    lon != null &&
                    lat in -90.0..90.0 &&
                    lon in -180.0..180.0
                ) {
                    selectedLatitude = lat
                    selectedLongitude = lon

                    status = "Lokasi dipilih: $lat, $lon"
                } else {
                    status = "Koordinat tidak valid!"
                }
            }
        ) {
            Text("Show Location")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // Memulai simulasi GPS
        Button(
            onClick = {
                val lat = latitude.toDoubleOrNull()
                val lon = longitude.toDoubleOrNull()

                if (
                    lat == null ||
                    lon == null ||
                    lat !in -90.0..90.0 ||
                    lon !in -180.0..180.0
                ) {
                    status = "Koordinat tidak valid!"
                } else {

                    selectedLatitude = lat
                    selectedLongitude = lon

                    pendingLatitude = lat
                    pendingLongitude = lon

                    val fineGranted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                    if (fineGranted) {
                        startSimulationService()
                    } else {
                        simulationRequested = true

                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Simulation")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Menghentikan simulasi GPS
        OutlinedButton(
            onClick = {
                val intent = Intent(
                    context,
                    MockLocationService::class.java
                ).apply {
                    action = MockLocationService.ACTION_STOP
                }

                try {
                    context.startService(intent)

                    awaitingConfirmation = false
                    showSuccessPopup = false

                    status = "Perintah Stop Simulation dikirim"
                } catch (e: Exception) {
                    status = "Gagal menghentikan layanan: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Simulation")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(text = status)

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Location Map",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OpenStreetMapView(
            latitude = selectedLatitude,
            longitude = selectedLongitude
        )
    }
}

@Composable
fun OpenStreetMapView(
    latitude: Double,
    longitude: Double
) {

    val context = LocalContext.current

    val mapView = remember(context) {

        val apiKey = BuildConfig.MAPTILER_API_KEY

        val mapTilerSource = XYTileSource(
            "MapTilerStreets",
            0,
            19,
            256,
            ".png?key=$apiKey",
            arrayOf(
                "https://api.maptiler.com/maps/streets-v2/256/"
            )
        )

        MapView(context).apply {
            setTileSource(mapTilerSource)
            setMultiTouchControls(true)
            setUseDataConnection(true)

            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(
        mapView,
        latitude,
        longitude
    ) {
        val point = GeoPoint(
            latitude,
            longitude
        )

        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(point)

        mapView.overlays.removeAll {
            it is Marker
        }

        val marker = Marker(mapView).apply {
            position = point
            title = "Selected Location"

            setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            )
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    Column {
        AndroidView(
            factory = {
                mapView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )

        Text(
            text = "© OpenStreetMap contributors · © MapTiler",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
