plugins {
    id("java-library")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.minebench.de/")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation("de.themoep:inventorygui:1.6.6-SNAPSHOT")
    implementation("org.luaj:luaj-jse:3.0.1")
    testImplementation(libs.paper.api)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
    }
    build {
        dependsOn("shadowJar")
    }

    shadowJar {
        relocate("de.themoep.inventorygui", "uk.akkiserver.otogi.libs.inventorygui")
        relocate("org.luaj", "uk.akkiserver.otogi.libs.luaj")
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
