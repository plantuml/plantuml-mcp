plugins {
	java
	eclipse
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "net.sourceforge.plantuml"
version = "1.0.0"
description = "Model Context Protocol (MCP) server for PlantUML"

// Java version defaults to 21 but can be overridden without editing this file,
// e.g. `./gradlew bootJar -PjavaVersion=17`.
val javaVersion = (findProperty("javaVersion") as String?)?.toInt() ?: 21

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion))
	}
}

repositories {
	mavenCentral()
}

val springAiVersion = "1.1.2"

dependencies {
	implementation("org.springframework.ai:spring-ai-starter-mcp-server")
	// The stdio MCP server factory references a Servlet environment class
	// (StandardServletEnvironment) from spring-web. We add spring-web so the
	// class is on the classpath. No web server is started: web-application-type
	// stays "none", so this only satisfies the class reference.
	implementation("org.springframework:spring-web")
	implementation("net.sourceforge.plantuml:plantuml:1.2026.5")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
	}
}

tasks.named<Test>("test") {
	useJUnitPlatform()
}
