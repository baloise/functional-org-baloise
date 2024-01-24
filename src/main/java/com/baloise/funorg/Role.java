package com.baloise.funorg;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum Role {
	SCRUM_MASTER, PRODUCT_OWNER;
	private final static String PREFIX = "F-AAD-ROLE-";
	final static String SEPERATOR = Team.SEPERATOR;

	public final String entitlement;
	private Role() {
		entitlement = PREFIX+camelCase();
	}
	private String camelCase(String s) {
		return s.charAt(0)+s.substring(1).toLowerCase();
	}
	private String camelCase() {
		return stream(toString().split("_")).map(this::camelCase).collect(joining(SEPERATOR));
	}
	
}
