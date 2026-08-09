plugins {
    id("java-library")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.minebench.de/")
    maven("https://repo.mikigal.pl/releases")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation("de.themoep:inventorygui:1.6.6-SNAPSHOT")
    implementation("pl.mikigal:ConfigAPI:1.2.6")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    build {
        dependsOn(shadow)
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
