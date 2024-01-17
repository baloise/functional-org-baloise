package com.baloise.funorg;

import java.util.Scanner;

public record Team(String unit, String name,boolean internal) {
	public final static String PREFIX = "F-AAD-TEAM-";
	private final static String SEPERATOR = "-";
	private final static String INTERNAL = "INTERNAL";
	
	public static Team parse(String entitlement) {
		try(Scanner scan = new Scanner(entitlement)){
			return new Team(
			scan.skip(PREFIX).useDelimiter(SEPERATOR).next(),
			scan.next(),
			INTERNAL.equals(scan.next()));
		}
	}
}
