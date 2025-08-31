import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.yaml:snakeyaml:1.26")
        classpath("com.google.code.gson:gson:2.8.6")
    }
}

plugins {
    id("java")
    id("eclipse")
    id("idea")
}

version = "2.8.0"
group = "net.tcpshield.tcpshield"

base {
    archivesName.set("TCPShield")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

sourceSets {
    named("main") {
        java.srcDir("src/main/java")
        resources.srcDir("src/main/resources")
    }
    named("test") {
        java.srcDir("src/test/java")
        resources.srcDir("src/test/resources")
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
}

repositories {
    maven { url = uri("https://repo1.maven.org/maven2/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.dmulloy2.net/nexus/repository/public/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.opencollab.dev/maven-snapshots/") }
}

dependencies {
    compileOnly(files("libs/ProtocolLib-5.0.jar")) // Import Legacy ProtocolLib API.
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.14-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0-M1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0-M1")
    compileOnly("org.geysermc.floodgate:api:2.1.1-SNAPSHOT")
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

configurations {
    named("testImplementation") {
        extendsFrom(configurations.named("compileOnly").get())
    }
}

fun updateYamls() {
    val options = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        setSplitLines(false)
        setPrettyFlow(true)
    }
    val yaml = Yaml(options)
    val files = listOf(
        File("src/main/resources/plugin.yml"),
        File("src/main/resources/bungee.yml")
    )
    files.forEach { file ->
        file.inputStream().use { inputStream ->
            @Suppress("UNCHECKED_CAST")
            val cfg = yaml.load<MutableMap<String, Any?>>(inputStream)
            cfg["version"] = project.version.toString()
            FileWriter(file).use { writer ->
                yaml.dump(cfg, writer)
            }
        }
    }
}

fun updateJsons() {
    val file = File("src/main/resources/velocity-plugin.json")
    file.inputStream().use { inputStream ->
        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
            val obj: JsonObject = JsonParser.parseReader(reader).asJsonObject
            obj.addProperty("version", project.version.toString())
            FileWriter(file).use { writer ->
                writer.write(obj.toString())
            }
        }
    }
}

tasks.register("updateVersion") {
    doLast {
        updateYamls()
        updateJsons()
    }
}

tasks.named("build") {
    dependsOn("updateVersion")
}

