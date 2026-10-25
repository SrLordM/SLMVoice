package dev.srlordm.slmvoice.gui.screen

import dev.srlordm.slmvoice.util.currentScreen
import net.minecraft.client.gui.screen.Screen
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

abstract class ScreenManager(private val cls: KClass<out Screen>) {
    fun open() {
        currentScreen = cls.createInstance()
    }

    fun close() = currentScreen?.let { if (it::class == cls) it.close() }
}
