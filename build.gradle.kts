plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.pitremake"
version = "1.0.0-SNAPSHOT"

val minecraftVersion = "1.8.8"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/nms/")
}

dependencies {
    compileOnly("org.github.paperspigot:paperspigot-api:1.8.8-R0.1-SNAPSHOT")
    // NMS (net.minecraft.server.v1_8_R3) — NBT 操作用
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")

    testImplementation("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        showStandardStreams = true
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion)
    // 1.8.8 サーバーは Java 8 で起動する
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    // 古いビルドの「outdated」警告による 20 秒待機をスキップ
    jvmArgs("-Xms1G", "-Xmx2G", "-DIReallyKnowWhatIAmDoingISwear=true")

    // 1.8.8 は -Dcom.mojang.eula.agree に非対応なので eula.txt を事前に作成する
    doFirst {
        val runDir = runDirectory.get().asFile
        runDir.mkdirs()
        runDir.resolve("eula.txt").writeText("eula=true\n")
    }
}
