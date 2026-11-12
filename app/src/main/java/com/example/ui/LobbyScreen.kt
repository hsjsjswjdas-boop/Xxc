package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CareerStats
import com.example.game.GameViewModel
import com.example.game.model.SkinType
import com.example.game.model.WeaponType

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBn = uiState.isBengali
    var selectedPlayerCount by remember { mutableStateOf(25) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2636), Color(0xFF173B55), Color(0xFF0D1B2A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lobby_screen_root")
    ) {
        // Decorative background geometric grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 45f
            for (x in 0..(size.width / step).toInt()) {
                drawLine(Color(0x0AFFFFFF), Offset(x * step, 0f), Offset(x * step, size.height), strokeWidth = 1f)
            }
            for (y in 0..(size.height / step).toInt()) {
                drawLine(Color(0x0AFFFFFF), Offset(0f, y * step), Offset(size.width, y * step), strokeWidth = 1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Toggle Pill
                Button(
                    onClick = { viewModel.toggleLanguage() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("language_toggle_button")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFFFB02E), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "বাংলা / EN" else "EN / বাংলা", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Sound & Settings
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (uiState.soundEnabled) Color(0xFFFFB02E) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // HERO TITLE SECTION
            Text(
                text = if (isBn) "শেষ বাঁচা 3D" else "SHESH BACHA 3D",
                color = Color(0xFFFFD24A),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isBn) "ব্যাটল রয়্যাল সারভাইভাল এরিনা" else "3D Battle Royale Survival Arena",
                color = Color(0xFFFFF6D8).copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SURVIVOR AVATAR / BADGE DISPLAY
            SurvivorAvatarCard(
                skin = uiState.player.skin,
                isBengali = isBn
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SKIN SELECTION CHIPS
            Text(
                text = if (isBn) "যোদ্ধা চরিত্র নির্বাচন করুন" else "Select Survivor Skin",
                color = Color(0xFFFFF6D8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkinType.values().forEach { skin ->
                    val isSelected = uiState.player.skin == skin
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(skin.primaryColor) else Color(0x33FFFFFF),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(skin.accentColor)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setPlayerSkin(skin) }
                            .testTag("skin_select_${skin.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(skin.accentColor))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = skin.getDisplayName(isBn),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // MATCH LOBBY SETTINGS: Survivor Count
            Text(
                text = if (isBn) "ম্যাচ সারভাইভার সংখ্যা" else "Match Lobby Size",
                color = Color(0xFFFFF6D8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(15 to (if (isBn) "১৫ জন (দ্রুত)" else "15 Quick"),
                    25 to (if (isBn) "২৫ জন (স্ট্যান্ডার্ড)" else "25 Standard"),
                    40 to (if (isBn) "৪০ জন (মেগা)" else "40 Mega")
                ).forEach { (count, label) ->
                    val isSelected = selectedPlayerCount == count
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFFFB02E) else Color(0x22FFFFFF),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPlayerCount = count }
                            .testTag("player_count_$count")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF173B55) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BIG START BATTLE BUTTON
            Button(
                onClick = { viewModel.startMatch(selectedPlayerCount) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB02E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_battle_button")
            ) {
                Icon(Icons.Default.SportsKabaddi, contentDescription = null, tint = Color(0xFF173B55), modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isBn) "যুদ্ধ শুরু করুন (START)" else "ENTER BATTLEFIELD",
                    color = Color(0xFF173B55),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CAREER STATS SUMMARY
            CareerStatsCard(stats = uiState.careerStats, isBengali = isBn)

            Spacer(modifier = Modifier.height(16.dp))

            // ARSENAL WEAPONS PREVIEW
            ArsenalCard(isBengali = isBn)

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun SurvivorAvatarCard(
    skin: SkinType,
    isBengali: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x44000000),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x44FFB02E)),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized 3D Survivor Silhouette Badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(skin.primaryColor), Color(skin.secondaryColor))))
                    .border(2.dp, Color(skin.accentColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(skin.accentColor),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = skin.getDisplayName(isBengali),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBengali) "প্রস্তুত রণবীর • অস্ত্রাগার সজ্জিত" else "Ready Combatant • Armed & Loaded",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBengali) "১০০ স্বাস্থ্য • ৫০ বর্ম শিল্ড • ৩টি বোমা" else "100 HP • 50 Armor • 3 Frag Bombs",
                        color = Color(0xCCFFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CareerStatsCard(
    stats: CareerStats,
    isBengali: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33173B55),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "ক্যারিয়ার রেকর্ড ও পরিসংখ্যান" else "Career Records & Combat Stats",
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = if (isBengali) "মোট ম্যাচ" else "Matches", value = "${stats.matchesPlayed}")
                StatItem(label = if (isBengali) "বিজয়ী (#১)" else "Victories", value = "${stats.totalWins}", highlight = true)
                StatItem(label = if (isBengali) "মোট নিধন" else "Total Kills", value = "${stats.totalKills}")
                StatItem(label = if (isBengali) "সর্বোচ্চ নিধন" else "Best Kills", value = "${stats.highestKills}")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = if (highlight) Color(0xFFFFD54F) else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = Color(0xAAFFFFFF),
            fontSize = 10.sp
        )
    }
}

@Composable
fun ArsenalCard(isBengali: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33173B55),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFFFB02E), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "যুদ্ধক্ষেত্রের অস্ত্রাগার" else "Available Battle Arsenal",
                    color = Color(0xFFFFB02E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            WeaponType.values().forEach { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(w.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(w.getDisplayName(isBengali), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = if (isBengali) "ক্ষতি: ${w.damage.toInt()} | ম্যাগ: ${w.maxAmmo}" else "DMG: ${w.damage.toInt()} | Mag: ${w.maxAmmo}",
                        color = Color(0xAAFFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBn = uiState.isBengali

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB02E))
            ) {
                Text(if (isBn) "সম্পন্ন" else "DONE", color = Color(0xFF173B55), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(if (isBn) "গেম সেটিংস" else "Game Settings", color = Color(0xFFFFF6D8), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Sound switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isBn) "শব্দ ও সাউন্ড ইফেক্ট" else "Sound Effects (SFX)", color = Color.White)
                    Switch(
                        checked = uiState.soundEnabled,
                        onCheckedChange = { viewModel.toggleSound() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB02E))
                    )
                }

                // Vibration switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isBn) "কম্পন / হ্যাপটিক ফিডব্যাক" else "Haptic Vibration", color = Color.White)
                    Switch(
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { viewModel.toggleVibration() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB02E))
                    )
                }

                // Sensitivity slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isBn) "ক্যামেরা নিয়ন্ত্রণ সংবেদনশীলতা" else "Aim Sensitivity", color = Color.White)
                        Text("${String.format("%.1f", uiState.sensitivity)}x", color = Color(0xFFFFB02E), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.sensitivity,
                        onValueChange = { viewModel.setSensitivity(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB02E), activeTrackColor = Color(0xFFFFB02E))
                    )
                }
            }
        },
        containerColor = Color(0xF5173B55),
        shape = RoundedCornerShape(16.dp)
    )
}
