plugins {
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
dependencies {
    implementation(files("libs/fit.jar")) // ✅ Ensures fit.jar is directly included
}

    // Example Guava dependency
    implementation("com.google.guava:guava:32.0.1-jre")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("org.example.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
