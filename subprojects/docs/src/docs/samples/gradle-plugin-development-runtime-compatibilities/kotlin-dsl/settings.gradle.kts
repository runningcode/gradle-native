pluginManagement {
	repositories {
		gradlePluginPortal()
		maven { url = uri("https://repo.nokee.dev/release") }
	}
}

plugins {
	id("dev.gradleplugins.gradle-plugin-development") version("1.2.2")
}

rootProject.name = "gradle-plugin-development-runtime-compatibilities"
