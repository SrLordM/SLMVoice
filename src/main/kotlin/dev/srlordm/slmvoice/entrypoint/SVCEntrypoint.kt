package dev.srlordm.slmvoice.entrypoint

import de.maxhenkel.voicechat.api.VoicechatClientApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.Event
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent
import dev.srlordm.slmvoice.MOD_ID
import dev.srlordm.slmvoice.LOGGER
import dev.srlordm.slmvoice.Soundboard
import dev.srlordm.slmvoice.audio.play.AudioScheduler
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED

object SVCEntrypoint : SoundboardEntrypoint, VoicechatPlugin, ClientModInitializer {
    private var api: VoicechatClientApi? = null
    private var channel: ClientAudioChannel? = null
    override fun getPluginId() = MOD_ID

    override fun onInitializeClient() {
        SoundboardAccess.register(this)
        Soundboard.initialize()
    }

    override fun registerEvents(reg: EventRegistration) = with(reg) {
        event<ClientVoicechatConnectionEvent> {
            val voiceApi = it.voicechat.takeIf { _ -> it.isConnected }
            val player = MinecraftClient.getInstance().player
            val newChannel = if (voiceApi != null && player != null) {
                runCatching { voiceApi.createStaticAudioChannel(player.uuid) }
                    .onFailure { error -> LOGGER.warn("Failed to create Simple Voice Chat audio channel", error) }
                    .getOrNull()
            } else null
            api = voiceApi.takeIf { newChannel != null }
            connected = newChannel != null
            channel = newChannel
            scheduler.reset()
        }
        event<MergeClientSoundEvent> {
            val extra = scheduler.next() ?: return@event
            if (!scheduler.local) it.mergeAudio(extra)
            channel?.play(extra)
        }
    }

    private inline fun <reified T : Event> EventRegistration.event(priority: Int = 0, noinline action: (T) -> Unit) {
        registerEvent(T::class.java, action, priority)
    }

    override val format = AudioFormat(PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false)
    override var connected = false
        private set
    override val scheduler: AudioScheduler = AudioScheduler(this)
    override val muted: Boolean
        get() = api?.run { isMuted || isDisabled || isDisconnected } ?: true
}
