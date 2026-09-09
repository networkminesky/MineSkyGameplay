plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://repo.opencollab.dev/main/")

    maven(url = "https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    paperweight.foliaDevBundle("26.1.2.build.+")

    compileOnly("io.lumine:Mythic-Dist:5.12.1")

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
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