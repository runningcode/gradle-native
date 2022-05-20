plugins {
	id("dev.nokee.c-application")									// <1>
}

application {
	cSources.setFrom(fileTree("srcs") { include("**/*.c") })		// <2>
	privateHeaders.setFrom(fileTree("hdrs") { include("**/*.h") })	// <3>
}
