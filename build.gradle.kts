import org.apache.tools.ant.taskdefs.condition.Os

val isCiBuild = providers.environmentVariable("CI").map { it.toBoolean() }.orElse(false).get()
val isSnapshot = providers.gradleProperty("isSnapshot").map { it.toBoolean() }.orElse(false).get()

val groupUrl = rootProject.group.toString().replace(".", "/")

val baseVersion = project.version.toString()
project.version = if (isSnapshot) "$baseVersion-SNAPSHOT" else if (!isCiBuild) "$baseVersion.local" else baseVersion

repositories {
    mavenCentral()
}

plugins {
    java
    alias(libs.plugins.shadow)

    `maven-publish`
    signing
}

dependencies {
    implementation(libs.asm.commons)
    implementation(libs.dsljson)

    compileOnly(libs.jspecify)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")

    manifest {
        attributes(
            "Premain-Class" to "dev.aoqia.leaf.proxy.Main",
            "Implementation-Version" to project.version,
            "Enable-Native-Access" to "ALL-UNNAMED"
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Werror"))
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

tasks.withType<Sign>().configureEach {
    enabled = isCiBuild && !isSnapshot
}

tasks.register<Copy>("copyToGame") {
    description = "Copies the JAR to the game to simulate production."

    val path: String = if (Os.isFamily(Os.FAMILY_UNIX)) {
        "projectzomboid"
    } else if (Os.isFamily(Os.FAMILY_MAC)) {
        "Contents/Java"
    } else {
        ""
    }

    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(providers.environmentVariable("LEAF_CLIENT_GAME_PATH").map { "$it/$path" })
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["shadow"])
            artifact(tasks.named("sourcesJar"))

            pom {
                name = rootProject.name
                group = rootProject.group
                description = rootProject.description
                url = property("url").toString()
                inceptionYear = "2026"

                developers {
                    developer {
                        id = "aoqia"
                        name = "aoqia"
                        email = "aoqia@aoqia.dev"
                    }
                }

                licenses {
                    license {
                        name = "MIT"
                        url = "https://spdx.org/licenses/MIT.html"
                    }
                }
            }
        }

        repositories {
            maven {
                name = "leaf"
                url = uri("https://maven.aoqia.dev/${if (isSnapshot) "snapshots" else "releases"}")

                credentials {
                    username = providers.gradleProperty("mavenUsername").orNull
                    password = providers.gradleProperty("mavenPassword").orNull
                }

                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }

    signing {
        isRequired = isCiBuild and !isSnapshot

        val signingKey = providers.gradleProperty("signingKey")
        val signingPassword = providers.gradleProperty("signingPassword")
        if (signingKey.isPresent && signingPassword.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
        }

        sign(publishing.publications)
    }
}
