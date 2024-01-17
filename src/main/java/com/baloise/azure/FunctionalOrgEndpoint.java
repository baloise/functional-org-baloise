package com.baloise.azure;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.baloise.funorg.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyvault.secrets.quickstart.Graph;
import com.keyvault.secrets.quickstart.Vault;
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
	
	Vault vault = new Vault();
	Graph lazygraph = null;
	ObjectMapper objectMapper = new ObjectMapper();
	
	Graph graph() {
		if(lazygraph == null) {
			lazygraph = new Graph(new TokenCredentialAuthProvider( new ClientSecretCredentialBuilder()
					.authorityHost(AzureProperties.authority())
					.tenantId(AzureProperties.tenantId()).clientId(AzureProperties.clientId())
					.clientSecret(vault.getSecret(AzureProperties.clientSecretName(), true)).build()));
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
			context.getLogger().log(Level.INFO, "unit "+ unit);
			context.getLogger().log(Level.INFO, "team "+ team);
			
			if(!"null".equals(team)) {
				
				return request.createResponseBuilder(HttpStatus.OK)
						.header("Content-Type","application/json; charset=UTF-8")
						.body( 					
								objectMapper.writeValueAsString(
										Map.of("teams",
												graph().getTeams(unit+"-"+team).stream()
												
												.map(g -> {
												Team t = Team.parse(g.displayName);
												List<DirectoryObject> group = graph().getGroupMembers(g.id);
												return Map.of(
														"entitlement", g.displayName,
														"name", t.name(),
														"unit", t.unit(),
														"url", request.getUri(),
														"members" , group.stream().map(u-> mapUser(u,t.internal())).collect(Collectors.toList())
														);}
														)
														.distinct()
														.collect(Collectors.toList())
												))
								).build();
			}
			
			if(!"null".equals(unit)) {
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
														"url", format("%s/%s", request.getUri(), t.name())
														)
														)
													.distinct()
													.collect(Collectors.toList())
												))
								).build();
			}
			
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
		} catch (JsonProcessingException e) {
			context.getLogger().warning(e.getLocalizedMessage());
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getLocalizedMessage()).build();
		} catch (Throwable t) {
			context.getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
			throw t;
		}
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
	
	@FunctionName("vault")
	public HttpResponseMessage vault(
			final ExecutionContext context,
			@HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) 
			HttpRequestMessage<Optional<String>> request
			) {
		try {
			return request.createResponseBuilder(HttpStatus.OK).body( 
					vault.list()
					).build();
		} catch (Throwable t) {
			context.getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
			throw t;
		}
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

	private Map<Object, Object> mapUser(DirectoryObject dirObj, boolean internal) {
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
