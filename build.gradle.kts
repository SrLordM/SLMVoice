plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.yamlang)
    alias(libs.plugins.mpp)
}

class ModData {
    val id: String = project.property("id").toString()
    val name: String = project.property("name").toString()
    val group: String = project.property("group").toString()
    val version: String = project.property("version").toString()
}

val mod = ModData()

version = "${mod.version}+${libs.versions.minecraft.get()}"
group = mod.group
base { archivesName.set(mod.id) }

allprojects {
    repositories {
        fun strictMaven(url: String, vararg groups: String) = exclusiveContent {
            forRepository { maven(url) }
            filter { groups.forEach(::includeGroup) }
        }
        mavenCentral()
        maven("https://jitpack.io")
        strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
        strictMaven("https://maven.wispforest.io", "io.wispforest", "io.wispforest.endec")
        strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc")
        strictMaven("https://maven.maxhenkel.de/releases", "de.maxhenkel.voicechat")
        strictMaven("https://repo.plasmoverse.com/snapshots", "su.plo.voice", "su.plo.voice.api", "su.plo.slib")
        strictMaven("https://repo.plasmoverse.com/releases", "su.plo.config")
    }
}

dependencies {
    include(project(":kowoui"))
    implementation(project(path = "kowoui", configuration = "namedElements"))
    runtimeOnly(project(path = "kowoui", configuration = "namedElements"))
    fun modules(vararg modules: String) {
        modules.forEach { modApi(fabricApi.module("fabric-$it", libs.versions.fabric.api.get())) }
    }
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.kotlin)

    libs.okhttp.let {
        include(it)
        api(it)
    }

    include(libs.okio)
    libs.mp3spi.let {
        include(it)
        api(it)
    }
    libs.jlayer.let {
        include(it)
        api(it)
    }
    libs.tritonus.share.let {
        include(it)
        api(it)
    }
    include(libs.owo.sentinel)
    modApi(libs.fabric.api)
    modApi(libs.modmenu)
    modApi(libs.owo) {
        exclude(group = "net.fabricmc")
    }
    compileOnly(libs.simplevc.api)
    modRuntimeOnly(libs.simplevc)
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/slmvoice.accesswidener")

    runConfigs.configureEach {
        ideConfigGenerated(false)
        vmArgs("-Dmixin.debug.export=true", "-Dfabric.log.level=debug")
    }

    decompilers {
        get("vineflower").apply {
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

tasks.processResources {
    exclude("resourcepacks/default/assets/slmvoice_example/slmvoice/metal_pipe.wav")
    inputs.property("version", mod.version)
    inputs.property("minecraft", project.property("mcdep").toString())
    inputs.property("flk", project.property("flk").toString())
    inputs.property("owolib", project.property("owolib").toString())

    val map = mapOf(
        "version" to mod.version,
        "minecraft" to project.property("mcdep").toString(),
        "flk" to project.property("flk").toString(),
        "owolib" to project.property("owolib").toString(),
    )

    filesMatching("fabric.mod.json") { expand(map) }
}

yamlang {
    targetSourceSets.set(mutableListOf(sourceSets["main"]))
    inputDir.set("assets/${mod.id}/lang")
    owolibRichTranslations = true
}

java {
    withSourcesJar()
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
    from(rootProject.file("LICENSES")) {
        into("META-INF/licenses")
    }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
    }
}

tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar") {
    exclude("resourcepacks/default/assets/slmvoice_example/slmvoice/metal_pipe.wav")
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
    from(rootProject.file("LICENSES")) {
        into("META-INF/licenses")
    }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
    }
}
