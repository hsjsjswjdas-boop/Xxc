package com.example.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.game.model.*
import kotlin.math.*

class Render3D {

    data class ScreenPoint(val x: Float, val y: Float, val z: Float, val visible: Boolean)

    // Project 3D world coordinates to 2D screen coordinates
    fun project(
        wx: Float, wy: Float, wz: Float,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        screenWidth: Float, screenHeight: Float,
        fov: Float = 68f
    ): ScreenPoint {
        val dx = wx - camX
        val dy = wy - camY
        val dz = wz - camZ

        // Yaw rotation
        val cosY = cos(yaw)
        val sinY = sin(yaw)
        val rx = dx * cosY - dz * sinY
        val rz = dx * sinY + dz * cosY

        // Pitch rotation
        val cosP = cos(pitch)
        val sinP = sin(pitch)
        val ry = dy * cosP - rz * sinP
        val finalZ = dy * sinP + rz * cosP

        if (finalZ <= 0.4f) {
            return ScreenPoint(0f, 0f, finalZ, false)
        }

        val fovRad = Math.toRadians(fov.toDouble()).toFloat()
        val fovFactor = (screenWidth * 0.5f) / tan(fovRad * 0.5f)

        val sx = screenWidth * 0.5f + (rx / finalZ) * fovFactor
        val sy = screenHeight * 0.5f - (ry / finalZ) * fovFactor

        return ScreenPoint(sx, sy, finalZ, true)
    }

    // Render entire 3D scene onto Compose Canvas DrawScope
    fun renderScene(
        drawScope: DrawScope,
        player: PlayerState,
        bots: List<EnemyBot>,
        projectiles: List<Projectile>,
        particles: List<Particle>,
        airdrop: Airdrop?,
        lootItems: List<LootItem>,
        buildings: List<WorldBuilding>,
        safeZone: SafeZone,
        isVictory: Boolean,
        isBengali: Boolean,
        victoryTime: Float
    ) {
        val canvasWidth = drawScope.size.width
        val canvasHeight = drawScope.size.height
        if (canvasWidth <= 0 || canvasHeight <= 0) return

        // Camera setup: Third-person shoulder view, or orbit camera in victory
        val camX: Float
        val camY: Float
        val camZ: Float
        val camYaw: Float
        val camPitch: Float

        if (isVictory) {
            val orbitAngle = victoryTime * 0.6f
            val orbitRadius = 7.5f
            camX = player.position.x + sin(orbitAngle) * orbitRadius
            camY = player.position.y + 2.8f + sin(victoryTime * 0.8f) * 0.3f
            camZ = player.position.z + cos(orbitAngle) * orbitRadius
            camYaw = orbitAngle + Math.PI.toFloat()
            camPitch = -0.15f
        } else if (player.isAiming && player.currentWeapon == WeaponType.SNIPER) {
            // First person sniper view
            camX = player.position.x
            camY = player.position.y + 1.6f
            camZ = player.position.z
            camYaw = player.yaw
            camPitch = player.pitch
        } else {
            // Over-the-shoulder 3rd person
            val camDistance = if (player.isAiming) 2.2f else 3.8f
            val camHeight = if (player.isAiming) 1.8f else 2.1f
            val shoulderOffset = 0.65f // Right shoulder offset

            val cosY = cos(player.yaw)
            val sinY = sin(player.yaw)

            camX = player.position.x - sinY * camDistance + cosY * shoulderOffset
            camY = player.position.y + camHeight + sin(player.pitch) * 0.8f
            camZ = player.position.z - cosY * camDistance - sinY * shoulderOffset
            camYaw = player.yaw
            camPitch = player.pitch
        }

        // 1. Draw Sky & Atmosphere
        drawSky(drawScope, camPitch, isVictory)

        // 2. Draw Horizon Mountains & Terrain
        drawTerrain(drawScope, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)

        // 3. Draw Safe Zone Boundary (glowing cylinder wall)
        drawSafeZoneWall(drawScope, safeZone, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)

        // 4. Collect 3D Renderables for Z-Sorting (Painters Algorithm)
        val renderList = mutableListOf<Renderable>()

        // Add buildings
        for (b in buildings) {
            val dist = Vector3(b.position.x, b.position.y, b.position.z).distanceTo(Vector3(camX, camY, camZ))
            renderList.add(Renderable(dist) {
                drawBuilding(drawScope, b, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)
            })
        }

        // Add Loot items
        for (loot in lootItems) {
            if (!loot.isCollected) {
                val dist = loot.position.distanceTo(Vector3(camX, camY, camZ))
                renderList.add(Renderable(dist) {
                    drawLootItem(drawScope, loot, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)
                })
            }
        }

        // Add Airdrop crate
        if (airdrop != null && !airdrop.isLooted) {
            val dist = airdrop.position.distanceTo(Vector3(camX, camY, camZ))
            renderList.add(Renderable(dist) {
                drawAirdrop(drawScope, airdrop, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)
            })
        }

        // Add Bots
        for (bot in bots) {
            if (!bot.isDead || bot.deathTimer < 2.5f) {
                val dist = bot.position.distanceTo(Vector3(camX, camY, camZ))
                renderList.add(Renderable(dist) {
                    drawBot(drawScope, bot, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight, isBengali)
                })
            }
        }

        // Add Player Character (in 3rd person)
        if (!player.isAiming || player.currentWeapon != WeaponType.SNIPER) {
            val dist = player.position.distanceTo(Vector3(camX, camY, camZ))
            renderList.add(Renderable(dist) {
                drawSurvivorPlayer(drawScope, player, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight, isVictory)
            })
        }

        // Add Projectiles
        for (proj in projectiles) {
            val dist = proj.position.distanceTo(Vector3(camX, camY, camZ))
            renderList.add(Renderable(dist) {
                drawProjectile(drawScope, proj, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)
            })
        }

        // Add Particles & Explosions
        for (p in particles) {
            val dist = p.position.distanceTo(Vector3(camX, camY, camZ))
            renderList.add(Renderable(dist) {
                drawParticle(drawScope, p, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight)
            })
        }

        // If victory, add golden trophy
        if (isVictory) {
            val trophyPos = Vector3(player.position.x, player.position.y + 1f, player.position.z + 1.8f)
            val dist = trophyPos.distanceTo(Vector3(camX, camY, camZ))
            renderList.add(Renderable(dist) {
                drawVictoryTrophy(drawScope, trophyPos, camX, camY, camZ, camYaw, camPitch, canvasWidth, canvasHeight, victoryTime)
            })
        }

        // Sort back to front (largest distance first)
        renderList.sortByDescending { it.depth }

        // Render all depth-sorted objects
        for (item in renderList) {
            item.render()
        }
    }

    private class Renderable(val depth: Float, val render: () -> Unit)

    // --- SKY RENDERING ---
    private fun drawSky(drawScope: DrawScope, pitch: Float, isVictory: Boolean) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val horizonY = height * 0.5f + pitch * (height * 0.8f)

        val topSkyColor = if (isVictory) Color(0xFFC0623A) else Color(0xFF388E3C)
        val bottomSkyColor = if (isVictory) Color(0xFFF2C27A) else Color(0xFF81D4FA)

        // Gradient sky
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = if (isVictory) listOf(Color(0xFFE65100), Color(0xFFFFB74D), Color(0xFFFFF9C4))
                else listOf(Color(0xFF0D47A1), Color(0xFF42A5F5), Color(0xFFB3E5FC)),
                startY = 0f,
                endY = horizonY.coerceAtLeast(0f)
            ),
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY.coerceIn(0f, height))
        )

        // Sun / Amber glow
        val sunX = width * 0.65f
        val sunY = (horizonY - height * 0.28f).coerceAtLeast(height * 0.05f)
        val sunColor = if (isVictory) Color(0xFFFFD54F) else Color(0xFFFFF9C4)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(sunColor, sunColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(sunX, sunY),
                radius = 80f
            ),
            radius = 80f,
            center = Offset(sunX, sunY)
        )
    }

    // --- TERRAIN & GRID RENDERING ---
    private fun drawTerrain(
        drawScope: DrawScope,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val horizonY = height * 0.5f + pitch * (height * 0.8f)

        // Ground base polygon
        val groundColor1 = Color(0xFF2E4829)
        val groundColor2 = Color(0xFF1E321B)
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundColor1, groundColor2),
                startY = horizonY.coerceAtLeast(0f),
                endY = height
            ),
            topLeft = Offset(0f, horizonY.coerceIn(0f, height)),
            size = Size(width, height - horizonY.coerceIn(0f, height))
        )

        // Perspective grid lines on terrain ground (y = 0)
        val gridStep = 8f
        val playerGridX = (camX / gridStep).toInt() * gridStep
        val playerGridZ = (camZ / gridStep).toInt() * gridStep
        val range = 10

        val gridLineColor = Color(0x28FFFFFF)

        // Longitudinal grid lines
        for (gx in -range..range) {
            val worldX = playerGridX + gx * gridStep
            val p1 = project(worldX, 0f, playerGridZ - range * gridStep, camX, camY, camZ, yaw, pitch, width, height)
            val p2 = project(worldX, 0f, playerGridZ + range * gridStep, camX, camY, camZ, yaw, pitch, width, height)
            if (p1.visible && p2.visible) {
                drawScope.drawLine(
                    color = gridLineColor,
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = (1.5f * (width / 400f)).coerceAtLeast(1f)
                )
            }
        }

        // Latitudinal grid lines
        for (gz in -range..range) {
            val worldZ = playerGridZ + gz * gridStep
            val p1 = project(playerGridX - range * gridStep, 0f, worldZ, camX, camY, camZ, yaw, pitch, width, height)
            val p2 = project(playerGridX + range * gridStep, 0f, worldZ, camX, camY, camZ, yaw, pitch, width, height)
            if (p1.visible && p2.visible) {
                drawScope.drawLine(
                    color = gridLineColor,
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = (1.5f * (width / 400f)).coerceAtLeast(1f)
                )
            }
        }
    }

    // --- SAFE ZONE CYLINDER WALL ---
    private fun drawSafeZoneWall(
        drawScope: DrawScope,
        safeZone: SafeZone,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val segments = 32
        val r = safeZone.currentRadius
        val wallColor = if (safeZone.isShrinking) Color(0x66FF3D00) else Color(0x5500E5FF)
        val wallTop = 18f

        var prevBottom: ScreenPoint? = null
        var prevTop: ScreenPoint? = null

        for (i in 0..segments) {
            val angle = (i % segments) * (2 * Math.PI.toFloat() / segments)
            val wx = safeZone.centerX + sin(angle) * r
            val wz = safeZone.centerZ + cos(angle) * r

            val pBot = project(wx, 0f, wz, camX, camY, camZ, yaw, pitch, width, height)
            val pTop = project(wx, wallTop, wz, camX, camY, camZ, yaw, pitch, width, height)

            if (prevBottom != null && prevTop != null && pBot.visible && pTop.visible && prevBottom.visible && prevTop.visible) {
                val path = Path().apply {
                    moveTo(prevBottom.x, prevBottom.y)
                    lineTo(pBot.x, pBot.y)
                    lineTo(pTop.x, pTop.y)
                    lineTo(prevTop.x, prevTop.y)
                    close()
                }
                drawScope.drawPath(path, color = wallColor)
                drawScope.drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(pBot.x, pBot.y),
                    end = Offset(pTop.x, pTop.y),
                    strokeWidth = 2f
                )
            }
            prevBottom = pBot
            prevTop = pTop
        }
    }

    // --- BUILDINGS & COVERS ---
    private fun drawBuilding(
        drawScope: DrawScope,
        b: WorldBuilding,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val px = b.position.x
        val py = b.position.y
        val pz = b.position.z

        when (b.type) {
            BuildingType.TREE -> {
                // 3D Tree: brown trunk + green conical foliage
                val trunkBase = project(px, py, pz, camX, camY, camZ, yaw, pitch, width, height)
                val trunkTop = project(px, py + b.height * 0.4f, pz, camX, camY, camZ, yaw, pitch, width, height)
                val foliageTop = project(px, py + b.height, pz, camX, camY, camZ, yaw, pitch, width, height)

                if (trunkBase.visible && trunkTop.visible && foliageTop.visible) {
                    val trunkThick = (18f / trunkBase.z).coerceIn(2f, 30f)
                    // Trunk
                    drawScope.drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(trunkBase.x, trunkBase.y),
                        end = Offset(trunkTop.x, trunkTop.y),
                        strokeWidth = trunkThick
                    )
                    // Foliage 3-tier cone
                    val foliageRadius = (b.width * 28f / trunkTop.z).coerceIn(6f, 90f)
                    drawScope.drawCircle(
                        color = Color(0xFF2E7D32),
                        radius = foliageRadius,
                        center = Offset(trunkTop.x, trunkTop.y)
                    )
                    drawScope.drawCircle(
                        color = Color(0xFF388E3C),
                        radius = foliageRadius * 0.75f,
                        center = Offset(foliageTop.x, (trunkTop.y + foliageTop.y) * 0.5f)
                    )
                }
            }
            BuildingType.BUNKER, BuildingType.CRATE_STACK, BuildingType.WALL, BuildingType.WATCHTOWER -> {
                // 3D Box / Building
                val hw = b.width * 0.5f
                val hd = b.depth * 0.5f
                val bh = b.height

                // 8 corners
                val c0 = project(px - hw, py, pz - hd, camX, camY, camZ, yaw, pitch, width, height)
                val c1 = project(px + hw, py, pz - hd, camX, camY, camZ, yaw, pitch, width, height)
                val c2 = project(px + hw, py, pz + hd, camX, camY, camZ, yaw, pitch, width, height)
                val c3 = project(px - hw, py, pz + hd, camX, camY, camZ, yaw, pitch, width, height)

                val c4 = project(px - hw, py + bh, pz - hd, camX, camY, camZ, yaw, pitch, width, height)
                val c5 = project(px + hw, py + bh, pz - hd, camX, camY, camZ, yaw, pitch, width, height)
                val c6 = project(px + hw, py + bh, pz + hd, camX, camY, camZ, yaw, pitch, width, height)
                val c7 = project(px - hw, py + bh, pz + hd, camX, camY, camZ, yaw, pitch, width, height)

                val allVisible = c0.visible && c1.visible && c2.visible && c3.visible &&
                        c4.visible && c5.visible && c6.visible && c7.visible
                if (!allVisible && !c0.visible && !c2.visible) return

                val wallColor = Color(b.colorHex)
                val roofColor = Color(b.roofColorHex)

                // Draw roof if camera is above
                if (c4.visible && c5.visible && c6.visible && c7.visible && camY > py + bh * 0.5f) {
                    val roofPath = Path().apply {
                        moveTo(c4.x, c4.y)
                        lineTo(c5.x, c5.y)
                        lineTo(c6.x, c6.y)
                        lineTo(c7.x, c7.y)
                        close()
                    }
                    drawScope.drawPath(roofPath, color = roofColor)
                }

                // Front/Side faces based on visible corners
                drawQuad(drawScope, c0, c1, c5, c4, wallColor)
                drawQuad(drawScope, c1, c2, c6, c5, wallColor.copy(alpha = 0.85f))
                drawQuad(drawScope, c2, c3, c7, c6, wallColor)
                drawQuad(drawScope, c3, c0, c4, c7, wallColor.copy(alpha = 0.85f))
            }
        }
    }

    private fun drawQuad(
        drawScope: DrawScope,
        p0: ScreenPoint, p1: ScreenPoint, p2: ScreenPoint, p3: ScreenPoint,
        color: Color
    ) {
        if (!p0.visible || !p1.visible || !p2.visible || !p3.visible) return
        val path = Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawScope.drawPath(path, color = color)
        drawScope.drawPath(path, color = Color.Black.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
    }

    // --- 3D SURVIVOR / PLAYER CHARACTER ---
    private fun drawSurvivorPlayer(
        drawScope: DrawScope,
        p: PlayerState,
        camX: Float, camY: Float, camZ: Float,
        camYaw: Float, camPitch: Float,
        width: Float, height: Float,
        isVictory: Boolean
    ) {
        val px = p.position.x
        val py = p.position.y
        val pz = p.position.z

        val base = project(px, py, pz, camX, camY, camZ, camYaw, camPitch, width, height)
        val chest = project(px, py + 1.1f, pz, camX, camY, camZ, camYaw, camPitch, width, height)
        val head = project(px, py + 1.75f, pz, camX, camY, camZ, camYaw, camPitch, width, height)

        if (!base.visible || !head.visible) return

        val zScale = (width / (base.z * 10f)).coerceIn(8f, 75f)

        // Shadow under player
        drawScope.drawOval(
            color = Color(0x77000000),
            topLeft = Offset(base.x - zScale * 0.9f, base.y - zScale * 0.25f),
            size = Size(zScale * 1.8f, zScale * 0.5f)
        )

        val skin = p.skin
        val legSwing = if (p.velocity.length() > 0.1f) sin(p.walkAnimTime * 8f) * zScale * 0.3f else 0f

        // Legs
        val legColor = Color(skin.secondaryColor)
        drawScope.drawLine(
            color = legColor,
            start = Offset(chest.x - zScale * 0.25f, chest.y),
            end = Offset(base.x - zScale * 0.25f + legSwing, base.y),
            strokeWidth = zScale * 0.3f,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = legColor,
            start = Offset(chest.x + zScale * 0.25f, chest.y),
            end = Offset(base.x + zScale * 0.25f - legSwing, base.y),
            strokeWidth = zScale * 0.3f,
            cap = StrokeCap.Round
        )

        // Torso / Tactical Armor
        val armorColor = if (p.hitFlashTimer > 0f) Color.White else Color(skin.primaryColor)
        drawScope.drawRoundRect(
            color = armorColor,
            topLeft = Offset(chest.x - zScale * 0.5f, chest.y - zScale * 0.6f),
            size = Size(zScale * 1f, zScale * 0.8f),
            cornerRadius = CornerRadius(zScale * 0.2f, zScale * 0.2f)
        )

        // Head & Survivor Helmet
        val helmetColor = Color(0xFF263238)
        drawScope.drawCircle(
            color = helmetColor,
            radius = zScale * 0.45f,
            center = Offset(head.x, head.y)
        )
        // Glowing Visor
        val visorColor = Color(skin.accentColor)
        drawScope.drawRoundRect(
            color = visorColor,
            topLeft = Offset(head.x - zScale * 0.28f, head.y - zScale * 0.12f),
            size = Size(zScale * 0.56f, zScale * 0.22f),
            cornerRadius = CornerRadius(zScale * 0.08f, zScale * 0.08f)
        )

        // 3D Weapon in hand
        val weaponColor = Color(p.currentWeapon.colorHex)
        val gunEndOffset = if (isVictory) Offset(head.x + zScale * 0.5f, head.y - zScale * 0.9f)
        else Offset(chest.x + zScale * 0.7f, chest.y - zScale * 0.2f)

        drawScope.drawLine(
            color = weaponColor,
            start = Offset(chest.x + zScale * 0.3f, chest.y),
            end = gunEndOffset,
            strokeWidth = zScale * 0.22f,
            cap = StrokeCap.Round
        )

        // Muzzle Flash
        if (p.isFiring && !isVictory) {
            drawScope.drawCircle(
                color = Color(0xFFFFF176),
                radius = zScale * 0.35f,
                center = gunEndOffset
            )
        }
    }

    // --- 3D ENEMY BOTS ---
    private fun drawBot(
        drawScope: DrawScope,
        bot: EnemyBot,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float,
        isBengali: Boolean
    ) {
        val px = bot.position.x
        val py = bot.position.y
        val pz = bot.position.z

        val base = project(px, py, pz, camX, camY, camZ, yaw, pitch, width, height)
        val chest = project(px, py + 1.0f, pz, camX, camY, camZ, yaw, pitch, width, height)
        val head = project(px, py + 1.7f, pz, camX, camY, camZ, yaw, pitch, width, height)

        if (!base.visible || !head.visible) return

        val zScale = (width / (base.z * 10f)).coerceIn(6f, 70f)

        // Bot shadow
        drawScope.drawOval(
            color = Color(0x66000000),
            topLeft = Offset(base.x - zScale * 0.8f, base.y - zScale * 0.2f),
            size = Size(zScale * 1.6f, zScale * 0.4f)
        )

        if (bot.isDead) {
            // Fallen enemy corpse / loot beacon
            drawScope.drawCircle(
                color = Color(0x88FF1744),
                radius = zScale * 0.6f,
                center = Offset(base.x, base.y)
            )
            return
        }

        // Bot Body
        val botColor = Color(0xFFC62828) // Red enemy faction
        drawScope.drawRoundRect(
            color = botColor,
            topLeft = Offset(chest.x - zScale * 0.45f, chest.y - zScale * 0.5f),
            size = Size(zScale * 0.9f, zScale * 0.8f),
            cornerRadius = CornerRadius(zScale * 0.15f, zScale * 0.15f)
        )

        // Bot Head
        drawScope.drawCircle(
            color = Color(0xFF37474F),
            radius = zScale * 0.38f,
            center = Offset(head.x, head.y)
        )
        // Red glowing eye visor
        drawScope.drawCircle(
            color = Color(0xFFFF1744),
            radius = zScale * 0.12f,
            center = Offset(head.x, head.y - zScale * 0.05f)
        )

        // Weapon
        drawScope.drawLine(
            color = Color(bot.weapon.colorHex),
            start = Offset(chest.x + zScale * 0.2f, chest.y),
            end = Offset(chest.x + zScale * 0.6f, chest.y - zScale * 0.15f),
            strokeWidth = zScale * 0.18f,
            cap = StrokeCap.Round
        )

        // Floating Health Bar above bot
        val barWidth = zScale * 1.8f
        val barHeight = (zScale * 0.25f).coerceAtLeast(4f)
        val barY = head.y - zScale * 0.7f
        val hpPercent = (bot.health / bot.maxHealth).coerceIn(0f, 1f)

        // Bar background
        drawScope.drawRoundRect(
            color = Color(0xAA000000),
            topLeft = Offset(head.x - barWidth * 0.5f, barY),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barHeight * 0.5f, barHeight * 0.5f)
        )
        // Bar health fill
        val hpColor = if (hpPercent > 0.5f) Color(0xFF00E676) else if (hpPercent > 0.25f) Color(0xFFFFEB3B) else Color(0xFFFF1744)
        drawScope.drawRoundRect(
            color = hpColor,
            topLeft = Offset(head.x - barWidth * 0.5f, barY),
            size = Size(barWidth * hpPercent, barHeight),
            cornerRadius = CornerRadius(barHeight * 0.5f, barHeight * 0.5f)
        )
    }

    // --- 3D AIRDROP CRATE ---
    private fun drawAirdrop(
        drawScope: DrawScope,
        drop: Airdrop,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val px = drop.position.x
        val py = drop.position.y
        val pz = drop.position.z

        val base = project(px, py, pz, camX, camY, camZ, yaw, pitch, width, height)
        val top = project(px, py + 1.4f, pz, camX, camY, camZ, yaw, pitch, width, height)

        if (!base.visible || !top.visible) return

        val zScale = (width / (base.z * 10f)).coerceIn(8f, 85f)

        // Parachute if falling
        if (drop.isFalling) {
            val chuteTop = project(px, py + 5.5f, pz, camX, camY, camZ, yaw, pitch, width, height)
            if (chuteTop.visible) {
                // Parachute dome
                drawScope.drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF1744), Color.White, Color(0xFFFF1744)),
                        center = Offset(chuteTop.x, chuteTop.y),
                        radius = zScale * 2.2f
                    ),
                    topLeft = Offset(chuteTop.x - zScale * 2f, chuteTop.y - zScale * 0.9f),
                    size = Size(zScale * 4f, zScale * 1.8f)
                )
                // Cords
                drawScope.drawLine(Color.White.copy(alpha = 0.7f), Offset(chuteTop.x - zScale * 1.8f, chuteTop.y), Offset(top.x - zScale * 0.5f, top.y), strokeWidth = 1.5f)
                drawScope.drawLine(Color.White.copy(alpha = 0.7f), Offset(chuteTop.x + zScale * 1.8f, chuteTop.y), Offset(top.x + zScale * 0.5f, top.y), strokeWidth = 1.5f)
            }
        }

        // Red airdrop crate box with yellow straps
        drawScope.drawRoundRect(
            color = Color(0xFFD50000),
            topLeft = Offset(base.x - zScale * 0.7f, top.y),
            size = Size(zScale * 1.4f, zScale * 1.2f),
            cornerRadius = CornerRadius(zScale * 0.1f, zScale * 0.1f)
        )
        // Yellow cross strap
        drawScope.drawLine(
            color = Color(0xFFFFD600),
            start = Offset(base.x - zScale * 0.7f, top.y + zScale * 0.6f),
            end = Offset(base.x + zScale * 0.7f, top.y + zScale * 0.6f),
            strokeWidth = zScale * 0.2f
        )

        // Red smoke column rising from crate when on ground
        if (!drop.isFalling && drop.smokeTimer > 0f) {
            val smokeTop = project(px, py + 12f, pz, camX, camY, camZ, yaw, pitch, width, height)
            if (smokeTop.visible) {
                drawScope.drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x66FF1744), Color(0xAAFF5252)),
                        startY = smokeTop.y,
                        endY = top.y
                    ),
                    start = Offset(top.x, top.y),
                    end = Offset(smokeTop.x + sin(py) * 10f, smokeTop.y),
                    strokeWidth = zScale * 0.8f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // --- 3D FLOATING LOOT ---
    private fun drawLootItem(
        drawScope: DrawScope,
        loot: LootItem,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val px = loot.position.x
        val py = loot.position.y + 0.4f + loot.hoverOffset
        val pz = loot.position.z

        val pt = project(px, py, pz, camX, camY, camZ, yaw, pitch, width, height)
        if (!pt.visible) return

        val zScale = (width / (pt.z * 12f)).coerceIn(4f, 40f)

        // Glowing circle base
        val lootColor = Color(loot.type.colorHex)
        drawScope.drawCircle(
            color = lootColor.copy(alpha = 0.35f),
            radius = zScale * 1.2f,
            center = Offset(pt.x, pt.y)
        )
        drawScope.drawCircle(
            color = lootColor,
            radius = zScale * 0.65f,
            center = Offset(pt.x, pt.y)
        )
        // White symbol inside
        drawScope.drawCircle(
            color = Color.White,
            radius = zScale * 0.25f,
            center = Offset(pt.x, pt.y)
        )
    }

    // --- 3D PROJECTILES (BULLETS & GRENADES) ---
    private fun drawProjectile(
        drawScope: DrawScope,
        proj: Projectile,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val pt = project(proj.position.x, proj.position.y, proj.position.z, camX, camY, camZ, yaw, pitch, width, height)
        if (!pt.visible) return

        val zScale = (width / (pt.z * 12f)).coerceIn(2f, 30f)

        if (proj.isGrenade) {
            // Grenade: dark olive sphere with blinking red fuse LED
            drawScope.drawCircle(
                color = Color(0xFF33691E),
                radius = zScale * 0.6f,
                center = Offset(pt.x, pt.y)
            )
            val blink = (proj.fuseTimer * 10).toInt() % 2 == 0
            if (blink) {
                drawScope.drawCircle(
                    color = Color(0xFFFF1744),
                    radius = zScale * 0.28f,
                    center = Offset(pt.x, pt.y - zScale * 0.3f)
                )
            }
        } else if (proj.isRocket) {
            // RPG rocket with fiery trail
            drawScope.drawCircle(
                color = Color(0xFFFF5722),
                radius = zScale * 0.8f,
                center = Offset(pt.x, pt.y)
            )
        } else {
            // Tracer Bullet
            val trailPt = project(
                proj.position.x - proj.velocity.x * 0.04f,
                proj.position.y - proj.velocity.y * 0.04f,
                proj.position.z - proj.velocity.z * 0.04f,
                camX, camY, camZ, yaw, pitch, width, height
            )
            if (trailPt.visible) {
                drawScope.drawLine(
                    color = Color(0xFFFFD54F),
                    start = Offset(trailPt.x, trailPt.y),
                    end = Offset(pt.x, pt.y),
                    strokeWidth = zScale * 0.4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // --- 3D PARTICLES & EXPLOSIONS ---
    private fun drawParticle(
        drawScope: DrawScope,
        p: Particle,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float
    ) {
        val pt = project(p.position.x, p.position.y, p.position.z, camX, camY, camZ, yaw, pitch, width, height)
        if (!pt.visible) return

        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val zScale = (width / (pt.z * 10f)).coerceIn(1f, 100f)
        val radius = p.size * zScale * 0.1f

        when (p.type) {
            ParticleType.BLAST_RING -> {
                // Expanding explosion blast ring
                drawScope.drawCircle(
                    color = Color(p.colorHex).copy(alpha = alpha * 0.7f),
                    radius = radius * 2.5f,
                    center = Offset(pt.x, pt.y),
                    style = Stroke(width = (zScale * 0.3f).coerceAtLeast(2f))
                )
            }
            ParticleType.FIRE, ParticleType.SPARK -> {
                drawScope.drawCircle(
                    color = Color(p.colorHex).copy(alpha = alpha),
                    radius = radius,
                    center = Offset(pt.x, pt.y)
                )
            }
            ParticleType.SMOKE -> {
                drawScope.drawCircle(
                    color = Color(0xFF616161).copy(alpha = alpha * 0.5f),
                    radius = radius * 1.5f,
                    center = Offset(pt.x, pt.y)
                )
            }
            ParticleType.BLOOD -> {
                drawScope.drawCircle(
                    color = Color(0xFFD50000).copy(alpha = alpha),
                    radius = radius * 0.8f,
                    center = Offset(pt.x, pt.y)
                )
            }
            ParticleType.HEAL -> {
                drawScope.drawCircle(
                    color = Color(0xFF00E676).copy(alpha = alpha),
                    radius = radius,
                    center = Offset(pt.x, pt.y)
                )
            }
            ParticleType.VICTORY_CONFETTI -> {
                drawScope.drawRect(
                    color = Color(p.colorHex).copy(alpha = alpha),
                    topLeft = Offset(pt.x - radius * 0.5f, pt.y - radius * 0.5f),
                    size = Size(radius, radius)
                )
            }
        }
    }

    // --- VICTORY TROPHY & GLOW BEAM ---
    private fun drawVictoryTrophy(
        drawScope: DrawScope,
        pos: Vector3,
        camX: Float, camY: Float, camZ: Float,
        yaw: Float, pitch: Float,
        width: Float, height: Float,
        time: Float
    ) {
        val base = project(pos.x, pos.y, pos.z, camX, camY, camZ, yaw, pitch, width, height)
        val beamTop = project(pos.x, pos.y + 25f, pos.z, camX, camY, camZ, yaw, pitch, width, height)

        if (!base.visible) return

        val zScale = (width / (base.z * 10f)).coerceIn(10f, 90f)

        // Celestial Gold Beam of Light
        if (beamTop.visible) {
            drawScope.drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x33FFE082), Color(0x99FFD54F)),
                    startY = beamTop.y,
                    endY = base.y
                ),
                start = Offset(base.x, base.y),
                end = Offset(beamTop.x, beamTop.y),
                strokeWidth = zScale * 2f
            )
        }

        // Golden Trophy Pedestal
        drawScope.drawRoundRect(
            color = Color(0xFFB8860B),
            topLeft = Offset(base.x - zScale * 0.8f, base.y - zScale * 0.3f),
            size = Size(zScale * 1.6f, zScale * 0.6f),
            cornerRadius = CornerRadius(zScale * 0.1f, zScale * 0.1f)
        )

        // Golden Cup Trophy
        drawScope.drawOval(
            color = Color(0xFFFFD700),
            topLeft = Offset(base.x - zScale * 0.6f, base.y - zScale * 1.4f),
            size = Size(zScale * 1.2f, zScale * 1.1f)
        )
        // Star on Trophy
        drawScope.drawCircle(
            color = Color(0xFFFFF9C4),
            radius = zScale * 0.25f,
            center = Offset(base.x, base.y - zScale * 0.85f)
        )
    }
}
