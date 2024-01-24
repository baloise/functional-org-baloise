package com.baloise.azure;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.baloise.funorg.Role;
import com.baloise.funorg.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
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
				return createTeamResponse(request, context, unit, team);
			}
			
			if(!"null".equals(unit)) {
				return createUnitResponse(request, context, unit);
			}
			
			return createRootResponse(request, context);
			
		} catch (IOException e) {
			context.getLogger().warning(e.getLocalizedMessage());
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getLocalizedMessage()).build();
		} catch (Throwable t) {
			context.getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
			throw t;
		}
	}

	private HttpResponseMessage createAvatarResponse(HttpRequestMessage<Optional<String>> request, String id) throws IOException {
		byte[] avatar = graph().avatar(id);
		String myETag = String.valueOf(Arrays.hashCode(avatar));		
		String theirETag = ignoreKeyCase(request.getHeaders()).get("If-None-Match");
		boolean sameEtag = Objects.equals(myETag, theirETag);
		Builder response = request
				.createResponseBuilder(sameEtag? HttpStatus.NOT_MODIFIED : HttpStatus.OK)
				.header("Content-Type","image/jpeg")
				.header("ETag",myETag);
		if(!sameEtag) {
			response = response
					.header("Content-Length",String.valueOf(avatar.length))
					.body(avatar);
		}
		return response.build();
	}

	private Map<String, String> ignoreKeyCase(Map<String, String> mixedMap) {
		Map<String, String> lowMap = new HashMap<>() {
			@Override
			public String get(Object key) {
				return super.get(key.toString().toLowerCase());
			}
		};
		mixedMap.entrySet().stream().forEach(e->lowMap.put(e.getKey().toLowerCase(), e.getValue()));
		return lowMap;
	}

	private HttpResponseMessage createRootResponse(HttpRequestMessage<Optional<String>> request, ExecutionContext context)
			throws JsonProcessingException {
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body( 					
						objectMapper.writeValueAsString(
								Map.of("units",
										graph().getTeams().stream()
										.map(g -> Team.parse(context.getLogger(), g.displayName))
										.filter(Objects::nonNull)
										.map(t -> 
										Map.of(
												"name", t.unit(),
												"url", getCleanUri(request)+"/"+ t.unit()
												)
												)
										.distinct()
										.collect(Collectors.toList())
										))
						).build();
	}

	private HttpResponseMessage createUnitResponse(HttpRequestMessage<Optional<String>> request, ExecutionContext context, String unit) throws JsonProcessingException {
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body( 					
						objectMapper.writeValueAsString(
								Map.of("teams",
										graph().getTeams(unit+"-").stream()
										.map(g -> Team.parse(context.getLogger(), g.displayName))
										.filter(Objects::nonNull)
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

	private HttpResponseMessage createTeamResponse(HttpRequestMessage<Optional<String>> request, ExecutionContext context, String unit, String team) throws JsonProcessingException {
		Map<String, Map<String,Object>> name2team = new HashMap<>();
		
		for (Group group : graph().getTeams(unit+"-"+team)) {
			try {
				Team t = Team.parse(group.displayName);
				Map<String, Object> tmp = name2team.get(t.name());
				if(tmp== null) {
					tmp = Map.of(
							"name", t.name(),
							"unit", t.unit(),
							"url", getPath(request)+"/"+t.name(),
							"members" , loadAndMapMembers(group, t.internal(), request.getQueryParameters().get("expand"))
							);
					name2team.put(t.name(), tmp);
				} else {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> members = (List<Map<String, Object>>) tmp.get("members");
					members.addAll(loadAndMapMembers(group, t.internal(), request.getQueryParameters().get("expand")));
				}
			} catch (ParseException e) {
				context.getLogger().log(Level.WARNING, e.getLocalizedMessage(), e);
			}
		}
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body( 					
						objectMapper.writeValueAsString(
								Map.of("teams", name2team.values()))
						).build();
	}

	private List<Map<String, Object>> loadAndMapMembers(Group group, boolean internal, String expand) {
		return graph().getGroupMembers(group).stream()
				.map(User.class::cast)
				.map(u-> mapUser(u,internal, graph().getRoles(u),expand)).collect(Collectors.toList());
	}
	
	private String getPath(HttpRequestMessage<?> request) {
		String uri = getCleanUri(request);
		return uri.substring(0, uri.lastIndexOf('/'));
	}

	private String getCleanUri(HttpRequestMessage<?> request) {
		return request.getUri().toString().replaceFirst("/\\z", "");
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

	private Map<String, Object> mapUser(User u, boolean internal, Set<Role> roles, String expand) {
		Map<String, Object> core = Map.of(
				"preferredLanguage", 	notNull(u.preferredLanguage),
				"officeLocation", 		notNull(u.officeLocation),
				"displayName", 			notNull(u.displayName),
				"givenName", 			notNull(u.givenName),
				"surname", 				notNull(u.surname),
				"mail", 				notNull(u.mail),
				"roles", 				roles,
				"internal", 			internal
				);
		return expand == null ?  
				core : 
					stream(expand.split(","))
					.map(String::trim)
					.filter(Objects::nonNull)
					.filter(not(String::isEmpty))
					.reduce(
				         new HashMap<>(core), (map, element) -> {
				    	  		try {
									map.put(element, u.getClass().getField(element).get(u));
								} catch (Exception e) {
									e.printStackTrace();
								}
				    	  		return map;
				          },
				          (map1, map2) -> {
				              map1.putAll(map2);
				              return map1;
				          }
					);
	}
	
	private String notNull(String mayBeNull) {return mayBeNull == null? "" :mayBeNull;}
	
}
