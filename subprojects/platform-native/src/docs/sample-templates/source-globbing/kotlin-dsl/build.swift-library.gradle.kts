plugins {
	id("dev.nokee.swift-library")										// <1>
}

application {
	swiftSources.setFrom(fileTree("srcs") { include("**/*.swift") })	// <2>
}
