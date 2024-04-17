package com.baloise.funorg;

public enum Role {
	SCRUM_MASTER, PRODUCT_OWNER;

	public final String entitlement;
	private Role() {
		entitlement = camelCase(name());
	}
	private String camelCase(String s) {
		return s.charAt(0)+s.substring(1).toLowerCase();
	}
	
}
