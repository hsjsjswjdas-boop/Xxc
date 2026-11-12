package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Minimap(
    player: PlayerState,
    bots: List<EnemyBot>,
    safeZone: SafeZone,
    airdrop: Airdrop?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(Color(0xCC102027))
            .border(2.dp, Color(0xAAFFFFFF), CircleShape)
            .testTag("minimap_radar")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val radarRadius = size.width * 0.5f
            val scale = radarRadius / 150f // map size is 300x300 (-150 to +150)

            // Radar rings
            drawCircle(Color(0x22FFFFFF), radius = radarRadius * 0.66f, center = center, style = Stroke(1f))
            drawCircle(Color(0x22FFFFFF), radius = radarRadius * 0.33f, center = center, style = Stroke(1f))

            // Safe zone boundary
            val szOffsetX = center.x + (safeZone.centerX - player.position.x) * scale
            val szOffsetZ = center.y + (safeZone.centerZ - player.position.z) * scale
            val szRadiusPx = safeZone.currentRadius * scale
            val szColor = if (safeZone.isShrinking) Color(0x88FF3D00) else Color(0x8800E5FF)
            drawCircle(szColor, radius = szRadiusPx, center = Offset(szOffsetX, szOffsetZ), style = Stroke(2f))

            // Airdrop icon
            if (airdrop != null && !airdrop.isLooted) {
                val adX = center.x + (airdrop.position.x - player.position.x) * scale
                val adZ = center.y + (airdrop.position.z - player.position.z) * scale
                val d = Offset(adX, adZ)
                if ((d - center).getDistance() < radarRadius) {
                    drawCircle(Color(0xFFFF1744), radius = 5f, center = d)
                }
            }

            // Enemy bots dots
            for (bot in bots) {
                if (!bot.isDead) {
                    val bx = center.x + (bot.position.x - player.position.x) * scale
                    val bz = center.y + (bot.position.z - player.position.z) * scale
                    val pos = Offset(bx, bz)
                    if ((pos - center).getDistance() < radarRadius) {
                        drawCircle(Color(0xFFFF5252), radius = 4f, center = pos)
                    }
                }
            }

            // Player arrow at center pointing in yaw direction
            val arrowLength = 9f
            val px = center.x
            val py = center.y
            val yaw = player.yaw
            val tip = Offset(px + sin(yaw) * arrowLength, py + cos(yaw) * arrowLength)
            val left = Offset(px + sin(yaw + 2.5f) * (arrowLength * 0.7f), py + cos(yaw + 2.5f) * (arrowLength * 0.7f))
            val right = Offset(px + sin(yaw - 2.5f) * (arrowLength * 0.7f), py + cos(yaw - 2.5f) * (arrowLength * 0.7f))

            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(px, py)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(path, color = Color(0xFFFFD54F))
        }
    }
}
