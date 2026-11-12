package com.example.game

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.GameAudioEngine
import com.example.game.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

enum class GameScreenState {
    LOBBY,
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER
}

data class CareerStats(
    val matchesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalKills: Int = 0,
    val highestKills: Int = 0,
    val totalDamage: Float = 0f
)

data class GameUiState(
    val screenState: GameScreenState = GameScreenState.LOBBY,
    val player: PlayerState = PlayerState(),
    val bots: List<EnemyBot> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val airdrop: Airdrop? = null,
    val lootItems: List<LootItem> = emptyList(),
    val buildings: List<WorldBuilding> = emptyList(),
    val safeZone: SafeZone = SafeZone(),
    val killFeed: List<KillFeedItem> = emptyList(),
    val aliveCount: Int = 25,
    val totalPlayers: Int = 25,
    val stormWarning: String? = null,
    val locationName: String = "ধানমন্ডি লেক ঘাট",
    val screenShake: Float = 0f,
    val victoryTimer: Float = 0f,
    val playerPlacement: Int = 1,
    val isBengali: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val careerStats: CareerStats = CareerStats()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val audioEngine = GameAudioEngine()
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var gameLoopJob: Job? = null
    private val prefs = application.getSharedPreferences("shesh_bacha_prefs", Context.MODE_PRIVATE)

    // Joystick input vector (-1..1)
    private var moveInputX = 0f
    private var moveInputY = 0f

    init {
        loadCareerStats()
    }

    private fun loadCareerStats() {
        val matches = prefs.getInt("matches_played", 0)
        val wins = prefs.getInt("total_wins", 0)
        val kills = prefs.getInt("total_kills", 0)
        val highestKills = prefs.getInt("highest_kills", 0)
        val damage = prefs.getFloat("total_damage", 0f)
        val isBn = prefs.getBoolean("is_bengali", true)
        val sound = prefs.getBoolean("sound_enabled", true)
        val vib = prefs.getBoolean("vib_enabled", true)

        audioEngine.isMuted = !sound
        _uiState.update {
            it.copy(
                careerStats = CareerStats(matches, wins, kills, highestKills, damage),
                isBengali = isBn,
                soundEnabled = sound,
                vibrationEnabled = vib
            )
        }
    }

    private fun saveCareerStats(won: Boolean, kills: Int, damage: Float) {
        val current = _uiState.value.careerStats
        val newMatches = current.matchesPlayed + 1
        val newWins = current.totalWins + (if (won) 1 else 0)
        val newKills = current.totalKills + kills
        val newHighestKills = max(current.highestKills, kills)
        val newDamage = current.totalDamage + damage

        prefs.edit()
            .putInt("matches_played", newMatches)
            .putInt("total_wins", newWins)
            .putInt("total_kills", newKills)
            .putInt("highest_kills", newHighestKills)
            .putFloat("total_damage", newDamage)
            .apply()

        _uiState.update {
            it.copy(
                careerStats = CareerStats(newMatches, newWins, newKills, newHighestKills, newDamage)
            )
        }
    }

    fun toggleLanguage() {
        _uiState.update {
            val next = !it.isBengali
            prefs.edit().putBoolean("is_bengali", next).apply()
            it.copy(isBengali = next)
        }
    }

    fun toggleSound() {
        _uiState.update {
            val next = !it.soundEnabled
            audioEngine.isMuted = !next
            prefs.edit().putBoolean("sound_enabled", next).apply()
            it.copy(soundEnabled = next)
        }
    }

    fun toggleVibration() {
        _uiState.update {
            val next = !it.vibrationEnabled
            prefs.edit().putBoolean("vib_enabled", next).apply()
            it.copy(vibrationEnabled = next)
        }
    }

    fun setSensitivity(value: Float) {
        _uiState.update { it.copy(sensitivity = value.coerceIn(0.5f, 2.5f)) }
    }

    fun setPlayerSkin(skin: SkinType) {
        _uiState.update {
            it.copy(player = it.player.copy(skin = skin))
        }
    }

    private fun triggerHaptic(durationMs: Long, amplitude: Int = 180) {
        if (!_uiState.value.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore haptic failure
        }
    }

    // --- GAME INITIALIZATION & LIFECYCLE ---

    fun startMatch(playerCount: Int = 25) {
        val buildings = generateArenaWorld()
        val loot = generateInitialLoot()
        val bots = generateEnemyBots(playerCount - 1)

        val playerInitialPos = Vector3(
            Random.nextFloat() * 40f - 20f,
            0f,
            Random.nextFloat() * 40f - 20f
        )

        val newPlayer = PlayerState(
            position = playerInitialPos,
            health = 100f,
            shield = 50f,
            bombs = 3,
            currentWeapon = WeaponType.ASSAULT_RIFLE,
            ammo = WeaponType.ASSAULT_RIFLE.maxAmmo,
            skin = _uiState.value.player.skin
        )

        val initialSafeZone = SafeZone(
            centerX = 0f,
            centerZ = 0f,
            currentRadius = 140f,
            targetRadius = 140f,
            countdownSec = 25f,
            wave = 1,
            isShrinking = false
        )

        _uiState.update {
            it.copy(
                screenState = GameScreenState.PLAYING,
                player = newPlayer,
                bots = bots,
                projectiles = emptyList(),
                particles = emptyList(),
                airdrop = null,
                lootItems = loot,
                buildings = buildings,
                safeZone = initialSafeZone,
                killFeed = emptyList(),
                aliveCount = playerCount,
                totalPlayers = playerCount,
                stormWarning = null,
                screenShake = 0f,
                victoryTimer = 0f,
                playerPlacement = 1
            )
        }

        startGameLoop()
    }

    fun pauseGame() {
        if (_uiState.value.screenState == GameScreenState.PLAYING) {
            _uiState.update { it.copy(screenState = GameScreenState.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.screenState == GameScreenState.PAUSED) {
            _uiState.update { it.copy(screenState = GameScreenState.PLAYING) }
        }
    }

    fun returnToLobby() {
        gameLoopJob?.cancel()
        _uiState.update { it.copy(screenState = GameScreenState.LOBBY) }
    }

    // --- GAME LOOP ---

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTime = now

                if (_uiState.value.screenState == GameScreenState.PLAYING) {
                    updateGamePhysics(dt)
                } else if (_uiState.value.screenState == GameScreenState.VICTORY) {
                    updateVictoryCinematic(dt)
                }

                delay(16) // ~60fps
            }
        }
    }

    // --- INPUT CONTROLS ---

    fun setMoveInput(x: Float, y: Float) {
        moveInputX = x.coerceIn(-1f, 1f)
        moveInputY = y.coerceIn(-1f, 1f)
    }

    fun rotateCamera(deltaYaw: Float, deltaPitch: Float) {
        val sens = _uiState.value.sensitivity
        _uiState.update { state ->
            val p = state.player
            val newYaw = p.yaw + deltaYaw * 0.005f * sens
            val newPitch = (p.pitch - deltaPitch * 0.005f * sens).coerceIn(-1.1f, 1.1f)
            state.copy(player = p.copy(yaw = newYaw, pitch = newPitch))
        }
    }

    fun triggerJump() {
        val player = _uiState.value.player
        if (!player.isJumping && player.position.y <= 0.05f) {
            _uiState.update {
                it.copy(
                    player = it.player.copy(
                        isJumping = true,
                        velocity = it.player.velocity.copy(y = 8.5f)
                    )
                )
            }
        }
    }

    fun setAiming(isAiming: Boolean) {
        _uiState.update {
            it.copy(player = it.player.copy(isAiming = isAiming))
        }
    }

    fun switchWeapon() {
        val p = _uiState.value.player
        val weapons = WeaponType.values()
        val nextIndex = (p.currentWeapon.ordinal + 1) % weapons.size
        val nextWeapon = weapons[nextIndex]

        audioEngine.playKatana()
        _uiState.update {
            it.copy(
                player = it.player.copy(
                    currentWeapon = nextWeapon,
                    ammo = nextWeapon.maxAmmo,
                    isReloading = false,
                    reloadTimer = 0f
                )
            )
        }
    }

    fun useMedkit() {
        val p = _uiState.value.player
        if (p.health < p.maxHealth || p.shield < p.maxShield) {
            val newHealth = (p.health + 45f).coerceAtMost(p.maxHealth)
            val newShield = (p.shield + 25f).coerceAtMost(p.maxShield)
            audioEngine.playHeal()
            triggerHaptic(60, 100)

            // Spawn green healing particles
            val healParticles = (0..8).map {
                Particle(
                    position = Vector3(p.position.x + Random.nextFloat() - 0.5f, p.position.y + 0.8f, p.position.z + Random.nextFloat() - 0.5f),
                    velocity = Vector3(0f, 1.5f + Random.nextFloat(), 0f),
                    colorHex = 0xFF00E676,
                    size = 12f,
                    life = 0.8f,
                    maxLife = 0.8f,
                    type = ParticleType.HEAL
                )
            }

            _uiState.update {
                it.copy(
                    player = it.player.copy(health = newHealth, shield = newShield),
                    particles = it.particles + healParticles
                )
            }
        }
    }

    fun throwBomb() {
        val p = _uiState.value.player
        if (p.bombs <= 0) return

        val cosY = cos(p.yaw)
        val sinY = sin(p.yaw)
        val cosP = cos(p.pitch)
        val sinP = sin(p.pitch)

        // Forward ballistic velocity
        val throwSpeed = 22f
        val vx = sinY * cosP * throwSpeed
        val vy = (-sinP * throwSpeed + 6f).coerceAtLeast(3f)
        val vz = cosY * cosP * throwSpeed

        val spawnPos = Vector3(
            p.position.x + sinY * 1f,
            p.position.y + 1.5f,
            p.position.z + cosY * 1f
        )

        val grenade = Projectile(
            position = spawnPos,
            velocity = Vector3(vx, vy, vz),
            isGrenade = true,
            fuseTimer = 2.4f,
            ownerId = 0,
            damage = 120f,
            radius = 12f
        )

        audioEngine.playRifle()
        triggerHaptic(40, 140)

        _uiState.update {
            it.copy(
                player = it.player.copy(bombs = it.player.bombs - 1),
                projectiles = it.projectiles + grenade
            )
        }
    }

    fun startFiring() {
        val p = _uiState.value.player
        if (p.ammo <= 0) {
            reloadWeapon()
            return
        }
        _uiState.update { it.copy(player = it.player.copy(isFiring = true)) }
    }

    fun stopFiring() {
        _uiState.update { it.copy(player = it.player.copy(isFiring = false)) }
    }

    fun reloadWeapon() {
        val p = _uiState.value.player
        if (p.isReloading || p.ammo == p.currentWeapon.maxAmmo) return
        _uiState.update {
            it.copy(player = it.player.copy(isReloading = true, reloadTimer = p.currentWeapon.reloadTimeSec))
        }
    }

    // --- PHYSICS & GAME STATE UPDATE ---

    private fun updateGamePhysics(dt: Float) {
        val currentState = _uiState.value
        var player = currentState.player
        var bots = currentState.bots.toMutableList()
        var projectiles = currentState.projectiles.toMutableList()
        var particles = currentState.particles.toMutableList()
        var safeZone = currentState.safeZone
        var airdrop = currentState.airdrop
        var lootItems = currentState.lootItems.toMutableList()
        val killFeed = currentState.killFeed.toMutableList()
        var aliveCount = currentState.aliveCount
        var shake = max(0f, currentState.screenShake - dt * 2.5f)
        var warning: String? = null

        // 1. Player Update
        // Movement relative to camera yaw
        val cosY = cos(player.yaw)
        val sinY = sin(player.yaw)
        val moveSpeed = if (player.isAiming) 4.2f else 7.5f

        val forwardX = sinY * moveInputY
        val forwardZ = cosY * moveInputY
        val strafeX = cosY * moveInputX
        val strafeZ = -sinY * moveInputX

        var vx = (forwardX + strafeX) * moveSpeed
        var vz = (forwardZ + strafeZ) * moveSpeed
        var vy = player.velocity.y

        // Gravity & Jump
        if (player.isJumping || player.position.y > 0f) {
            vy -= 22f * dt
        }

        var py = player.position.y + vy * dt
        if (py <= 0f) {
            py = 0f
            vy = 0f
            player = player.copy(isJumping = false)
        }

        var px = player.position.x + vx * dt
        var pz = player.position.z + vz * dt

        // Arena boundary collision (-150 to +150)
        px = px.coerceIn(-140f, 140f)
        pz = pz.coerceIn(-140f, 140f)

        // Building collision resolution
        for (b in currentState.buildings) {
            val hw = b.width * 0.5f + 0.6f
            val hd = b.depth * 0.5f + 0.6f
            if (px > b.position.x - hw && px < b.position.x + hw &&
                pz > b.position.z - hd && pz < b.position.z + hd
            ) {
                // Push back to closest edge
                val dxLeft = px - (b.position.x - hw)
                val dxRight = (b.position.x + hw) - px
                val dzTop = pz - (b.position.z - hd)
                val dzBot = (b.position.z + hd) - pz
                val minD = min(min(dxLeft, dxRight), min(dzTop, dzBot))
                when (minD) {
                    dxLeft -> px = b.position.x - hw
                    dxRight -> px = b.position.x + hw
                    dzTop -> pz = b.position.z - hd
                    dzBot -> pz = b.position.z + hd
                }
            }
        }

        val walkTime = if (abs(vx) > 0.1f || abs(vz) > 0.1f) player.walkAnimTime + dt else 0f
        val hitFlash = max(0f, player.hitFlashTimer - dt)
        val survival = player.survivalTime + dt

        // Player Reloading
        var isReloading = player.isReloading
        var reloadTimer = player.reloadTimer
        var ammo = player.ammo
        if (isReloading) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                isReloading = false
                ammo = player.currentWeapon.maxAmmo
            }
        }

        // Player Firing
        var fireCooldown = max(0f, player.fireCooldown - dt)
        if (player.isFiring && !isReloading && fireCooldown <= 0f && ammo > 0) {
            ammo--
            fireCooldown = 1f / player.currentWeapon.fireRate
            triggerHaptic(25, 120)

            when (player.currentWeapon) {
                WeaponType.ASSAULT_RIFLE -> audioEngine.playRifle()
                WeaponType.SHOTGUN -> audioEngine.playShotgun()
                WeaponType.SNIPER -> audioEngine.playSniper()
                WeaponType.KATANA -> audioEngine.playKatana()
                WeaponType.ROCKET_LAUNCHER -> audioEngine.playExplosion()
            }

            // Spawn bullet/rocket projectile
            val spreadFactor = player.currentWeapon.spread
            val spreadX = (Random.nextFloat() * 2f - 1f) * spreadFactor
            val spreadY = (Random.nextFloat() * 2f - 1f) * spreadFactor

            val bulletSpeed = player.currentWeapon.bulletSpeed
            val bvx = (sinY + spreadX) * cos(player.pitch) * bulletSpeed
            val bvy = (-sin(player.pitch) + spreadY) * bulletSpeed
            val bvz = (cosY + spreadX) * cos(player.pitch) * bulletSpeed

            val spawnB = Vector3(px + sinY * 0.8f, py + 1.4f, pz + cosY * 0.8f)

            if (player.currentWeapon == WeaponType.ROCKET_LAUNCHER) {
                projectiles.add(
                    Projectile(
                        position = spawnB,
                        velocity = Vector3(bvx * 0.5f, bvy * 0.5f, bvz * 0.5f),
                        isRocket = true,
                        ownerId = 0,
                        damage = player.currentWeapon.damage,
                        radius = 8f
                    )
                )
            } else if (player.currentWeapon == WeaponType.KATANA) {
                // Immediate melee sweep
                for (bot in bots) {
                    if (!bot.isDead && bot.position.distanceTo(player.position) < player.currentWeapon.range) {
                        bot.health -= player.currentWeapon.damage
                        audioEngine.playHit()
                        player = player.copy(damageDealt = player.damageDealt + player.currentWeapon.damage)
                        if (bot.health <= 0f) {
                            bot.isDead = true
                            player = player.copy(kills = player.kills + 1)
                            killFeed.add(KillFeedItem("Player", bot.name, player.currentWeapon.getDisplayName(currentState.isBengali), true, false))
                        }
                    }
                }
            } else {
                projectiles.add(
                    Projectile(
                        position = spawnB,
                        velocity = Vector3(bvx, bvy, bvz),
                        ownerId = 0,
                        damage = player.currentWeapon.damage
                    )
                )
            }

            if (ammo <= 0) {
                isReloading = true
                reloadTimer = player.currentWeapon.reloadTimeSec
            }
        }

        player = player.copy(
            position = Vector3(px, py, pz),
            velocity = Vector3(vx, vy, vz),
            ammo = ammo,
            isReloading = isReloading,
            reloadTimer = reloadTimer,
            fireCooldown = fireCooldown,
            walkAnimTime = walkTime,
            hitFlashTimer = hitFlash,
            survivalTime = survival
        )

        // 2. Safe Zone / Storm Update
        var szRadius = safeZone.currentRadius
        var szCountdown = safeZone.countdownSec - dt
        var isShrinking = safeZone.isShrinking

        if (szCountdown <= 0f) {
            if (!isShrinking) {
                isShrinking = true
                audioEngine.playZoneWarning()
                triggerHaptic(120, 200)
                warning = if (currentState.isBengali) "ঝড় সংকুচিত হচ্ছে! নিরাপদ জোনে যান!" else "Storm shrinking! Move to Safe Zone!"
                safeZone = safeZone.copy(targetRadius = (szRadius * 0.55f).coerceAtLeast(18f))
            } else {
                szRadius -= safeZone.shrinkSpeed * dt
                if (szRadius <= safeZone.targetRadius) {
                    szRadius = safeZone.targetRadius
                    isShrinking = false
                    szCountdown = 30f // next wave countdown
                    safeZone = safeZone.copy(wave = safeZone.wave + 1)
                }
            }
        }
        safeZone = safeZone.copy(currentRadius = szRadius, countdownSec = szCountdown, isShrinking = isShrinking)

        // Safe zone damage to player
        val distToCenter = sqrt((px - safeZone.centerX).pow(2) + (pz - safeZone.centerZ).pow(2))
        if (distToCenter > szRadius) {
            val stormDmg = 8f * dt * safeZone.wave
            if (player.shield > 0) {
                player = player.copy(shield = max(0f, player.shield - stormDmg))
            } else {
                player = player.copy(health = max(0f, player.health - stormDmg), hitFlashTimer = 0.2f)
            }
            warning = if (currentState.isBengali) "সতর্কতা: ঝড়ের ক্ষয়ক্ষতি হচ্ছে!" else "Warning: Taking Storm Damage!"
        }

        // 3. Projectiles Update
        val remainingProjectiles = mutableListOf<Projectile>()
        for (proj in projectiles) {
            proj.lifeTime -= dt
            if (proj.isGrenade) {
                proj.fuseTimer -= dt
                // Ballistic trajectory
                proj.velocity = proj.velocity.copy(y = proj.velocity.y - 18f * dt)
                proj.position = proj.position + proj.velocity * dt

                // Bounce off ground
                if (proj.position.y <= 0.2f) {
                    proj.position = proj.position.copy(y = 0.2f)
                    proj.velocity = proj.velocity.copy(
                        x = proj.velocity.x * 0.45f,
                        y = -proj.velocity.y * 0.35f,
                        z = proj.velocity.z * 0.45f
                    )
                }

                if (proj.fuseTimer <= 0f) {
                    // BOOM!
                    shake = 1.0f
                    audioEngine.playExplosion()
                    triggerHaptic(220, 255)
                    spawnExplosionParticles(particles, proj.position)

                    // Damage player if in radius
                    val pDist = proj.position.distanceTo(player.position)
                    if (pDist < proj.radius) {
                        val factor = 1f - pDist / proj.radius
                        val dmg = proj.damage * factor
                        player = applyDamageToPlayer(player, dmg)
                    }

                    // Damage bots in radius
                    for (bot in bots) {
                        if (!bot.isDead) {
                            val bDist = proj.position.distanceTo(bot.position)
                            if (bDist < proj.radius) {
                                val factor = 1f - bDist / proj.radius
                                val dmg = proj.damage * factor
                                bot.health -= dmg
                                if (bot.health <= 0f) {
                                    bot.isDead = true
                                    if (proj.ownerId == 0) {
                                        player = player.copy(kills = player.kills + 1)
                                        killFeed.add(KillFeedItem("Player", bot.name, "Frag Bomb", true, false))
                                    }
                                }
                            }
                        }
                    }
                    continue
                }
                remainingProjectiles.add(proj)
            } else if (proj.isRocket) {
                proj.position = proj.position + proj.velocity * dt
                if (proj.position.y <= 0.2f || proj.lifeTime <= 0f) {
                    // Rocket Explosion
                    shake = 0.8f
                    audioEngine.playExplosion()
                    spawnExplosionParticles(particles, proj.position)
                    continue
                }
                remainingProjectiles.add(proj)
            } else {
                // Fast Bullet
                proj.position = proj.position + proj.velocity * dt

                // Check hit against player
                if (proj.ownerId != 0 && proj.position.distanceTo(player.position) < 1.2f) {
                    player = applyDamageToPlayer(player, proj.damage)
                    audioEngine.playPlayerHurt()
                    triggerHaptic(80, 180)
                    spawnHitParticles(particles, proj.position, ParticleType.BLOOD)
                    continue
                }

                // Check hit against bots
                var hitBot = false
                if (proj.ownerId == 0) {
                    for (bot in bots) {
                        if (!bot.isDead && proj.position.distanceTo(bot.position) < 1.4f) {
                            bot.health -= proj.damage
                            player = player.copy(damageDealt = player.damageDealt + proj.damage)
                            audioEngine.playHit()
                            spawnHitParticles(particles, proj.position, ParticleType.SPARK)
                            if (bot.health <= 0f) {
                                bot.isDead = true
                                player = player.copy(kills = player.kills + 1)
                                killFeed.add(KillFeedItem("Player", bot.name, player.currentWeapon.getDisplayName(currentState.isBengali), true, false))
                            }
                            hitBot = true
                            break
                        }
                    }
                }

                if (!hitBot && proj.lifeTime > 0f && proj.position.y > 0f) {
                    remainingProjectiles.add(proj)
                }
            }
        }

        // 4. Bots AI Update & Inter-bot battles
        for (bot in bots) {
            if (bot.isDead) {
                bot.deathTimer += dt
                continue
            }

            // Storm damage to bots
            val bDistCenter = sqrt((bot.position.x - safeZone.centerX).pow(2) + (bot.position.z - safeZone.centerZ).pow(2))
            if (bDistCenter > szRadius) {
                bot.health -= 12f * dt
                if (bot.health <= 0f) {
                    bot.isDead = true
                    killFeed.add(KillFeedItem("Storm", bot.name, "Zone", false, false))
                    continue
                }
            }

            // Bot Decision
            val distToPlayer = bot.position.distanceTo(player.position)
            bot.fireCooldown = max(0f, bot.fireCooldown - dt)

            if (distToPlayer < 45f) {
                // Face player
                val dx = player.position.x - bot.position.x
                val dz = player.position.z - bot.position.z
                bot.yaw = atan2(dx, dz)

                // Move towards or circle player
                if (distToPlayer > 12f) {
                    val spd = 4.5f
                    bot.position = bot.position + Vector3(sin(bot.yaw) * spd * dt, 0f, cos(bot.yaw) * spd * dt)
                }

                // Shoot at player
                if (bot.fireCooldown <= 0f && distToPlayer < 35f) {
                    bot.fireCooldown = 1.4f + Random.nextFloat() * 0.8f
                    val bDir = (player.position - bot.position).normalized()
                    val bvx = bDir.x * 65f + (Random.nextFloat() * 4f - 2f)
                    val bvy = bDir.y * 65f + (Random.nextFloat() * 2f - 1f)
                    val bvz = bDir.z * 65f + (Random.nextFloat() * 4f - 2f)

                    remainingProjectiles.add(
                        Projectile(
                            position = bot.position + Vector3(0f, 1.4f, 0f),
                            velocity = Vector3(bvx, bvy, bvz),
                            ownerId = bot.id,
                            damage = 14f
                        )
                    )
                }
            } else {
                // Random wander towards safe zone
                val targetDir = Vector3(safeZone.centerX - bot.position.x, 0f, safeZone.centerZ - bot.position.z).normalized()
                bot.yaw = atan2(targetDir.x, targetDir.z)
                bot.position = bot.position + targetDir * (3.0f * dt)

                // Inter-bot skirmish simulation in distant zones
                if (Random.nextFloat() < 0.003f * dt * bots.size) {
                    val other = bots.filter { it != bot && !it.isDead }.randomOrNull()
                    if (other != null) {
                        other.health -= 35f
                        if (other.health <= 0f) {
                            other.isDead = true
                            killFeed.add(KillFeedItem(bot.name, other.name, bot.weapon.getDisplayName(currentState.isBengali), false, false))
                        }
                    }
                }
            }
        }

        // Count remaining alive
        val activeBots = bots.count { !it.isDead }
        val playerAlive = player.health > 0f
        aliveCount = activeBots + (if (playerAlive) 1 else 0)

        // 5. Airdrop Update
        if (airdrop == null && safeZone.wave >= 2 && Random.nextFloat() < 0.02f) {
            // Spawn airdrop
            airdrop = Airdrop(
                position = Vector3(
                    safeZone.centerX + (Random.nextFloat() * 30f - 15f),
                    45f,
                    safeZone.centerZ + (Random.nextFloat() * 30f - 15f)
                ),
                targetY = 0f,
                isFalling = true
            )
        }
        if (airdrop != null && airdrop.isFalling) {
            val newY = airdrop.position.y - 4.5f * dt
            if (newY <= 0f) {
                airdrop = airdrop.copy(
                    position = airdrop.position.copy(y = 0f),
                    isFalling = false,
                    parachuteOpen = false
                )
                // Spawn high-tier loot near airdrop
                lootItems.add(LootItem(999, airdrop.position + Vector3(1.5f, 0f, 0f), LootType.AIRDROP_WEAPON, WeaponType.ROCKET_LAUNCHER))
                lootItems.add(LootItem(998, airdrop.position + Vector3(-1.5f, 0f, 0f), LootType.HEALTH_PACK, amount = 2))
                lootItems.add(LootItem(997, airdrop.position + Vector3(0f, 0f, 1.5f), LootType.BOMB_PACK, amount = 3))
            } else {
                airdrop = airdrop.copy(position = airdrop.position.copy(y = newY))
            }
        }

        // 6. Loot items collection
        for (loot in lootItems) {
            if (!loot.isCollected) {
                loot.hoverOffset = sin(survival * 4f) * 0.15f
                if (player.position.distanceTo(loot.position) < 2.2f) {
                    loot.isCollected = true
                    audioEngine.playLoot()
                    triggerHaptic(40, 100)

                    when (loot.type) {
                        LootType.HEALTH_PACK -> player = player.copy(health = (player.health + 40f).coerceAtMost(player.maxHealth))
                        LootType.SHIELD_PACK -> player = player.copy(shield = (player.shield + 50f).coerceAtMost(player.maxShield))
                        LootType.BOMB_PACK -> player = player.copy(bombs = player.bombs + loot.amount)
                        LootType.AMMO_PACK -> player = player.copy(ammo = player.currentWeapon.maxAmmo)
                        LootType.AIRDROP_WEAPON -> {
                            loot.weapon?.let { w ->
                                player = player.copy(currentWeapon = w, ammo = w.maxAmmo)
                            }
                        }
                    }
                }
            }
        }

        // 7. Particles Update
        val remainingParticles = mutableListOf<Particle>()
        for (p in particles) {
            p.life -= dt
            p.position = p.position + p.velocity * dt
            if (p.life > 0f) remainingParticles.add(p)
        }

        // 8. Win / Loss Condition
        var screenState = currentState.screenState
        var placement = currentState.playerPlacement
        if (!playerAlive) {
            screenState = GameScreenState.GAME_OVER
            placement = aliveCount + 1
            audioEngine.playDefeat()
            saveCareerStats(false, player.kills, player.damageDealt)
        } else if (activeBots == 0 && aliveCount == 1) {
            screenState = GameScreenState.VICTORY
            placement = 1
            audioEngine.playVictory()
            saveCareerStats(true, player.kills, player.damageDealt)
        }

        // Location Zone Name calculation
        val loc = getLocationZoneName(px, pz, currentState.isBengali)

        _uiState.update {
            it.copy(
                screenState = screenState,
                player = player,
                bots = bots,
                projectiles = remainingProjectiles,
                particles = remainingParticles,
                airdrop = airdrop,
                lootItems = lootItems,
                safeZone = safeZone,
                killFeed = killFeed.takeLast(5),
                aliveCount = aliveCount,
                stormWarning = warning,
                locationName = loc,
                screenShake = shake,
                playerPlacement = placement
            )
        }
    }

    private fun updateVictoryCinematic(dt: Float) {
        _uiState.update {
            val newTime = it.victoryTimer + dt
            // Spawn victory celebration confetti particles
            val confetti = if (Random.nextFloat() < 0.35f) {
                listOf(
                    Particle(
                        position = Vector3(
                            it.player.position.x + Random.nextFloat() * 14f - 7f,
                            it.player.position.y + 8f + Random.nextFloat() * 4f,
                            it.player.position.z + Random.nextFloat() * 14f - 7f
                        ),
                        velocity = Vector3(Random.nextFloat() - 0.5f, -2.5f - Random.nextFloat(), Random.nextFloat() - 0.5f),
                        colorHex = listOf(0xFFFFD700, 0xFFFF5722, 0xFF00E5FF, 0xFF76FF03, 0xFFE040FB).random(),
                        size = 14f,
                        life = 2.2f,
                        maxLife = 2.2f,
                        type = ParticleType.VICTORY_CONFETTI
                    )
                )
            } else emptyList()

            it.copy(
                victoryTimer = newTime,
                particles = it.particles.filter { p -> p.life > 0 } + confetti
            )
        }
    }

    private fun applyDamageToPlayer(p: PlayerState, damage: Float): PlayerState {
        var shield = p.shield
        var health = p.health
        if (shield > 0) {
            val shieldDmg = min(shield, damage)
            shield -= shieldDmg
            val remainingDmg = damage - shieldDmg
            health = max(0f, health - remainingDmg)
        } else {
            health = max(0f, health - damage)
        }
        return p.copy(health = health, shield = shield, hitFlashTimer = 0.35f)
    }

    private fun spawnExplosionParticles(list: MutableList<Particle>, pos: Vector3) {
        // Blast ring
        list.add(
            Particle(
                position = pos,
                velocity = Vector3(),
                colorHex = 0xFFFF7043,
                size = 35f,
                life = 0.6f,
                maxLife = 0.6f,
                type = ParticleType.BLAST_RING
            )
        )
        // Fiery blast sparks
        for (i in 0..18) {
            val dir = Vector3(
                Random.nextFloat() * 2f - 1f,
                Random.nextFloat() * 1.5f + 0.2f,
                Random.nextFloat() * 2f - 1f
            ).normalized()
            val speed = Random.nextFloat() * 14f + 4f
            list.add(
                Particle(
                    position = pos,
                    velocity = dir * speed,
                    colorHex = listOf(0xFFFFD54F, 0xFFFF7043, 0xFFFF1744).random(),
                    size = Random.nextFloat() * 12f + 8f,
                    life = Random.nextFloat() * 0.7f + 0.3f,
                    maxLife = 1.0f,
                    type = ParticleType.SPARK
                )
            )
        }
    }

    private fun spawnHitParticles(list: MutableList<Particle>, pos: Vector3, type: ParticleType) {
        for (i in 0..6) {
            val vel = Vector3(
                Random.nextFloat() * 4f - 2f,
                Random.nextFloat() * 3f + 1f,
                Random.nextFloat() * 4f - 2f
            )
            list.add(
                Particle(
                    position = pos,
                    velocity = vel,
                    colorHex = if (type == ParticleType.BLOOD) 0xFFD50000 else 0xFFFFD54F,
                    size = 8f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    type = type
                )
            )
        }
    }

    private fun getLocationZoneName(x: Float, z: Float, isBn: Boolean): String {
        return when {
            x < -40 && z < -40 -> if (isBn) "পরিত্যক্ত কারখানা" else "Abandoned Factory"
            x > 40 && z < -40 -> if (isBn) "মিলিটারি ক্যাম্প" else "Military Outpost"
            x < -40 && z > 40 -> if (isBn) "নদী তীরবর্তী গ্রাম" else "River Village"
            x > 40 && z > 40 -> if (isBn) "পাহাড় দুর্গ" else "Hill Fortress"
            abs(x) < 35 && abs(z) < 35 -> if (isBn) "ধানমন্ডি লেক ঘাট" else "Lake Harbor"
            else -> if (isBn) "যুদ্ধক্ষেত্র চত্বর" else "Battleground Plains"
        }
    }

    // --- GENERATORS ---

    private fun generateArenaWorld(): List<WorldBuilding> {
        val list = mutableListOf<WorldBuilding>()
        // Bunkers
        list.add(WorldBuilding(BuildingType.BUNKER, Vector3(25f, 0f, 20f), 10f, 4.5f, 8f, 0xFF546E7A, 0xFF37474F))
        list.add(WorldBuilding(BuildingType.BUNKER, Vector3(-30f, 0f, -25f), 12f, 4.5f, 10f, 0xFF546E7A, 0xFF37474F))
        list.add(WorldBuilding(BuildingType.BUNKER, Vector3(-20f, 0f, 35f), 9f, 4.5f, 7f, 0xFF455A64, 0xFF263238))

        // Watchtowers
        list.add(WorldBuilding(BuildingType.WATCHTOWER, Vector3(45f, 0f, -30f), 5f, 11f, 5f, 0xFF795548, 0xFF4E342E))
        list.add(WorldBuilding(BuildingType.WATCHTOWER, Vector3(-45f, 0f, 40f), 5f, 11f, 5f, 0xFF795548, 0xFF4E342E))

        // Crate Stacks
        list.add(WorldBuilding(BuildingType.CRATE_STACK, Vector3(8f, 0f, -12f), 4f, 2.5f, 4f, 0xFF8D6E63))
        list.add(WorldBuilding(BuildingType.CRATE_STACK, Vector3(-14f, 0f, 10f), 5f, 2.8f, 3f, 0xFF8D6E63))
        list.add(WorldBuilding(BuildingType.CRATE_STACK, Vector3(35f, 0f, 45f), 6f, 3f, 4f, 0xFF6D4C41))

        // Forest Trees scattered around
        val treeCoords = listOf(
            Pair(-15f, -35f), Pair(18f, -40f), Pair(40f, 15f),
            Pair(-35f, 15f), Pair(50f, -10f), Pair(-50f, -10f),
            Pair(10f, 45f), Pair(-25f, -50f), Pair(30f, -55f),
            Pair(-45f, -30f), Pair(55f, 35f), Pair(-10f, 60f)
        )
        for ((tx, tz) in treeCoords) {
            list.add(WorldBuilding(BuildingType.TREE, Vector3(tx, 0f, tz), 3.5f, 9f, 3.5f, 0xFF5D4037))
        }

        return list
    }

    private fun generateInitialLoot(): List<LootItem> {
        val list = mutableListOf<LootItem>()
        var id = 1
        val spawns = listOf(
            Pair(Vector3(12f, 0f, -10f), LootType.HEALTH_PACK),
            Pair(Vector3(-10f, 0f, 14f), LootType.SHIELD_PACK),
            Pair(Vector3(5f, 0f, 18f), LootType.BOMB_PACK),
            Pair(Vector3(-22f, 0f, -15f), LootType.AMMO_PACK),
            Pair(Vector3(28f, 0f, 15f), LootType.HEALTH_PACK),
            Pair(Vector3(-28f, 0f, -20f), LootType.SHIELD_PACK),
            Pair(Vector3(40f, 0f, -25f), LootType.AMMO_PACK),
            Pair(Vector3(-38f, 0f, 30f), LootType.BOMB_PACK)
        )
        for ((pos, type) in spawns) {
            list.add(LootItem(id++, pos, type, amount = if (type == LootType.BOMB_PACK) 2 else 1))
        }
        return list
    }

    private fun generateEnemyBots(count: Int): List<EnemyBot> {
        val botNamesBn = listOf("সৈনিক রফিক", "কমান্ডার টাইগার", "শার্পশুটার রনি", "রণবীর", "শিকারি তামিম", "ঘাতক সাজিদ", "বিদ্রোহী শাকিল", "লৌহ মানব", "অগ্নিবীর", "কালো চিতা")
        val botNamesEn = listOf("Sgt_Rafi", "Viper_99", "Ghost_Recon", "Shadow_X", "Hunter_T", "HawkEye", "Storm_Breaker", "Iron_Wolf", "Cobra_1", "Blaze")

        val weapons = listOf(WeaponType.ASSAULT_RIFLE, WeaponType.SHOTGUN, WeaponType.SNIPER)
        val skins = SkinType.values()

        return (1..count).map { i ->
            val angle = (i.toFloat() / count) * 2 * PI.toFloat()
            val dist = Random.nextFloat() * 70f + 25f
            val bx = sin(angle) * dist
            val bz = cos(angle) * dist
            val isBn = _uiState.value.isBengali
            val name = if (isBn) botNamesBn[(i - 1) % botNamesBn.size] + "_$i"
            else botNamesEn[(i - 1) % botNamesEn.size] + "_$i"

            EnemyBot(
                id = i,
                name = name,
                position = Vector3(bx, 0f, bz),
                yaw = Random.nextFloat() * 2 * PI.toFloat(),
                health = 100f,
                weapon = weapons.random(),
                skin = skins.random()
            )
        }
    }
}
