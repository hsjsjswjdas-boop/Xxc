package com.example.game.model

enum class WeaponType(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val damage: Float,
    val fireRate: Float, // shots per sec
    val maxAmmo: Int,
    val reloadTimeSec: Float,
    val range: Float,
    val spread: Float,
    val bulletSpeed: Float,
    val colorHex: Long
) {
    ASSAULT_RIFLE(
        id = "ak47",
        nameBn = "একে-৪৭ রাইফেল",
        nameEn = "AK-47 Assault",
        damage = 28f,
        fireRate = 7.5f,
        maxAmmo = 30,
        reloadTimeSec = 1.8f,
        range = 75f,
        spread = 0.04f,
        bulletSpeed = 80f,
        colorHex = 0xFFFFB02E
    ),
    SHOTGUN(
        id = "shotgun",
        nameBn = "চরম শটগান",
        nameEn = "Combat Shotgun",
        damage = 65f,
        fireRate = 1.5f,
        maxAmmo = 8,
        reloadTimeSec = 2.2f,
        range = 25f,
        spread = 0.12f,
        bulletSpeed = 65f,
        colorHex = 0xFFFF5722
    ),
    SNIPER(
        id = "sniper",
        nameBn = "এডাব্লিউএম স্নাইপার",
        nameEn = "AWM Sniper",
        damage = 95f,
        fireRate = 0.9f,
        maxAmmo = 5,
        reloadTimeSec = 2.6f,
        range = 140f,
        spread = 0.008f,
        bulletSpeed = 130f,
        colorHex = 0xFF00E5FF
    ),
    KATANA(
        id = "katana",
        nameBn = "ধারালো কাতানা",
        nameEn = "Shadow Katana",
        damage = 55f,
        fireRate = 2.2f,
        maxAmmo = 999,
        reloadTimeSec = 0.1f,
        range = 4f,
        spread = 0f,
        bulletSpeed = 0f,
        colorHex = 0xFF76FF03
    ),
    ROCKET_LAUNCHER(
        id = "rpg",
        nameBn = "আরপিজি রকেট",
        nameEn = "RPG Launcher",
        damage = 110f,
        fireRate = 0.7f,
        maxAmmo = 3,
        reloadTimeSec = 3.0f,
        range = 100f,
        spread = 0.02f,
        bulletSpeed = 45f,
        colorHex = 0xFFFF1744
    );

    fun getDisplayName(isBengali: Boolean): String = if (isBengali) nameBn else nameEn
}

enum class SkinType(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val accentColor: Long
) {
    COMMANDO(
        id = "commando",
        nameBn = "কমান্ডো সারভাইভার",
        nameEn = "Commando Survivor",
        primaryColor = 0xFF2E7D32,
        secondaryColor = 0xFF1B5E20,
        accentColor = 0xFFFFB02E
    ),
    CYBER_NINJA(
        id = "cyber_ninja",
        nameBn = "সাইবার যোদ্ধা",
        nameEn = "Cyber Ninja",
        primaryColor = 0xFF00B0FF,
        secondaryColor = 0xFF0D47A1,
        accentColor = 0xFF00E5FF
    ),
    DESERT_REBEL(
        id = "desert_rebel",
        nameBn = "মরুভূমির বিদ্রোহী",
        nameEn = "Desert Rebel",
        primaryColor = 0xFFD87D38,
        secondaryColor = 0xFF8D4F1E,
        accentColor = 0xFFFFD54F
    ),
    GOLD_VETERAN(
        id = "gold_veteran",
        nameBn = "স্বর্ণ যোদ্ধা",
        nameEn = "Golden Veteran",
        primaryColor = 0xFFFFD700,
        secondaryColor = 0xFFB8860B,
        accentColor = 0xFFFFF8E1
    );

    fun getDisplayName(isBengali: Boolean): String = if (isBengali) nameBn else nameEn
}

enum class LootType(val nameBn: String, val nameEn: String, val colorHex: Long) {
    HEALTH_PACK("চিকিৎসা কিট", "Medkit", 0xFF00E676),
    SHIELD_PACK("বর্ম শিল্ড", "Shield Vest", 0xFF00B0FF),
    BOMB_PACK("হ্যান্ড বোমা", "Frag Grenade", 0xFFFF9100),
    AMMO_PACK("গুলি বক্স", "Ammo Crate", 0xFFFFEA00),
    AIRDROP_WEAPON("এয়ারড্রপ অস্ত্র", "Special Weapon", 0xFFFF1744);

    fun getDisplayName(isBengali: Boolean): String = if (isBengali) nameBn else nameEn
}
