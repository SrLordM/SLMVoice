package dev.srlordm.slmvoice

import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val MOD_ID = "slmvoice"
const val VERSION = "0.3.0"
val LOGGER: Logger = LoggerFactory.getLogger("SLM Voice")
val CONFIG get() = Soundboard.config

val GAME_DIR = FabricLoader.getInstance().gameDir
