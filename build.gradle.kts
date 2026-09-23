import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
}

configurations.all {
    exclude(group = "org.spigotmc")
    resolutionStrategy {
        force("net.kyori:adventure-api:5.2.0")
        force("net.kyori:adventure-text-minimessage:5.2.0")
        force("net.kyori:adventure-text-serializer-legacy:5.2.0")
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io")
}

// Task 100% compatível com o Configuration Cache do Gradle 9
abstract class StripJarTask : DefaultTask() {

    @get:InputFiles
    abstract val inputJar: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun execute() {
        val src = inputJar.singleFile
        val dest = outputJar.get().asFile
        dest.parentFile.mkdirs()

        val excludes = listOf(
            "net/kyori/",
            "org/bukkit/",
            "io/papermc/",
            "com/destroystokyo/paper/"
        )

        val seenEntries = HashSet<String>()

        ZipFile(src).use { zipIn ->
            ZipOutputStream(dest.outputStream().buffered()).use { zipOut ->
                for (entry in zipIn.entries()) {
                    val name = entry.name
                    // Se pertencer ao adventure ou bukkit antigos, pula a cópia
                    if (excludes.any { name.startsWith(it) } || !seenEntries.add(name)) {
                        continue
                    }

                    val newEntry = ZipEntry(name)
                    newEntry.time = entry.time
                    zipOut.putNextEntry(newEntry)

                    if (!entry.isDirectory) {
                        zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }
}

// 1. Configuração que baixa o HuskClaims bruto
val huskClaimsRaw by configurations.creating

// 2. Registro da task sem closures externas
val stripHuskClaims by tasks.registering(StripJarTask::class) {
    inputJar.from(huskClaimsRaw)
    outputJar.set(layout.buildDirectory.file("clean-libs/huskclaims-clean.jar"))
}

dependencies {
    paperweight.foliaDevBundle("26.2.build.+")

    compileOnly("net.kyori:adventure-api:5.2.0")

    compileOnly("io.lumine:Mythic-Dist:5.12.1") {
        isTransitive = false
    }

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2") {
        isTransitive = false
    }

    compileOnly("com.github.networkminesky:mineskyguildas:60181761df") {
        isTransitive = false
    }

    compileOnly("net.essentialsx:EssentialsX:2.20.1") {
        isTransitive = false
    }

    // 3. Pede o HuskClaims na configuração isolada
    huskClaimsRaw("com.github.networkminesky.mineskyclaims:huskclaims-bukkit:1.5.13-RELEASE") {
        isTransitive = false
    }

    // 4. Passa a saída do JAR higienizado para a compilação
    compileOnly(files(stripHuskClaims.flatMap { it.outputJar }))
}

tasks.named("compileJava") {
    dependsOn(stripHuskClaims)
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
