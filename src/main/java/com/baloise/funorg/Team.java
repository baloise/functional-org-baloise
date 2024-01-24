package com.baloise.funorg;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public record Team(String unit, String name,boolean internal) {
	public final static String PREFIX = "F-AAD-TEAM-";
	final static String SEPERATOR = "-";
	private final static String INTERNAL = "INTERNAL";
	
	/**
	 * @param logger
	 * @param entitlement
	 * @return null if a ParseException occurs, the exception will be logged as WARNING
	 */
	public static Team parse(Logger logger, String entitlement) {
		try {
			return parse(entitlement);
		} catch (ParseException e) {
			logger.log(Level.WARNING, e.getLocalizedMessage(), e);
			return null;
		}
	}
	
	public static Team parse(String entitlement) throws ParseException {
		String[] tokens = entitlement.split(Pattern.quote(SEPERATOR));
		if(tokens.length <6) throw new ParseException(format("%s dos not contains the minimum of 5 seperators: %s",entitlement, SEPERATOR),0);
		return new Team(
				stream(Arrays.copyOfRange(tokens, 3, tokens.length-2)).collect(joining(SEPERATOR)), 
				tokens[tokens.length-2], 
				INTERNAL.equals(tokens[tokens.length-1])
				);
	}
}
