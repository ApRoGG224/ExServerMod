plugins {
    id("net.fabricmc.fabric-loom") version "1.19.2"
    kotlin("jvm") version "2.4.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

val modVersion = project.property("mod_version") as String
val mavenGroup = project.property("maven_group") as String
val modArchiveName = project.property("archives_base_name") as String
val minecraftVersion = project.property("minecraft_version") as String
val loaderVersion = project.property("loader_version") as String
val fabricVersion = project.property("fabric_version") as String
val fabricKotlinVersion = project.property("fabric_kotlin_version") as String

version = modVersion
group = mavenGroup

base {
    archivesName.set(modArchiveName)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.5.0")
    ignoreFailures.set(false)
}
