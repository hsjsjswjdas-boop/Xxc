package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.model.Vector3
import com.example.game.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("শেষ বাঁচা 3D", appName)
    }

    @Test
    fun `vector3 3D math calculations`() {
        val v1 = Vector3(3f, 0f, 4f)
        assertEquals(5f, v1.length(), 0.001f)

        val v2 = Vector3(0f, 0f, 0f)
        assertEquals(5f, v1.distanceTo(v2), 0.001f)

        val norm = v1.normalized()
        assertEquals(1f, norm.length(), 0.001f)
    }

    @Test
    fun `weapon types and damage specifications`() {
        assertTrue(WeaponType.ASSAULT_RIFLE.damage > 0)
        assertTrue(WeaponType.SNIPER.damage > WeaponType.ASSAULT_RIFLE.damage)
        assertEquals("একে-৪৭ রাইফেল", WeaponType.ASSAULT_RIFLE.getDisplayName(true))
        assertEquals("AK-47 Assault", WeaponType.ASSAULT_RIFLE.getDisplayName(false))
    }
}
