package com.baloise.azure;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.baloise.funorg.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;

/**
 * Azure Functions with HTTP Trigger.
 */
public class FunctionalOrgEndpoint {
	/**
	 * This function listens at endpoint "/api/Hello". Two ways to invoke it using
	 * "curl" command in bash: 1. curl -d "HTTP Body" {your host}/api/Hello 2. curl
	 * "{your host}/api/Hello?name=HTTP%20Query"
	 */
	
	Vault lazyVault = null;
	Graph lazygraph = null;
	ObjectMapper objectMapper = new ObjectMapper();
	
	Vault vault() {
		if(lazyVault == null) {
			lazyVault = new Vault();
		}
		return lazyVault;
	}
	
	Graph graph() {
		if(lazygraph == null) {
			lazygraph = new Graph(new TokenCredentialAuthProvider( new ClientSecretCredentialBuilder()
					.authorityHost(AzureProperties.authority())
					.tenantId(AzureProperties.tenantId()).clientId(AzureProperties.clientId())
					.clientSecret(vault().getSecret(AzureProperties.clientSecretName(), true)).build()));
		}
		return lazygraph;
	}
	
	@FunctionName("V1")
	public HttpResponseMessage v1(
			@HttpTrigger(
					name = "req", 
					methods = { HttpMethod.GET, HttpMethod.POST }, 
					authLevel = AuthorizationLevel.ANONYMOUS,
					route = "V1/{unit=null}/{team=null}"
			) 
			HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context,
			@BindingName("unit") String unit,
			@BindingName("team") String team
		) {
		try {
			
			
			if(!"null".equals(team) && "avatar".equals(unit)) {				
				return createAvatarResponse(request, team);
			}
			if(!"null".equals(team)) {				
				return createTeamResponse(request, unit, team);
			}
			
			if(!"null".equals(unit)) {
				return createUnitResponse(request, unit);
			}
			
			return createRootResponse(request);
			
		} catch (IOException e) {
			context.getLogger().warning(e.getLocalizedMessage());
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getLocalizedMessage()).build();
		} catch (Throwable t) {
			context.getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
			throw t;
		}
	}

	private HttpResponseMessage createAvatarResponse(HttpRequestMessage<Optional<String>> request, String id) throws IOException {
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","image/jpeg")
				.body(graph().avatar(id)).build();
	}

	private HttpResponseMessage createRootResponse(HttpRequestMessage<Optional<String>> request)
			throws JsonProcessingException {
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body( 					
						objectMapper.writeValueAsString(
								Map.of("units",
										graph().getTeams().stream()
										.map(g -> Team.parse(g.displayName))
										.map(t -> 
										Map.of(
												"name", t.unit(),
												"url", request.getUri()+"/"+ t.unit()
												)
												)
										.distinct()
										.collect(Collectors.toList())
										))
						).build();
	}

	private HttpResponseMessage createUnitResponse(HttpRequestMessage<Optional<String>> request, String unit) throws JsonProcessingException {
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body( 					
						objectMapper.writeValueAsString(
								Map.of("teams",
										graph().getTeams(unit+"-").stream()
										.map(g -> Team.parse(g.displayName))									
										.map(t -> 
										Map.of(
												"name", t.name(),
												"unit", t.unit(),
												"url", format("%s/%s/%s", getPath(request), t.unit(),t.name())
												)
												)
											.distinct()
											.collect(Collectors.toList())
										))
						).build();
	}

	private HttpResponseMessage createTeamResponse(HttpRequestMessage<Optional<String>> request, String unit, String team) throws JsonProcessingException {
		Map<String, Map<String,Object>> name2team = new HashMap<>();
		
		for (Group group : graph().getTeams(unit+"-"+team)) {
			Team t = Team.parse(group.displayName);
			Map<String, Object> tmp = name2team.get(t.name());
			if(tmp== null) {
				tmp = Map.of(
						"name", t.name(),
						"unit", t.unit(),
						"url", getPath(request)+"/"+t.name(),
						"members" , loadAndMapMembers(group, t.internal())
						);
				name2team.put(t.name(), tmp);
			} else {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> members = (List<Map<String, Object>>) tmp.get("members");
				members.addAll(loadAndMapMembers(group, t.internal()));
			}
		}
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body( 					
						objectMapper.writeValueAsString(
								Map.of("teams", name2team.values()))
						).build();
	}

	private List<Map<String, Object>> loadAndMapMembers(Group group, boolean internal) {
		return graph().getGroupMembers(group.id).stream().map(u-> mapUser(u,internal)).collect(Collectors.toList());
	}
	
	private String getPath(HttpRequestMessage<?> request) {
		String uri = request.getUri().toString();
		return uri.substring(0, uri.lastIndexOf('/'));
	}

	@FunctionName("dump")
	public HttpResponseMessage dump(
			final ExecutionContext context,
			@HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) 
			HttpRequestMessage<Optional<String>> request
			) {
		return request.createResponseBuilder(HttpStatus.OK).body(
				listEnv() + listProps()
				).build();
	}
	
	@FunctionName("hello")
	public HttpResponseMessage hello(
			final ExecutionContext context,
			@HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) 
			HttpRequestMessage<Optional<String>> request
			) {
		try {
				return request.createResponseBuilder(HttpStatus.OK).body( 
						"Hi there"
						).build();
		} catch (Throwable t) {
			context.getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
			throw t;
		}
	}

	private String listProps() {
		return "\nProps\n\n"+ System.getProperties().entrySet().stream().map(e -> format("%s = %s", e.getKey(), e.getValue())).collect(joining("\n"));
	}

	private String listEnv() {
		return "\nEnv\n\n"+System.getenv().entrySet().stream().map(e -> format("%s = %s", e.getKey(), e.getValue())).collect(joining("\n"));
	}

	private Map<String, Object> mapUser(DirectoryObject dirObj, boolean internal) {
		User u = (User) dirObj;
		return Map.of(
				"preferredLanguage", 	notNull(u.preferredLanguage),
				"officeLocation", 		notNull(u.officeLocation),
				"givenName", 			notNull(u.givenName),
				"surname", 				notNull(u.surname),
				"mail", 				notNull(u.mail),
				"internal", 			internal
				);
	}
	
	private String notNull(String mayBeNull) {return mayBeNull == null? "" :mayBeNull;}
	
}
