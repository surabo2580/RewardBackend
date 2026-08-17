plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":reward-core"))
    implementation(kotlin("stdlib"))
}
