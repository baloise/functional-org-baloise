package com.baloise.azure;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Map.of;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.azure.identity.ClientSecretCredential;
import com.microsoft.graph.models.Team;
import com.microsoft.graph.models.TeamCollectionResponse;
import com.microsoft.graph.models.TeamworkTag;
import com.microsoft.graph.models.TeamworkTagMember;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import common.StringTree;

public class Graph {
	private final String SCRUM_ROLES = "~SCRUM";
	private final Map<String, Set<String>> rolesSchemes = new HashMap<>(of(SCRUM_ROLES, new TreeSet<String>(asList("Member", "ScrumMaster", "ProductOwner"))));
	final String teamMarker = "👨‍👨‍👦‍👦";
	final String orgMarker = "🏢";
	final String orgSeparator = "-";
	final String teamFilter = "startsWith(displayName,'"+teamMarker+"')";
	final Pattern orgPattern = Pattern.compile(orgMarker+"\\s*\\(\\s*([\\w"+orgSeparator+"]+)\\s*\\)");
	StringTree org = new StringTree("Baloise");
	GraphServiceClient graphClient;
		
	Graph() {
		// for testing only
	} 
	
	public Graph(ClientSecretCredential credential, String[] scopes) {
		graphClient = new GraphServiceClient(credential, scopes);
	}


	public byte[] avatar(String id) throws IOException {
		try(InputStream is = graphClient.users().byUserId(id).photo().content().get()){
			return is.readAllBytes();
		}
	}
	
	public StringTree getOrg() {

		TeamCollectionResponse response = graphClient.teams().get(requestConfiguration -> {
			requestConfiguration.queryParameters.filter = teamFilter;
		});
		
		for (Team team : response.getValue()) {
			org.merge(
					parseOrg(team.getDescription())
						.addChild(new StringTree(parseName(team.getDisplayName())).withProperty("id", team.getId()))
						.getRoot()
					);
		}
		return org;
	}
	
	private String notNull(String mayBeNull) {return mayBeNull == null? "" :mayBeNull;}

	public Map<String, Object> loadTeam(String teamId, String ... roleNames) {
		return expandRoles(roleNames).stream().collect(toMap(identity(), (roleName)-> {
			final String tagId = getTagId(teamId, roleName);
			return tagId == null ? 
					Collections.emptyList() :
					map(graphClient.teams().byTeamId(teamId).tags().byTeamworkTagId(tagId).members().get()
						.getValue());
		}));
	}
	
	private List<Map<String, String>> map(List<TeamworkTagMember> members) {
		return graphClient.users().get((requestConfiguration)->{
			requestConfiguration.queryParameters.filter = format(
					"id in (%s)", 
					members.stream().map(TeamworkTagMember::getUserId).collect(joining("', '", "'", "'"))
					);
			requestConfiguration.queryParameters.select = new String []{"displayName", "mail", "officeLocation","preferredLanguage"};
		}).getValue().stream().map(this::mapMember).collect(toList());
	}

	private Map<String, String> mapMember(User u) {
		return of(
				"displayName", 			notNull(u.getDisplayName()),
				"mail", 				notNull(u.getMail()),
				"officeLocation", 		notNull(u.getOfficeLocation()),
				"preferredLanguage", 	notNull(u.getPreferredLanguage())
				);
		
	}

	private String getTagId(String teamId, String tagName) {
		return getTags(teamId).get(tagName);
	}
	
	Map<String, Map<String, String>> tagCache = new HashMap<>();
	private Map<String, String> getTags(String teamId) {
		return tagCache.computeIfAbsent(teamId, 
				(id)-> graphClient.teams().byTeamId(id).tags().get().getValue().stream().collect(toMap(TeamworkTag::getDisplayName, TeamworkTag::getId))
				);
	}
	

	Set<String> expandRoles(String ... rolesNames) {
		return rolesNames == null || rolesNames.length == 0 ? 
				expandRoles(SCRUM_ROLES) :
				stream(rolesNames)
					.flatMap((name)-> rolesSchemes.computeIfAbsent(name, Collections::singleton).stream())
					.collect(toSet());
	}

	String parseName(String input) {
		return input.replaceAll(teamMarker, "").trim();
	}


	StringTree parseOrg(String description) {
		Matcher matcher = orgPattern.matcher(description);
		matcher.find();
		return new StringTree(org.getName()).addChild(matcher.group(1).split(orgSeparator));
	}
	
}
