package com.example.game.model

data class PlayerState(
    var position: Vector3 = Vector3(0f, 0f, 0f),
    var velocity: Vector3 = Vector3(0f, 0f, 0f),
    var yaw: Float = 0f,     // horizontal rotation in radians
    var pitch: Float = 0f,   // vertical tilt in radians
    var health: Float = 100f,
    val maxHealth: Float = 100f,
    var shield: Float = 50f,
    val maxShield: Float = 100f,
    var bombs: Int = 3,
    var currentWeapon: WeaponType = WeaponType.ASSAULT_RIFLE,
    var ammo: Int = WeaponType.ASSAULT_RIFLE.maxAmmo,
    var isReloading: Boolean = false,
    var reloadTimer: Float = 0f,
    var isJumping: Boolean = false,
    var isAiming: Boolean = false,
    var kills: Int = 0,
    var damageDealt: Float = 0f,
    var survivalTime: Float = 0f,
    var skin: SkinType = SkinType.COMMANDO,
    var isFiring: Boolean = false,
    var fireCooldown: Float = 0f,
    var walkAnimTime: Float = 0f,
    var hitFlashTimer: Float = 0f
)

enum class BotState {
    PATROL,
    CHASE_PLAYER,
    ENGAGE_ENEMY,
    FLEE_STORM,
    DEAD
}

data class EnemyBot(
    val id: Int,
    val name: String,
    var position: Vector3,
    var velocity: Vector3 = Vector3(0f, 0f, 0f),
    var yaw: Float = 0f,
    var health: Float = 100f,
    val maxHealth: Float = 100f,
    var weapon: WeaponType = WeaponType.ASSAULT_RIFLE,
    var state: BotState = BotState.PATROL,
    var targetPosition: Vector3 = Vector3(0f, 0f, 0f),
    var fireCooldown: Float = 0f,
    var reactionTimer: Float = 0f,
    var walkAnimTime: Float = 0f,
    var skin: SkinType = SkinType.COMMANDO,
    var isDead: Boolean = false,
    var deathTimer: Float = 0f,
    var kills: Int = 0
)

data class Projectile(
    var position: Vector3,
    var velocity: Vector3,
    val isGrenade: Boolean = false,
    val isRocket: Boolean = false,
    var fuseTimer: Float = 2.4f,
    val ownerId: Int, // 0 for player, >0 for bot
    val damage: Float,
    val radius: Float = 1f,
    var lifeTime: Float = 4f
)

enum class ParticleType {
    SPARK,
    BLAST_RING,
    SMOKE,
    FIRE,
    BLOOD,
    HEAL,
    VICTORY_CONFETTI
}

data class Particle(
    var position: Vector3,
    var velocity: Vector3,
    val colorHex: Long,
    var size: Float,
    var life: Float,
    val maxLife: Float,
    val type: ParticleType
)

data class Airdrop(
    var position: Vector3,
    val targetY: Float = 0f,
    var isFalling: Boolean = true,
    var parachuteOpen: Boolean = true,
    var isLooted: Boolean = false,
    var smokeTimer: Float = 40f
)

data class LootItem(
    val id: Int,
    var position: Vector3,
    val type: LootType,
    val weapon: WeaponType? = null,
    val amount: Int = 1,
    var isCollected: Boolean = false,
    var hoverOffset: Float = 0f
)

enum class BuildingType {
    BUNKER,
    WATCHTOWER,
    CRATE_STACK,
    WALL,
    TREE
}

data class WorldBuilding(
    val type: BuildingType,
    val position: Vector3,
    val width: Float,
    val height: Float,
    val depth: Float,
    val colorHex: Long,
    val roofColorHex: Long = 0xFF37474F
)

data class SafeZone(
    var centerX: Float = 0f,
    var centerZ: Float = 0f,
    var currentRadius: Float = 130f,
    var targetRadius: Float = 130f,
    var shrinkSpeed: Float = 3f,
    var countdownSec: Float = 20f,
    var wave: Int = 1,
    val maxWaves: Int = 4,
    var isShrinking: Boolean = false
)

data class KillFeedItem(
    val killer: String,
    val victim: String,
    val weaponName: String,
    val isPlayerKiller: Boolean,
    val isPlayerVictim: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)
