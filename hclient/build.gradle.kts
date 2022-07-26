plugins {
    id("java")
    id("application")
}

group = "tech.lucasz"
version = "0.0.1"

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
    mainModule.set("partyhouse.hclient")
    mainClass.set("tech.fastj.partyhouse.Main")
}

repositories.maven { setUrl("https://jitpack.io/") }
repositories.mavenCentral()

dependencies.implementation(libs.bundles.fastj)
dependencies.implementation(projects.network)
dependencies.implementation(projects.hcore)
dependencies.implementation("com.formdev:flatlaf:2.3")

dependencies.testImplementation(dependencies.platform("org.junit:junit-bom:5.8.2"))
dependencies.testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
dependencies.testRuntimeOnly("org.junit.platform:junit-platform-launcher")

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}