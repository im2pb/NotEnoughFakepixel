plugins {
    id("fabric-loom") version "1.7.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val minecraftVersion: String by project
val yarnVersion: String by project
val loaderVersion: String by project
val fabricApiVersion: String by project
val modVersion: String by project
val lombok = "org.projectlombok:lombok:1.18.48"
val devAuthEnabled = providers.gradleProperty("devAuth").map(String::toBoolean).getOrElse(false)

group = "com.nef.notenoughfakepixel"
version = modVersion

base {
    archivesName.set("notenoughfakepixel-fabric-$minecraftVersion")
}

repositories {
    mavenCentral()
    maven("https://maven.notenoughupdates.org/releases/") {
        content {
            includeGroup("org.notenoughupdates.moulconfig")
        }
    }
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") {
        content {
            includeGroup("me.djtheredstoner")
        }
    }
}

val shadowModImpl by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

val shadowLibrary by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    compileOnly(lombok)
    annotationProcessor(lombok)

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnVersion:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation(fabricApi.module("fabric-command-api-v2", fabricApiVersion))
    modImplementation(fabricApi.module("fabric-key-binding-api-v1", fabricApiVersion))
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabricApiVersion))
    modImplementation(fabricApi.module("fabric-message-api-v1", fabricApiVersion))
    modImplementation(fabricApi.module("fabric-networking-api-v1", fabricApiVersion))
    modImplementation(fabricApi.module("fabric-resource-loader-v0", fabricApiVersion))
    modImplementation(fabricApi.module("fabric-screen-api-v1", fabricApiVersion))
    // This release targets 1.21, before Modern artifacts were named per Minecraft version.
    shadowModImpl("org.notenoughupdates.moulconfig:modern:3.0.0-beta.15")
    shadowLibrary("org.jetbrains.kotlin:kotlin-stdlib:2.0.0") {
        isTransitive = false
    }
    if (devAuthEnabled) {
        modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/notenoughfakepixel.accesswidener"))

    runs.named("client") {
        property("devauth.enabled", devAuthEnabled.toString())
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.processResources {
    val metadata = mapOf(
        "version" to modVersion,
        "minecraftVersion" to minecraftVersion,
        "loaderVersion" to loaderVersion
    )
    inputs.properties(metadata)
    filesMatching("fabric.mod.json") {
        expand(metadata)
    }
}

tasks.shadowJar {
    configurations = listOf(shadowModImpl, shadowLibrary)
    archiveClassifier.set("dev-shadow")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "com.nef.notenoughfakepixel.deps.moulconfig")
    relocate("kotlin", "com.nef.notenoughfakepixel.deps.kotlin")
    exclude("moulconfig.accesswidener")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
