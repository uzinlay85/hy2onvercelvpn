package com.safenet.vpn.presentation.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Theme Colors (Dark Mode Glassmorphism)
    val bgColor = Color(0xFF0F172A) // Slate 900
    val accentColor = Color(0xFF06B6D4) // Cyan 500
    val secondaryAccent = Color(0xFF8B5CF6) // Violet 500
    val surfaceColor = Color(0xFF1E293B).copy(alpha = 0.7f) // Glassy Slate 800

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("SafeNet Free", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(bgColor, Color(0xFF000000))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Connection Status Text
                Text(
                    text = when(uiState.vpnState) {
                        VpnState.CONNECTED -> "CONNECTED"
                        VpnState.CONNECTING -> "CONNECTING..."
                        VpnState.DISCONNECTED -> "NOT CONNECTED"
                        else -> "DISCONNECTED"
                    },
                    color = when(uiState.vpnState) {
                        VpnState.CONNECTED -> Color(0xFF10B981) // Emerald 500
                        VpnState.CONNECTING -> accentColor
                        else -> Color.Gray
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(40.dp))

                // Big Connect/Disconnect Button
                ConnectButton(
                    vpnState = uiState.vpnState,
                    accentColor = accentColor,
                    secondaryAccent = secondaryAccent,
                    onClick = {
                        if (uiState.vpnState == VpnState.CONNECTED || uiState.vpnState == VpnState.CONNECTING) {
                            viewModel.disconnect()
                        } else {
                            viewModel.connect()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(60.dp))

                // Server Selector Dropdown
                ServerSelector(
                    surfaceColor = surfaceColor,
                    accentColor = accentColor,
                    selectedServer = uiState.selectedServerName,
                    onClick = { showServerSelector = true }
                )
            }
        }

        // Server Selection Bottom Sheet
        if (showServerSelector) {
            ModalBottomSheet(
                onDismissRequest = { showServerSelector = false },
                containerColor = Color(0xFF1E293B),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Select Free Server",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (uiState.availableServers.isEmpty()) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        uiState.availableServers.forEach { server ->
                            ServerItem(
                                name = server.name,
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
    accentColor: Color,
    secondaryAccent: Color,
    onClick: () -> Unit
) {
    val isConnected = vpnState == VpnState.CONNECTED
    val isConnecting = vpnState == VpnState.CONNECTING

    // Animations
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isConnected || isConnecting) 60f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF10B981) else if (isConnecting) accentColor else Color(0xFF334155),
        animationSpec = tween(500)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Pulse effect
        if (isConnected || isConnecting) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(buttonColor.copy(alpha = pulseAlpha), Color.Transparent)
                    ),
                    radius = size.minDimension / 2 + pulseRadius
                )
            }
        }

        // Main Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .shadow(
                    elevation = if (isConnected) 20.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = buttonColor
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (isConnected) listOf(Color(0xFF34D399), Color(0xFF059669)) 
                                 else listOf(buttonColor, buttonColor.copy(alpha = 0.8f))
                    )
                )
                .clickable { onClick() }
        ) {
            // Inner ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = size.minDimension / 2 - 10f,
                    style = Stroke(width = 4f)
                )
            }
            
            // Icon or Text
            Text(
                text = if (isConnected) "STOP" else "START",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun ServerSelector(
    surfaceColor: Color,
    accentColor: Color,
    selectedServer: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Current Server", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(selectedServer, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Server", tint = accentColor)
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF334155) else Color.Transparent)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(protocol, color = Color.Gray, fontSize = 12.sp)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF06B6D4)) // Cyan
            )
        }
    }
}
