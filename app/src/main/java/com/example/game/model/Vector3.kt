package com.example.game.model

import kotlin.math.*

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else Vector3()

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun distanceTo(other: Vector3): Float = (this - other).length()
    fun distance2D(other: Vector3): Float = sqrt((x - other.x).pow(2) + (z - other.z).pow(2))

    fun normalized(): Vector3 {
        val l = length()
        return if (l > 0.0001f) this * (1f / l) else Vector3(0f, 0f, 0f)
    }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
    }
}
