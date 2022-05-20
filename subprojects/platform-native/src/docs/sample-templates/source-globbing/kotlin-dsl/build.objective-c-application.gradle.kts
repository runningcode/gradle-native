plugins {
	id("dev.nokee.objective-c-application")								// <1>
}

application {
	objectiveCSources.setFrom(fileTree("srcs") { include("**/*.m") })	// <2>
	privateHeaders.setFrom(fileTree("hdrs") { include("**/*.h") })		// <3>
}
