plugins {
    id("java")
    id("application")
    id("org.beryx.jlink") version "2.25.0"
}

group = "tech.lucasz"
version = "0.0.1"
description = "The server for a Java Jam 2022 submission"

sourceSets {
    main {
        java.setSrcDirs(listOf("main/java"))
        resources.setSrcDirs(listOf("main/resources"))
    }

    test {
        java.setSrcDirs(listOf("test/java"))
        resources.setSrcDirs(listOf("test/resources"))
    }
}

application {
    mainModule.set("partyhouse.hserver")
    mainClass.set("tech.fastj.partyhouse.Main")
}

repositories.maven { setUrl("https://jitpack.io/") }
repositories.mavenCentral()

dependencies.implementation(libs.bundles.fastj)
dependencies.implementation(projects.network)
dependencies.implementation(projects.hcore)

dependencies.testImplementation(dependencies.platform("org.junit:junit-bom:5.8.2"))
dependencies.testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
dependencies.testRuntimeOnly("org.junit.platform:junit-platform-launcher")

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

jlink {

    options.addAll(
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
        "--compress", "1"
    )

    launcher {
        noConsole = false
    }

    forceMerge("slf4j-api", "slf4j-simple", "gson", "flatlaf")

    jpackage {
        /* Use this to define the path of the icons for your project. */
        val iconPath = "project-resources/fastj_icon"
        val currentOs = org.gradle.internal.os.OperatingSystem.current()

        addExtraDependencies("slf4j", "gson")

        installerOptions.addAll(
            listOf(
                "--name", "partyhouse",
                "--description", project.description as String,
                "--vendor", project.group as String,
                "--app-version", project.version as String,
                "--license-file", "$rootDir/LICENSE.md",
                "--copyright", "Copyright (c) 2022 Andrew Dey",
                "--vendor", "Andrew Dey"
            )
        )

        when {
            currentOs.isWindows -> {
                installerType = "msi"
                imageOptions = listOf("--icon", "${iconPath}.ico")
                installerOptions.addAll(
                    listOf(
                        "--win-per-user-install",
                        "--win-dir-chooser",
                        "--win-shortcut"
                    )
                )
            }
            currentOs.isLinux -> {
                installerType = "deb"
                imageOptions = listOf("--icon", "${iconPath}.png")
                installerOptions.add("--linux-shortcut")
            }
            currentOs.isMacOsX -> {
                installerType = "pkg"
                imageOptions = listOf("--icon", "${iconPath}.icns")
                installerOptions.addAll(
                    listOf(
                        "--mac-package-name", project.name
                    )
                )
            }
        }
    }
}

tasks.named("jpackageImage") {
    doLast {
        copy {
            from("audio").include("*.*")
            into("$buildDir/jpackage/partyhouse/audio")
        }
        copy {
            from("img").include("*.*")
            into("$buildDir/jpackage/partyhouse/img")
        }
        copy {
            from("json").include("*.*")
            into("$buildDir/jpackage/partyhouse/json")
        }
        delete(fileTree("$buildDir/jpackage/partyhouse/runtime") {
            include("release", "bin/api**.dll", "bin/partyhouse**", "lib/jrt-fs.jar")
        })
    }
}

