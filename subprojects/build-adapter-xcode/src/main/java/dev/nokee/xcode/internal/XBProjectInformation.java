package dev.nokee.xcode.internal;

import lombok.Value;

import java.util.List;

@Value
public class XBProjectInformation {
	List<String> configurations;
	String name;
	List<String> schemes;
	List<String> targets;
}
