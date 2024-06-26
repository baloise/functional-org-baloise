package com.baloise.azure;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Map.of;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
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
	final Map<String, Set<String>> rolesSchemes = new HashMap<>(of(SCRUM_ROLES, new TreeSet<String>(asList("Member", "ScrumMaster", "ProductOwner"))));
	final String teamMarker = "👨‍👨‍👦‍👦";
	final String orgMarker = "🏢";
	final String orgSeparator = "-";
	final String teamFilter = "startsWith(displayName,'"+teamMarker+"')";
	final Pattern orgPattern = Pattern.compile(orgMarker+"\\s*\\(\\s*([\\w"+orgSeparator+"]+)\\s*\\)");
	StringTree org = new StringTree("root");
	GraphServiceClient graphClient;
	private boolean obfuscated = false;
		
	Graph() {
		// for testing only
	} 
	
	public Graph(ClientSecretCredential credential, String[] scopes, boolean obfuscated) {
		this.obfuscated  = obfuscated;
		graphClient = new GraphServiceClient(credential, scopes);
	}
	
	
	public void clear() {
		org = new StringTree(org.getName());
	}

	public byte[] avatar(String id) throws IOException {
		if(obfuscated) id = "unknown_person.jpg";
		try(InputStream is = graphClient.users().byUserId(id).photo().content().get()){
			return is.readAllBytes();
		} catch (Exception e) {
			try(InputStream is = getClass().getResourceAsStream("unknown_person.jpg")){
				return is.readAllBytes();
			}	
		}
	}
	
	public StringTree getOrg() {

		TeamCollectionResponse response = graphClient.teams().get(requestConfiguration -> {
			requestConfiguration.queryParameters.filter = teamFilter;
		});
		
		for (Team team : response.getValue()) {
			parseOrg(team.getDescription())
				.addChild(
						new StringTree(parseName(team.getDisplayName()))
							.withProperty("id", team.getId())
							.withProperty("description", notNull(cleanDescription(team.getDescription())))
				);
		}
		return org;
	}

	private String notNull(String mayBeNull) {		
		String ret = mayBeNull == null? "" :mayBeNull;
		return  obfuscated ? (ret + "...").substring(0, 3)+"..." : ret;
	}
	
	private List<String> notNull(List<String> strings) {
		return strings.stream().map(this::notNull).collect(toList());
	}

	public Map<String, Object> loadTeam(String teamId, String ... roleNames) {
		Map<String, Map<String, Object>> mail2member = new TreeMap<>();
		expandRoles(roleNames).stream().forEach(roleName-> {
			final String tagId = getTagId(teamId, roleName);
			if(tagId != null) { 
				map(graphClient.teams().byTeamId(teamId).tags().byTeamworkTagId(tagId).members().get().getValue()).forEach(member->{
					Map<String, Object> mappedMember = mail2member.computeIfAbsent(member.getMail(), (ignored)-> new TreeMap<>());
					mappedMember.put("displayName",notNull(member.getDisplayName()));
					mappedMember.put("givenName",notNull(member.getGivenName()));
					mappedMember.put("surname",notNull(member.getSurname()));
					mappedMember.put("mail",notNull(member.getMail()));
					mappedMember.put("officeLocation",notNull(member.getOfficeLocation()));
					mappedMember.put("preferredLanguage",notNull(member.getPreferredLanguage()));
					mappedMember.put("businessPhones",notNull(member.getBusinessPhones()));
					mappedMember.put("department",notNull(member.getDepartment()));
					if(!obfuscated) mappedMember.put("userKey",notNull(member.getMailNickname()));
					mappedMember.put("usageLocation",notNull(member.getUsageLocation()));
					((Set<String>) mappedMember.computeIfAbsent("roles",(ignored)-> new TreeSet<>())).add(roleName);
				});
			}
		});
		return new TreeMap<>(of("members", mail2member.values()));
	}
	

	private List<User> map(List<TeamworkTagMember> members) {
		return graphClient.users().get((requestConfiguration)->{
			requestConfiguration.queryParameters.filter = format(
					"id in (%s)", 
					members.stream().map(TeamworkTagMember::getUserId).collect(joining("', '", "'", "'"))
					);
			requestConfiguration.queryParameters.select = new String []{
					"displayName", 
					"mail", 
					"officeLocation",
					"preferredLanguage",
					"businessPhones",
					"department",
					"employeeId",
					//"mailNickname",
					"usageLocation",
					"givenName",
					"surname"
			};
		}).getValue();
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
				expandRoles(getDefaultRoleScheme()) :
				stream(rolesNames)
					.flatMap((name)-> rolesSchemes.computeIfAbsent(name, Collections::singleton).stream())
					.collect(toSet());
	}

	public String getDefaultRoleScheme() {
		return SCRUM_ROLES;
	}

	String parseName(String input) {
		return input.replaceAll(teamMarker, "").trim();
	}


	StringTree parseOrg(String description) {
		Matcher matcher = orgPattern.matcher(description);
		return matcher.find() ? org.addChild(matcher.group(1).split(orgSeparator)) : org;
	}
	
	String cleanDescription(String description) {
		return orgPattern.matcher(description).replaceAll("");
	}

	public Map<String, Set<String>> getRoleSchemes() {
		return rolesSchemes.entrySet().stream().filter(e->e.getValue().size()>1).collect(toMap(Entry::getKey,Entry::getValue));
	}

	public Graph withObfuscation(boolean obfuscated) {
		if(this.obfuscated != obfuscated) clear();
		this.obfuscated = obfuscated;
		return this;
	}
	
}
