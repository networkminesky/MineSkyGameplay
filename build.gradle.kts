plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://repo.opencollab.dev/main/")

    maven(url = "https://mvn.lumine.io/repository/maven-public/")

    // Essentials
    maven("https://repo.essentialsx.net/releases/")

    maven("https://jitpack.io")
}

dependencies {
    paperweight.foliaDevBundle("26.1.2.build.+")

    compileOnly("io.lumine:Mythic-Dist:5.12.1")

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")

    compileOnly("com.github.networkminesky:mineskyguildas:60181761df")

    // Essentials
    compileOnly("net.essentialsx:EssentialsX:2.20.1") {
        isTransitive = false
    }

    compileOnly("com.github.networkminesky.mineskyclaims:huskclaims-bukkit:1.5.13-RELEASE")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}