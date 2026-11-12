package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.game.model.PlayerState
import com.example.game.model.WeaponType
import kotlin.math.hypot

@Composable
fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 110f

    Box(
        modifier = modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(Color(0x55000000))
            .border(2.dp, Color(0x66FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val distance = hypot(newOffset.x, newOffset.y)
                        thumbOffset = if (distance > maxRadiusPx) {
                            Offset(
                                (newOffset.x / distance) * maxRadiusPx,
                                (newOffset.y / distance) * maxRadiusPx
                            )
                        } else {
                            newOffset
                        }

                        // Normalize to -1..1 (inverted Y because screen down is positive)
                        val normX = (thumbOffset.x / maxRadiusPx).coerceIn(-1f, 1f)
                        val normY = -(thumbOffset.y / maxRadiusPx).coerceIn(-1f, 1f)
                        onMove(normX, normY)
                    }
                )
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val thumbCenter = center + thumbOffset

            // Inner stick knob
            drawCircle(
                color = Color(0xAAFFFFFF),
                radius = 28f,
                center = thumbCenter
            )
            drawCircle(
                color = Color(0xFFFFB02E),
                radius = 12f,
                center = thumbCenter
            )
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    subLabel: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorHex: Long = 0xFFFFB02E,
    sizeDp: Int = 62,
    testTag: String,
    onClick: () -> Unit,
    onPressState: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(colorHex).copy(alpha = 0.45f))
            .border(2.5.dp, Color(colorHex), CircleShape)
            .then(
                if (onPressState != null) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onPressState(true) },
                            onDragEnd = { onPressState(false) },
                            onDragCancel = { onPressState(false) },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, radius = (sizeDp * 0.5f).dp)
                    ) {
                        onClick()
                    }
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, radius = (sizeDp * 0.5f).dp)
                    ) {
                        onClick()
                    }
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size((sizeDp * 0.42f).dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = if (sizeDp > 70) 13.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    color = Color(0xFFFFE082),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
