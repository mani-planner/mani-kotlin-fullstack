import io.ktor.plugin.features.*

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.pluginSerialization)
    application
}

group = "ru.workinprogress.mani"
version = "0.0.1-${providers.gradleProperty("BUILD_NUMBER").getOrElse("snapshot")}"

application {
    mainClass.set("ru.workinprogress.mani.ApplicationKt")
    applicationDefaultJvmArgs =
        listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "true"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.swagger)
    implementation(libs.swagger.parser)
    implementation(libs.ktor.swagger.ui)
    implementation(libs.slf4j.api)

    implementation(libs.mongodb.driver.kotlin.coroutine)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)

    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)

    testImplementation(libs.de.flapdoodle.embed.mongo)
}

val copyFrontend = task<Copy>("copyFrontend") {
    val jsBrowserDistribution =
        project(rootProject.projects.composeApp.path).tasks.named("jsBrowserDistribution")
    from(jsBrowserDistribution)
    include("styles.css", "skiko.js", "mani.js", "index.html", "**.wasm", "composeResources/**/*")
    destinationDir = file("$projectDir/build/resources/main/static")
}

project.tasks.find { "processResources" == it.name }!!.dependsOn(copyFrontend)

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
        localImageName.set("mani-backend")
        imageTag.set("0.0.1-${providers.gradleProperty("BUILD_NUMBER").getOrElse("snapshot")}")
        customBaseImage.set("amazoncorretto:21-alpine3.20-jdk")
        externalRegistry.set(
            DockerImageRegistry.externalRegistry(
                username = providers.gradleProperty("REGISTRY_USERNAME"),
                password = providers.gradleProperty("REGISTRY_PASSWORD"),
                project = provider { "mani-backend" },
                hostname = providers.gradleProperty("REGISTRY_HOSTNAME"),
            )
        )
    }
}
