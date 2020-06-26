import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.3.72"
    idea
    java
}

val ktor_version = "1.3.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:${ktor_version}")
    implementation("io.ktor:ktor-server-netty:${ktor_version}")
    implementation("io.ktor:ktor-jackson:${ktor_version}")

    implementation("org.postgresql:postgresql:42.2.12")
    implementation("com.graphql-java:graphql-java:14.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("log4j", "log4j", "1.2.17")


    testImplementation("org.slf4j", "slf4j-log4j12", "1.7.30")
    testImplementation("junit:junit:4.12")
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Main-Class"] = "io.ktor.server.netty.EngineMain"
    }
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    build {
        dependsOn(fatJar)
    }
}
