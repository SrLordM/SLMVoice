package dev.srlordm.slmvoice.util

import dev.srlordm.slmvoice.MOD_ID
import kotlinx.coroutines.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.Vec3d
import org.lwjgl.glfw.GLFW
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.*

typealias Property<T> = KMutableProperty0<T>

fun idOf(path: String): Identifier = Identifier.of(MOD_ID, path)
fun idOf(namespace: String, path: String) = Identifier.of(namespace, path)

@OptIn(DelicateCoroutinesApi::class)
fun runOn(context: CoroutineContext, action: suspend CoroutineScope.() -> Unit) {
    GlobalScope.launch(context, block = action)
}

inline fun ShortArray.reassign(transform: (Short) -> Short) {
    for (i in indices) this[i] = transform(this[i])
}

inline infix fun Boolean.then(action: () -> Unit): Boolean {
    if (this) action()
    return this
}

fun Class<*>.accurateName(): String = simpleName.ifEmpty {
    val parent = if (this == Any::class.java) "Object" else superclass.accurateName()
    "out $parent"
}

fun Path.resolveOrNull(string: String) = runCatching { resolve(string) }.getOrNull()

fun Path.navigate() {
    Util.getOperatingSystem().open(this)
}

val client = MinecraftClient.getInstance()

val shiftDown: Boolean
    get() = InputUtil.isKeyPressed(client.window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
        InputUtil.isKeyPressed(client.window, GLFW.GLFW_KEY_RIGHT_SHIFT)
val ctrlDown: Boolean
    get() = InputUtil.isKeyPressed(client.window, GLFW.GLFW_KEY_LEFT_CONTROL) ||
        InputUtil.isKeyPressed(client.window, GLFW.GLFW_KEY_RIGHT_CONTROL)
val altDown: Boolean
    get() = InputUtil.isKeyPressed(client.window, GLFW.GLFW_KEY_LEFT_ALT) ||
        InputUtil.isKeyPressed(client.window, GLFW.GLFW_KEY_RIGHT_ALT)

var currentScreen
    get() = client.currentScreen
    set(value) {
        client.setScreen(value)
    }

operator fun Vec3d.plus(other: Vec3d) = add(other)
