plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

group = "io.github.lucasstarsz.fastj"
version = "0.0.3"

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

repositories.maven { setUrl("https://jitpack.io/") }
repositories.mavenCentral()

dependencies.implementation(libs.bundles.fastj)

dependencies.testImplementation(dependencies.platform("org.junit:junit-bom:5.8.2"))
dependencies.testImplementation(libs.bundles.unittest)

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java.withJavadocJar()
java.withSourcesJar()

val shouldPublish = System.getenv("ossrhUsername") != null && System.getenv("ossrhPassword") != null

if (shouldPublish) {
    publishing.publications {
        create<MavenPublication>("fastjMultiplayerPublish") {

            groupId = project.group as String?
            version = project.version as String?
            artifactId = "fastj-networking"

            pom {
                name.set("FastJ Networking Library")
                description.set(project.description)
                url.set("https://github.com/fastjengine/FastJ")

                scm {
                    connection.set("scm:git:https://github.com/fastjengine/FastJ.git")
                    developerConnection.set("scm:git:https://github.com/fastjengine/FastJ.git")
                    url.set("https://fastj.tech")
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/fastjengine/FastJ/blob/main/LICENSE.txt")
                    }
                }

                developers {
                    developer {
                        id.set("andrewd")
                        name.set("Andrew Dey")
                        email.set("andrewrcdey@gmail.com")
                    }
                }
            }

            from(components["java"])
        }
    }

    publishing.repositories.maven {
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        credentials.username = System.getenv("ossrhUsername")
        credentials.password = System.getenv("ossrhPassword")
    }

    signing {
        sign(publishing.publications.getByName("fastjMultiplayerPublish"))
    }
}
