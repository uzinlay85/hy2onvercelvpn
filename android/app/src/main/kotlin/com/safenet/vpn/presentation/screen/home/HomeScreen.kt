package com.safenet.vpn.presentation.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safenet.vpn.MainActivity
import com.safenet.vpn.presentation.viewmodel.HomeViewModel
import com.safenet.vpn.domain.model.VpnState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showServerSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Handle VPN permission request from ViewModel
    LaunchedEffect(uiState.needsVpnPermission) {
        if (uiState.needsVpnPermission) {
            val activity = context as? MainActivity ?: return@LaunchedEffect
            val intent = android.net.VpnService.prepare(context) ?: return@LaunchedEffect
            activity.onVpnPermissionResult = { granted ->
                if (granted) viewModel.onVpnPermissionGranted()
                else viewModel.onVpnPermissionDenied()
            }
            activity.vpnPermissionLauncher.launch(intent)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Zin SafeNet V2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Top Section: Error Messages if any
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { viewModel.fetchServers() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Retry")
                    }
                }
            }

            // Middle Section: The Giant Connect Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                ConnectButton(
                    vpnState = uiState.vpnState,
                    onClick = {
                        if (uiState.vpnState == VpnState.CONNECTED || uiState.vpnState == VpnState.CONNECTING) {
                            viewModel.disconnect()
                        } else {
                            viewModel.connect()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Connection Status Text
                Text(
                    text = when(uiState.vpnState) {
                        VpnState.CONNECTED -> "Your connection is protected"
                        VpnState.CONNECTING -> "Connecting..."
                        VpnState.DISCONNECTED -> "Not connected"
                        else -> "Disconnected"
                    },
                    color = when(uiState.vpnState) {
                        VpnState.CONNECTED -> MaterialTheme.colorScheme.tertiary
                        VpnState.CONNECTING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bottom Section: Server Selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ServerSelector(
                    selectedServer = uiState.selectedServerName,
                    onClick = { showServerSelector = true }
                )
            }
        }

        // Server Selection Bottom Sheet
        if (showServerSelector) {
            ModalBottomSheet(
                onDismissRequest = { showServerSelector = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Select Server",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (uiState.availableServers.isEmpty() && uiState.errorMessage == null) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (uiState.availableServers.isEmpty() && uiState.errorMessage != null) {
                        Text("No servers available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.availableServers.forEach { server ->
                            ServerItem(
                                name = server.name.replace(" Free", "").replace(" Premium", ""),
                                protocol = server.protocol,
                                isSelected = uiState.selectedServerId == server.id,
                                onClick = {
                                    viewModel.selectServer(server.id)
                                    showServerSelector = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectButton(
    vpnState: VpnState,
    onClick: () -> Unit
) {
    val isConnected = vpnState == VpnState.CONNECTED
    val isConnecting = vpnState == VpnState.CONNECTING

    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.tertiary // Green
            isConnecting -> MaterialTheme.colorScheme.primary // Vibrant Teal
            else -> MaterialTheme.colorScheme.surfaceVariant // Gray/Dark Blue
        },
        animationSpec = tween(500)
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isConnected || isConnecting) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(500)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .shadow(
                elevation = if (isConnected || isConnecting) 24.dp else 8.dp,
                shape = CircleShape,
                spotColor = buttonColor
            )
            .clip(CircleShape)
            .background(buttonColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Inner circle for Amnezia look
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Power",
                tint = iconTint,
                modifier = Modifier.size(64.dp * pulseScale)
            )
        }
    }
}

@Composable
fun ServerSelector(
    selectedServer: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(selectedServer.ifEmpty { "Select Server" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Tap to change location", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ServerItem(
    name: String,
    protocol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(protocol, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
