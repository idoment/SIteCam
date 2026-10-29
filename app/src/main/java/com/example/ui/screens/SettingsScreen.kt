package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.config.AppConfig
import com.example.model.AppSettings
import com.example.model.UserSession
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.GpsGreen
import com.example.ui.theme.GpsRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    user: UserSession,
    settings: AppSettings,
    siteLocation: String,
    allEngineersLocations: List<String>,
    onSiteLocationChange: (String) -> Unit,
    onUpdateProfile: (name: String, phone: String) -> Unit,
    onSettingsUpdated: (AppSettings) -> Unit,
    onSignOut: () -> Unit,
    onClearAllData: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var locationDropdownExpanded by remember { mutableStateOf(false) }

    // User Profile Edit State
    var engineerNameInput by remember(user.displayName) { mutableStateOf(user.displayName) }
    var phoneNumberInput by remember(user.phoneNumber) { mutableStateOf(user.phoneNumber) }
    var profileSaveSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyPrimary)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Account Section (with editable Name and Phone Number)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Engineer Profile & Contact", Icons.Default.AccountCircle)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, AmberAccent, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(AmberAccent.copy(alpha = 0.2f))
                                    .border(2.dp, AmberAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayName.take(1).uppercase(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = user.email.ifBlank { "Signed in with Google" },
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            if (user.phoneNumber.isNotBlank()) {
                                Text(
                                    text = "Tel: ${user.phoneNumber}",
                                    fontSize = 12.sp,
                                    color = AmberAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (user.isTestMode) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AmberAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TEST MODE SESSION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Editable Name Input
                    OutlinedTextField(
                        value = engineerNameInput,
                        onValueChange = { engineerNameInput = it },
                        label = { Text("Engineer Full Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engineer_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = BorderOutline,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = AmberAccent,
                            unfocusedLabelColor = TextMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable Phone Number Input
                    OutlinedTextField(
                        value = phoneNumberInput,
                        onValueChange = { phoneNumberInput = it },
                        label = { Text("Contact Phone Number") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engineer_phone_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = BorderOutline,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = AmberAccent,
                            unfocusedLabelColor = TextMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Save Profile Button
                    Button(
                        onClick = {
                            onUpdateProfile(engineerNameInput, phoneNumberInput)
                            profileSaveSuccess = true
                            coroutineScope.launch {
                                delay(2500L)
                                profileSaveSuccess = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (profileSaveSuccess) GpsGreen else AmberAccent,
                            contentColor = NavyDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (profileSaveSuccess) "Profile Details Saved!" else "Save Profile Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444)))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Site Location Section (Text field & Dropdown with all engineers saved locations)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Default Site Location", Icons.Default.LocationOn)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The site location below is stamped on all photos. Edit the field or tap the arrow to pick from all saved engineers' locations:",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = siteLocation,
                            onValueChange = onSiteLocationChange,
                            label = { Text("Site Location (Default)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_site_location_input"),
                            trailingIcon = {
                                IconButton(
                                    onClick = { locationDropdownExpanded = !locationDropdownExpanded },
                                    modifier = Modifier.testTag("site_location_dropdown_trigger")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Saved Locations Dropdown",
                                        tint = AmberAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberAccent,
                                unfocusedBorderColor = BorderOutline,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = AmberAccent,
                                unfocusedLabelColor = TextMuted
                            )
                        )

                        DropdownMenu(
                            expanded = locationDropdownExpanded,
                            onDismissRequest = { locationDropdownExpanded = false },
                            modifier = Modifier
                                .background(NavyDark)
                                .border(1.dp, BorderOutline)
                        ) {
                            if (allEngineersLocations.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "No saved locations yet",
                                            color = TextMuted,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = { locationDropdownExpanded = false }
                                )
                            } else {
                                allEngineersLocations.forEach { loc ->
                                    val isCurrentDefault = loc.equals(siteLocation.trim(), ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = loc,
                                                    color = if (isCurrentDefault) AmberAccent else TextWhite,
                                                    fontWeight = if (isCurrentDefault) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                if (isCurrentDefault) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "(Default)",
                                                        color = AmberAccent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSiteLocationChange(loc)
                                            locationDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Current Default: ${if (siteLocation.isNotBlank()) siteLocation else "None set"}",
                        fontSize = 11.sp,
                        color = AmberAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Data Management (Current User Only)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Data & Privacy", Icons.Default.DeleteForever)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Photos and GPS records are kept private to your account on this device. Clearing data deletes only your local records.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F1D1D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All My Local Photos", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. About Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${AppConfig.FULL_APP_TITLE} v1.0.0",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Powered by ")
                        withStyle(
                            style = SpanStyle(
                                color = AmberAccent,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(AppConfig.POWERED_BY_BRAND)
                        }
                    },
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Sign Out Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out", color = TextWhite) },
                text = { Text("Are you sure you want to sign out of MAXINFRA SiteCam?", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    TextButton(onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }) {
                        Text("Sign Out", color = AmberAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Clear Data Dialog
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear All Photos?", color = TextWhite) },
                text = {
                    Text(
                        "This will delete all site photos taken under account ${user.email} from this device. This cannot be undone.",
                        color = Color(0xFFCBD5E1)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDataDialog = false
                        onClearAllData(context)
                    }) {
                        Text("Clear All", color = GpsRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmberAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AmberAccent
        )
    }
}

