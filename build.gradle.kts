import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.1"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.0"
	kotlin("plugin.spring") version "1.6.0"
}

group = "me.bgerstle"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	// Spring
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-rsocket")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.kafka:spring-kafka")

	// Money
    implementation("org.javamoney:moneta:1.4.2")
	implementation("nl.hiddewieringa:money-kotlin:1.0.1")
	implementation("org.zalando:jackson-datatype-money:1.3.0")

	// Kotlin
	val coroutines_version = "1.5.1"
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

	// Kafka
	val kafkaVersion = "3.0.0"
	implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
	implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
	implementation("io.projectreactor.kafka:reactor-kafka:1.3.8")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	val kotestVersion = "5.0.1"
	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
	testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
	testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
	testImplementation("io.kotest:kotest-property:$kotestVersion")

	testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
	testImplementation("org.testcontainers:testcontainers:1.16.2")

}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
