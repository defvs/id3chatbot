import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	kotlin("jvm") version "1.3.50"
	id("com.github.johnrengelman.shadow") version "5.2.0"
	application
}

group = "com.defvs.id3chatbot"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
	jcenter()
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	testCompile("junit", "junit", "4.12")

	implementation("com.squareup.okhttp3:okhttp:4.4.0")
	implementation("com.beust:klaxon:5.0.1")

	implementation("io.github.config4k:config4k:0.4.1")

	implementation("org.slf4j:slf4j-simple:1.7.29")
	implementation("io.github.microutils:kotlin-logging:1.7.9")
}

configure<JavaPluginConvention> {
	sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "1.8"
}

val jar by tasks.getting(Jar::class) {
	manifest {
		attributes["Main-Class"] = "dev.defvs.id3chatbot.Main"
	}
}

application {
	mainClassName = "dev.defvs.id3chatbot.Main"
}