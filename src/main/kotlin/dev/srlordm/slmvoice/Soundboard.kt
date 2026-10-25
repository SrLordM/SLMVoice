package dev.srlordm.slmvoice

import dev.srlordm.slmvoice.audio.BASE_DIR
import dev.srlordm.slmvoice.audio.play.AudioPreparation
import dev.srlordm.slmvoice.audio.registry.ResourceAudioHolder
import dev.srlordm.slmvoice.audio.registry.SoundRegistry
import dev.srlordm.slmvoice.config.AudioConfig
import dev.srlordm.slmvoice.config.SoundboardConfig
import dev.srlordm.slmvoice.entrypoint.SoundboardAccess
import dev.srlordm.slmvoice.gui.EditorAudioPreparation
import dev.srlordm.slmvoice.gui.screen.SoundBrowser
import dev.srlordm.slmvoice.util.client
import dev.srlordm.slmvoice.util.idOf
import dev.srlordm.slmvoice.util.shiftDown
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.util.InputUtil
import net.minecraft.resource.ResourceType
import org.lwjgl.glfw.GLFW
import kotlin.io.path.createDirectories

object Soundboard {
    private var ready = false
    private val pressedSoundKeys = mutableSetOf<String>()
    val config = SoundboardConfig.load()

    fun initialize() {
        if (ready) return
        ready = true

        AudioConfig // Inits the object
        BASE_DIR.createDirectories()
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ResourceAudioHolder)

        ModKeyBinds.keybind(GLFW.GLFW_KEY_J, "browser") {
            inGame { SoundBrowser.open() }
            inGui {
                if (this is SoundBrowser) {
                    if (settings != null) closeSettings()
                    else close()
                }
            }
        }
        ModKeyBinds.keybind(GLFW.GLFW_KEY_U, "cancel") {
            val reset = { SoundboardAccess.forEach { scheduler.reset() } }
            inGame { reset() }
            inGui { reset() }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            SoundboardAccess.forEach { scheduler.shutdown() }
            AudioPreparation.shutdown()
            EditorAudioPreparation.shutdown()
            pressedSoundKeys.clear()
            SoundBrowser.clearSessionMemory()
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (client.currentScreen != null || (config.keybinds.isEmpty() && config.keybindCombos.isEmpty())) {
                pressedSoundKeys.clear()
                return@register
            }

            val window = client.window
            val sounds = (config.keybindCombos.keys + config.keybinds.keys).distinct()
            sounds.forEach { sound ->
                val keys = config.keybind(sound)?.takeIf { combo -> combo.all { it >= GLFW.GLFW_KEY_SPACE } } ?: return@forEach
                val marker = "$sound:${keys.joinToString("+")}"
                val pressed = keys.all { InputUtil.isKeyPressed(window, it) }
                if (pressed && marker !in pressedSoundKeys) {
                    SoundRegistry.update()
                    SoundRegistry[dev.srlordm.slmvoice.audio.data.SoundId(sound)]?.let { entry ->
                        SoundboardAccess.active?.schedule(entry, shiftDown)
                    }
                }
                if (pressed) pressedSoundKeys += marker else pressedSoundKeys -= marker
            }
        }

        ResourceManagerHelper.registerBuiltinResourcePack(
            idOf("default"),
            FabricLoader.getInstance().getModContainer(MOD_ID).get(),
            ResourcePackActivationType.NORMAL
        )
    }
}
