plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // JUnit for testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Garmin FIT SDK from local JAR
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(files("libs/fit.jar")) // ✅ Ensures fit.jar is directly included

    // Example Guava dependency
    implementation("com.google.guava:guava:32.0.1-jre")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("org.example.WebServer")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.WebServer"
    }
    archiveBaseName.set("javachainring")
    archiveVersion.set("0.0.1")
    destinationDirectory.set(file("$rootDir/build/libs")) // ✅ Ensures JAR is placed in `build/libs/`
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("javachainring")
    archiveVersion.set("0.0.1")
    destinationDirectory.set(file("$rootDir/build/libs")) // ✅ Ensures Boot JAR is also placed in `build/libs/`
    enabled = true
}

tasks.register("copyJar", Copy::class) {
    from("$buildDir/libs/javachainring-0.0.1.jar")
    into("build/libs/")
}
