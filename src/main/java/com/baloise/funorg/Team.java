package com.baloise.funorg;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.regex.Pattern;

public record Team(String unit, String name,boolean internal) {
	public final static String PREFIX = "F-AAD-TEAM-";
	private final static String SEPERATOR = "-";
	private final static String INTERNAL = "INTERNAL";
	
	public static Team parse(String entitlement) {
		String[] tokens = entitlement.split(Pattern.quote(SEPERATOR));
		if(tokens.length <6) throw new IllegalArgumentException(format("%s dos not contains the minimum of 5 seperators: %s",entitlement, SEPERATOR));
		return new Team(
				stream(Arrays.copyOfRange(tokens, 3, tokens.length-2)).collect(joining(SEPERATOR)), 
				tokens[tokens.length-2], 
				INTERNAL.equals(tokens[tokens.length-1])
				);
	}
}
