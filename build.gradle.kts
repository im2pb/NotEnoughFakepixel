/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    kotlin("jvm") version "1.8.21"
    id("gg.essential.loom") version "0.10.0.5"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project

val devEnv: Configuration by configurations.creating {
    configurations.runtimeClasspath.get().extendsFrom(this)
    isCanBeResolved = false
    isCanBeConsumed = false
    isVisible = false
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        
        mixinConfig("mixins.$modid.json")
    }
    
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    java.srcDirs("src/main/java", "src/main/kotlin")
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://mvnrepository.com/repos/central")
    maven("https://maven.architectury.dev/")
    
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    implementation("org.slick2d:slick2d-core:1.0.1")
    shadowImpl("org.reflections:reflections:0.9.12")
    implementation(kotlin("stdlib"))
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    
    devEnv("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    // Carry project and third-party notices inside source and distributable JARs.
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE-NOTENOUGHFAKEPIXEL-LGPL-3.0-or-later.txt" }
    }
    from(rootProject.file("NOTICE.md")) {
        into("META-INF")
        rename { "NOTICE-NOTENOUGHFAKEPIXEL.md" }
    }
    from(rootProject.file("THIRD-PARTY-NOTICES.md")) {
        into("META-INF")
        rename { "THIRD-PARTY-NOTICES-NOTENOUGHFAKEPIXEL.md" }
    }
    from(rootProject.file("CHANGELOG.md")) {
        into("META-INF")
        rename { "CHANGELOG-NOTENOUGHFAKEPIXEL.md" }
    }
    from(rootProject.file("LICENSES")) {
        into("META-INF/licenses")
    }

    archiveBaseName.set("NotEnoughFakepixel")
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)

    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
        expand(inputs.properties)
    }

    rename("(.+_at.cfg)", "META-INF/$1")
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    // Keep the distributable filename stable and explicit for releases.
    archiveFileName.set("NotEnoughFakepixel-1.2.6.jar")
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    manifest.attributes += mapOf(
        "ModSide" to "CLIENT",
        "TweakOrder" to 0,
        "ForceLoadAsMod" to true
    )
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)

    relocate("org.reflections", "$baseGroup.deps.org.reflections")

    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }

    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}

tasks.assemble.get().dependsOn(tasks.remapJar)

tasks.named<net.fabricmc.loom.task.AbstractRunTask>("runClient") {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}


// Prevent new Forge listeners from bypassing FeatureBootstrap lifecycle ownership.
tasks.register("verifyFeatureArchitecture") {
    group = "verification"
    description = "Verifies that every @RegisterEvents and @RegisterInstance declaration has exactly one feature-module owner."

    doLast {
        val bootstrapFile = file("src/main/java/com/nef/notenoughfakepixel/core/feature/FeatureBootstrap.java")
        val sourceRoot = file("src/main/java")
        check(bootstrapFile.isFile) { "FeatureBootstrap.java is missing" }

        val bootstrapText = bootstrapFile.readText(Charsets.UTF_8)
        val packageRoots = bootstrapText.lineSequence().mapNotNull { line ->
            val marker = "BASE + \""
            val start = line.indexOf(marker)
            if (start < 0) {
                null
            } else {
                val valueStart = start + marker.length
                val valueEnd = line.indexOf('\"', valueStart)
                if (valueEnd <= valueStart) null
                else "com.nef.notenoughfakepixel" + line.substring(valueStart, valueEnd)
            }
        }.distinct().toList()

        check(packageRoots.isNotEmpty()) { "No feature package roots were found in FeatureBootstrap.java" }

        val overlappingRoots = packageRoots.flatMapIndexed { index, root ->
            packageRoots.drop(index + 1).filter { candidate ->
                candidate == root || candidate.startsWith(root + ".") || root.startsWith(candidate + ".")
            }.map { candidate -> "$root <-> $candidate" }
        }
        check(overlappingRoots.isEmpty()) {
            "Feature module package roots overlap:\n" + overlappingRoots.joinToString("\n")
        }

        fun packageName(source: File): String {
            val declaration = source.readLines(Charsets.UTF_8).firstOrNull {
                it.trim().startsWith("package ")
            } ?: return ""
            return declaration.trim().removePrefix("package ").removeSuffix(";").trim()
        }

        fun isOwned(source: File, roots: List<String>): Boolean {
            val pkg = packageName(source)
            return roots.any { pkg == it || pkg.startsWith(it + ".") }
        }

        val javaFiles = fileTree(sourceRoot) { include("**/*.java") }.files
        val eventListeners = javaFiles.filter { source ->
            source.readLines(Charsets.UTF_8).any { it.trimStart().startsWith("@RegisterEvents") }
        }
        val uncoveredEvents = eventListeners.filterNot { isOwned(it, packageRoots) }
        val multiOwnedEvents = eventListeners.filter { source ->
            val pkg = packageName(source)
            packageRoots.count { pkg == it || pkg.startsWith(it + ".") } > 1
        }

        val singletonFields = javaFiles.filter { source ->
            source.readLines(Charsets.UTF_8).any { it.trimStart().startsWith("@RegisterInstance") }
        }
        val uncoveredSingletons = singletonFields.filterNot { isOwned(it, packageRoots) }
        val multiOwnedSingletons = singletonFields.filter { source ->
            val pkg = packageName(source)
            packageRoots.count { pkg == it || pkg.startsWith(it + ".") } > 1
        }

        check(uncoveredEvents.isEmpty()) {
            "Unowned @RegisterEvents declarations:\n" + uncoveredEvents.joinToString("\n") { it.path }
        }
        check(multiOwnedEvents.isEmpty()) {
            "Overlapping module ownership for @RegisterEvents declarations:\n" + multiOwnedEvents.joinToString("\n") { it.path }
        }
        check(uncoveredSingletons.isEmpty()) {
            "Unowned @RegisterInstance declarations:\n" + uncoveredSingletons.joinToString("\n") { it.path }
        }
        check(multiOwnedSingletons.isEmpty()) {
            "Overlapping module ownership for @RegisterInstance declarations:\n" + multiOwnedSingletons.joinToString("\n") { it.path }
        }

        // Diana is explicitly constructed by DianaFeatureModule, not package-scanned. Keep this
        // ownership visible to the architecture check so it cannot silently fall back to duplicate registration.
        val dianaModuleFile = file("src/main/java/com/nef/notenoughfakepixel/features/skyblock/diana/DianaFeatureModule.java")
        val dianaBootstrapOwned = bootstrapText.contains("new DianaFeatureModule()")
        check(dianaModuleFile.isFile && dianaBootstrapOwned) {
            "Diana must be registered through its explicit DianaFeatureModule."
        }
        val dianaModuleText = dianaModuleFile.readText(Charsets.UTF_8)
        check(dianaModuleText.contains("new Diana(newRuntime)") &&
                dianaModuleText.contains("new GuessBurrow(newRuntime)") &&
                dianaModuleText.contains("context.registerEventListener(newDianaListener)") &&
                dianaModuleText.contains("context.registerEventListener(newGuessListener)")) {
            "DianaFeatureModule must construct and register both Diana listeners explicitly."
        }
        // GuessBurrow was moved into the dedicated guess package during the modular refactor.
        // Keep this list aligned with the actual source locations; a missing file is a separate
        // architecture error, not evidence of duplicate @RegisterEvents discovery.
        val dianaListenerFiles = listOf(
            file("src/main/java/com/nef/notenoughfakepixel/features/skyblock/diana/Diana.java"),
            file("src/main/java/com/nef/notenoughfakepixel/features/skyblock/diana/guess/GuessBurrow.java")
        )
        check(dianaListenerFiles.all { it.isFile }) {
            "An explicitly-owned Diana listener source file is missing. Expected: " +
                    dianaListenerFiles.filterNot { it.isFile }.joinToString(", ") { it.path }
        }
        val packageScannedDianaListeners = dianaListenerFiles.filter { listenerFile ->
            listenerFile.readLines(Charsets.UTF_8).any {
                it.trimStart().startsWith("@RegisterEvents")
            }
        }
        check(packageScannedDianaListeners.isEmpty()) {
            "Explicitly-owned Diana listeners must not also use @RegisterEvents package discovery:\n" +
                    packageScannedDianaListeners.joinToString("\n") { it.path }
        }
        val progressionDeclaration = bootstrapText.substringAfter("module(\"skyblock-progression\"")
                .substringBefore("));")
        check(!progressionDeclaration.contains("features.skyblock.diana")) {
            "Diana must not also be owned by skyblock-progression."
        }

        println("Feature architecture verified: ${eventListeners.size} discovered event listener types, " +
                "${singletonFields.size} singleton field source files, ${packageRoots.size} package roots, and explicit Diana ownership.")
    }
}

tasks.named("check") {
    dependsOn("verifyFeatureArchitecture")
}
