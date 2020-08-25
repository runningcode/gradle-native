package dev.nokee.xcode.internal;

import lombok.Value;

import java.util.Map;

@Value
public class XBShowBuildSettingsResponse {
	String target;
	String action;
	Map<String, String> buildSettings;
}
