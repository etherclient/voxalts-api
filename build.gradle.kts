plugins {
    id("java")
    id("maven-publish")
}

// Toolchains:
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Dependencies:
repositories {
    mavenCentral()
}

val annotationImplementation: Configuration by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
    configurations.testCompileOnly.get().extendsFrom(this)
    configurations.annotationProcessor.get().extendsFrom(this)
    configurations.testAnnotationProcessor.get().extendsFrom(this)
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.2")
    compileOnly("org.jetbrains:annotations:26.0.2")
    annotationImplementation("org.projectlombok:lombok:1.18.36")
}

// Task:
tasks.compileJava {
    options.encoding = "UTF-8"
}

// Publishing:
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "me.darragh"
            artifactId = "ptalts-api"
            version = project.version.toString()

            pom {
                name.set("ptalts-api")
                properties.set(mapOf(
                    "java.version" to "17",
                    "project.build.sourceEncoding" to "UTF-8",
                    "project.reporting.outputEncoding" to "UTF-8"
                ))
                developers {
                    developer {
                        id.set("darraghd493")
                        name.set("Darragh")
                    }
                }
                organization {
                    name.set("darragh.website")
                    url.set("https://darragh.website")
                }
                scm {
                    connection.set("scm:git:git://github.com/etherclient/ptalts-api.git")
                    developerConnection.set("scm:git:ssh://github.com/etherclient/ptalts-api.git")
                    url.set("https://github.com/etherclient/ptalts-api")
                }
            }

            java {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            url = uri("https://repo.darragh.website/releases")
            credentials {
                username = System.getenv("REPO_TOKEN")
                password = System.getenv("REPO_SECRET")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}