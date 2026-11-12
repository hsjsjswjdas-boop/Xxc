package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameScreenState
import com.example.game.GameViewModel
import com.example.game.model.WeaponType
import com.example.game.renderer.Render3D

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val render3D = remember { Render3D() }

    BackHandler {
        if (uiState.screenState == GameScreenState.PLAYING) {
            viewModel.pauseGame()
        } else if (uiState.screenState == GameScreenState.PAUSED) {
            viewModel.resumeGame()
        } else {
            viewModel.returnToLobby()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("game_screen_container")
    ) {
        // 1. 3D Game Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_3d_canvas")
        ) {
            render3D.renderScene(
                drawScope = this,
                player = uiState.player,
                bots = uiState.bots,
                projectiles = uiState.projectiles,
                particles = uiState.particles,
                airdrop = uiState.airdrop,
                lootItems = uiState.lootItems,
                buildings = uiState.buildings,
                safeZone = uiState.safeZone,
                isVictory = uiState.screenState == GameScreenState.VICTORY,
                isBengali = uiState.isBengali,
                victoryTime = uiState.victoryTimer
            )
        }

        // 2. Camera Touch Drag Area (Right 65% of screen for smooth aiming)
        if (uiState.screenState == GameScreenState.PLAYING) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.7f)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                viewModel.rotateCamera(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
                    .testTag("camera_aim_touch_area")
            )
        }

        // 3. Sniper Scope Overlay (if aiming with sniper)
        if (uiState.player.isAiming && uiState.player.currentWeapon == WeaponType.SNIPER) {
            SniperScopeOverlay()
        } else if (uiState.screenState == GameScreenState.PLAYING) {
            // Standard Tactical Crosshair
            TacticalCrosshair(isAiming = uiState.player.isAiming)
        }

        // 4. Hit Flash / Damage Vignette
        if (uiState.player.hitFlashTimer > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0x66FF1744)),
                            radius = 900f
                        )
                    )
            )
        }

        // 5. Game HUD (Top Bar, Minimap, Kill Feed, Health & Ammo, Controls)
        if (uiState.screenState == GameScreenState.PLAYING || uiState.screenState == GameScreenState.PAUSED) {
            GameHudOverlay(viewModel = viewModel)
        }

        // 6. Pause Dialog
        if (uiState.screenState == GameScreenState.PAUSED) {
            PauseMenuDialog(
                isBengali = uiState.isBengali,
                onResume = { viewModel.resumeGame() },
                onLobby = { viewModel.returnToLobby() }
            )
        }

        // 7. Victory Screen
        if (uiState.screenState == GameScreenState.VICTORY) {
            VictoryOverlay(
                kills = uiState.player.kills,
                damage = uiState.player.damageDealt,
                survivalTime = uiState.player.survivalTime,
                isBengali = uiState.isBengali,
                onPlayAgain = { viewModel.startMatch(uiState.totalPlayers) },
                onLobby = { viewModel.returnToLobby() }
            )
        }

        // 8. Game Over Screen
        if (uiState.screenState == GameScreenState.GAME_OVER) {
            GameOverOverlay(
                placement = uiState.playerPlacement,
                totalPlayers = uiState.totalPlayers,
                kills = uiState.player.kills,
                damage = uiState.player.damageDealt,
                isBengali = uiState.isBengali,
                onPlayAgain = { viewModel.startMatch(uiState.totalPlayers) },
                onLobby = { viewModel.returnToLobby() }
            )
        }
    }
}

@Composable
fun TacticalCrosshair(isAiming: Boolean) {
    val size = if (isAiming) 18.dp else 28.dp
    val color = if (isAiming) Color(0xFFFF5252) else Color(0xEEFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
            val gap = if (isAiming) 4f else 8f
            val len = if (isAiming) 10f else 14f

            // Top, Bottom, Left, Right crosshair ticks
            drawLine(color, Offset(center.x, center.y - gap), Offset(center.x, center.y - gap - len), strokeWidth = 2.5f)
            drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + gap + len), strokeWidth = 2.5f)
            drawLine(color, Offset(center.x - gap, center.y), Offset(center.x - gap - len, center.y), strokeWidth = 2.5f)
            drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + gap + len, center.y), strokeWidth = 2.5f)

            // Center dot
            drawCircle(Color(0xFFFFB02E), radius = 2.5f, center = center)
        }
    }
}

@Composable
fun SniperScopeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val scopeRadius = size.height * 0.42f

            // Outer vignette
            drawCircle(
                color = Color(0xAA000000),
                radius = scopeRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 300f)
            )

            // Scope reticle cross
            drawLine(Color(0xDD00E5FF), Offset(center.x - scopeRadius, center.y), Offset(center.x + scopeRadius, center.y), strokeWidth = 2f)
            drawLine(Color(0xDD00E5FF), Offset(center.x, center.y - scopeRadius), Offset(center.x, center.y + scopeRadius), strokeWidth = 2f)

            // Range hash marks
            for (i in 1..4) {
                val offset = i * 40f
                drawLine(Color(0xDD00E5FF), Offset(center.x - 15f, center.y + offset), Offset(center.x + 15f, center.y + offset), strokeWidth = 2f)
            }
        }
    }
}

@Composable
fun GameHudOverlay(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isBn = uiState.isBengali
    val player = uiState.player

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP ROW: Pause, Stats badges, Location, Minimap
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Top: Pause button + Stats
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.pauseGame() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x77000000))
                        .testTag("pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Alive & Kills pills
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x99173B55)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF5EE27A), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "${uiState.aliveCount} জন অবশিষ্ট" else "${uiState.aliveCount} Alive",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x99173B55)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CrisisAlert, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "${player.kills} নিহত" else "${player.kills} Kills",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Zone countdown
                    Spacer(modifier = Modifier.height(4.dp))
                    val waveText = if (isBn) "ওয়েভ ${uiState.safeZone.wave}: ${uiState.safeZone.countdownSec.toInt()}সে."
                    else "Wave ${uiState.safeZone.wave}: ${uiState.safeZone.countdownSec.toInt()}s"
                    Text(
                        text = waveText,
                        color = if (uiState.safeZone.isShrinking) Color(0xFFFF5252) else Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center Top: Current Location Badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xAA173B55),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x88FFB02E))
            ) {
                Text(
                    text = uiState.locationName,
                    color = Color(0xFFFFF6D8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Right Top: Minimap radar
            Minimap(
                player = player,
                bots = uiState.bots,
                safeZone = uiState.safeZone,
                airdrop = uiState.airdrop
            )
        }

        // Storm warning alert banner
        if (uiState.stormWarning != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xDDF44336),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = uiState.stormWarning!!,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Kill Feed (subtle floating announcements)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(vertical = 4.dp)
        ) {
            uiState.killFeed.takeLast(3).forEach { item ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x77000000),
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    val feedText = if (item.isPlayerKiller) {
                        "🎯 ${item.killer} ➔ ${item.victim} [${item.weaponName}]"
                    } else {
                        "☠️ ${item.killer} ➔ ${item.victim}"
                    }
                    Text(
                        text = feedText,
                        color = if (item.isPlayerKiller) Color(0xFFFFD54F) else Color(0xCCFFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // BOTTOM CONTROLS & BARS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // BOTTOM-LEFT: Health bar + Shield bar + Virtual Joystick
            Column {
                // Health Bar (HP)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF5EE27A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x88000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(player.health / player.maxHealth)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF00E676), Color(0xFF5EE27A))))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${player.health.toInt()}", color = Color(0xFF5EE27A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Shield Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00B0FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x88000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(player.shield / player.maxShield)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF0091EA), Color(0xFF00E5FF))))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${player.shield.toInt()}", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Virtual Joystick
                VirtualJoystick(
                    onMove = { x, y -> viewModel.setMoveInput(x, y) }
                )
            }

            // BOTTOM-RIGHT: Action buttons cluster
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Secondary row: Medkit, Bomb, Aim Zoom
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Heal Button
                    ActionButton(
                        label = if (isBn) "চিকিৎসা" else "HEAL",
                        icon = Icons.Default.MedicalServices,
                        colorHex = 0xFF5EE27A,
                        sizeDp = 52,
                        testTag = "heal_button",
                        onClick = { viewModel.useMedkit() }
                    )

                    // Bomb Button
                    ActionButton(
                        label = if (isBn) "বোমা" else "BOMB",
                        subLabel = "x${player.bombs}",
                        icon = Icons.Default.LocalFireDepartment,
                        colorHex = 0xFFFF7A5A,
                        sizeDp = 56,
                        testTag = "bomb_button",
                        onClick = { viewModel.throwBomb() }
                    )

                    // Aim Zoom / ADS Button
                    ActionButton(
                        label = if (isBn) "জুম" else "ADS",
                        icon = Icons.Default.ZoomIn,
                        colorHex = 0xFF00E5FF,
                        sizeDp = 52,
                        testTag = "aim_zoom_button",
                        onClick = { viewModel.setAiming(!player.isAiming) }
                    )
                }

                // Primary row: Switch Weapon, Jump, BIG Fire Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Switch Weapon Button with ammo info
                    ActionButton(
                        label = if (isBn) "অস্ত্র" else "SWAP",
                        subLabel = "${player.ammo}/${player.currentWeapon.maxAmmo}",
                        icon = Icons.Default.SwapHoriz,
                        colorHex = 0xFF5AD1FF,
                        sizeDp = 60,
                        testTag = "switch_weapon_button",
                        onClick = { viewModel.switchWeapon() }
                    )

                    // Jump Button
                    ActionButton(
                        label = if (isBn) "লাফ" else "JUMP",
                        icon = Icons.Default.ArrowUpward,
                        colorHex = 0xFFFFB02E,
                        sizeDp = 62,
                        testTag = "jump_button",
                        onClick = { viewModel.triggerJump() }
                    )

                    // Big Primary Fire Button
                    ActionButton(
                        label = if (player.isReloading) (if (isBn) "রিলোড" else "RELOAD") else (if (isBn) "গুলি" else "FIRE"),
                        icon = Icons.Default.Adjust,
                        colorHex = if (player.ammo <= 0) 0xFFFF1744 else 0xFFFFB02E,
                        sizeDp = 86,
                        testTag = "fire_button",
                        onClick = { },
                        onPressState = { pressed ->
                            if (pressed) viewModel.startFiring() else viewModel.stopFiring()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PauseMenuDialog(
    isBengali: Boolean,
    onResume: () -> Unit,
    onLobby: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xE6173B55),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFB02E)),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isBengali) "খেলা বিরতি" else "GAME PAUSED",
                    color = Color(0xFFFFF6D8),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB02E))
                ) {
                    Text(if (isBengali) "খেলা চালিয়ে যান" else "RESUME GAME", color = Color(0xFF173B55), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onLobby,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_lobby_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(if (isBengali) "লবিতে ফিরে যান" else "QUIT TO LOBBY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VictoryOverlay(
    kills: Int,
    damage: Float,
    survivalTime: Float,
    isBengali: Boolean,
    onPlayAgain: () -> Unit,
    onLobby: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF2173B55),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFD54F)),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isBengali) "বিজয়ী! শেষ বীর!" else "VICTORY! LAST SURVIVOR",
                    color = Color(0xFFFFD700),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (isBengali) "#১ চ্যাম্পিয়ন রণবীর" else "#1 CHAMPION",
                    color = Color(0xFFFFF6D8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x55000000))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "মোট নিধন (Kills):" else "Eliminations:", color = Color.White, fontSize = 12.sp)
                        Text("$kills", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "মোট ক্ষতি (Damage):" else "Damage Dealt:", color = Color.White, fontSize = 12.sp)
                        Text("${damage.toInt()}", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "বেঁচে থাকার সময়:" else "Survival Time:", color = Color.White, fontSize = 12.sp)
                        Text("${survivalTime.toInt()}s", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("victory_play_again_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB02E))
                ) {
                    Text(if (isBengali) "পুনরায় খেলুন" else "PLAY AGAIN", color = Color(0xFF173B55), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onLobby,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("victory_lobby_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(if (isBengali) "প্রধান লবি" else "MAIN LOBBY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    placement: Int,
    totalPlayers: Int,
    kills: Int,
    damage: Float,
    isBengali: Boolean,
    onPlayAgain: () -> Unit,
    onLobby: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF2173B55),
            border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFF5252)),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(50.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isBengali) "পরাজিত!" else "ELIMINATED",
                    color = Color(0xFFFF5252),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (isBengali) "অবস্থান: #$placement / $totalPlayers" else "Placed: #$placement / $totalPlayers",
                    color = Color(0xFFFFF6D8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x55000000))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "নিহত শত্রু:" else "Kills:", color = Color.White, fontSize = 12.sp)
                        Text("$kills", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "ক্ষয়ক্ষতি:" else "Damage:", color = Color.White, fontSize = 12.sp)
                        Text("${damage.toInt()}", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gameover_play_again_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB02E))
                ) {
                    Text(if (isBengali) "আবার চেষ্টা করুন" else "TRY AGAIN", color = Color(0xFF173B55), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onLobby,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gameover_lobby_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(if (isBengali) "লবিতে ফিরে যান" else "RETURN TO LOBBY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
