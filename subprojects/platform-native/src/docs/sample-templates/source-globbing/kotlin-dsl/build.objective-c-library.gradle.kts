plugins {
	id("dev.nokee.objective-c-library")									// <1>
}

application {
	objectiveCSources.setFrom(fileTree("srcs") { include("**/*.m") })	// <2>
	privateHeaders.setFrom(fileTree("hdrs") { include("**/*.h") })		// <3>
	publicHeaders.setFrom(fileTree("incs") { include("**/*.h") })		// <3>
}
