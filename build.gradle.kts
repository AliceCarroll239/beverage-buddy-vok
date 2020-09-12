import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// The Beverage Buddy sample project ported to Kotlin.
// Original project: https://github.com/vaadin/beverage-starter-flow

val vaadinonkotlin_version = "0.8.2"
val vaadin_version = "14.3.5"

plugins {
    kotlin("jvm") version "1.4.10"
    id("org.gretty") version "3.0.3"  // https://github.com/gretty-gradle-plugin/gretty
    war
    id("com.vaadin") version "0.8.0"
}

defaultTasks("clean", "build")

repositories {
    jcenter()  // doesn't work with mavenCentral(): Gretty won't find its gretty-runner-jetty94
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in Travis-CI console.
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val staging by configurations.creating

dependencies {
    implementation("com.vaadin:vaadin-core:$vaadin_version") {
        // Webjars are only needed when running in Vaadin 13 compatibility mode
        listOf("com.vaadin.webjar", "org.webjars.bowergithub.insites",
                "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents")
                .forEach { exclude(group = it) }
    }
    // Vaadin-on-Kotlin dependency, includes Vaadin
    implementation("eu.vaadinonkotlin:vok-framework-v10-vokdb:$vaadinonkotlin_version")
    // newest Karibu-DSL
    implementation("com.github.mvysny.karibudsl:karibu-dsl:1.0.2")
    implementation("com.zaxxer:HikariCP:3.4.5")
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    implementation(kotlin("stdlib-jdk8"))

    // logging
    // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
    implementation("org.slf4j:slf4j-simple:1.7.30")

    // db
    implementation("org.flywaydb:flyway-core:6.1.4")
    implementation("com.h2database:h2:1.4.200") // remove this and replace it with a database driver of your choice.

    // REST
    implementation("eu.vaadinonkotlin:vok-rest:$vaadinonkotlin_version")

    // testing
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:1.2.5")
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:0.17")
    testImplementation("eu.vaadinonkotlin:vok-rest-client:$vaadinonkotlin_version")
    testImplementation("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")

    // heroku app runner
    staging("com.heroku:webapp-runner-main:9.0.36.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

// Heroku
tasks {
    val copyToLib by registering(Copy::class) {
        into("$buildDir/server")
        from(staging) {
            include("webapp-runner*")
        }
    }
    val stage by registering {
        dependsOn("build", copyToLib)
    }
}

vaadin {
    if (gradle.startParameter.taskNames.contains("stage")) {
        productionMode = true
    }
    pnpmEnable = true
}
