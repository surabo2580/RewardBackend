plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.reward.platform"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<ProcessResources>().configureEach {
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
    }
}
