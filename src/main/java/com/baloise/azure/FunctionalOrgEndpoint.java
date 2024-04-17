package com.baloise.azure;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import common.StringTree;

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
			final String[] scopes = new String[] { AzureProperties.defaultScope() };
			final ClientSecretCredential credential = 
					new ClientSecretCredentialBuilder()
						.clientId(AzureProperties.clientId()).
						tenantId(AzureProperties.tenantId())
						.clientSecret(
								vault().getSecret(AzureProperties.clientSecretName(), true)
			    		)
						.build();

			
			lazygraph = new Graph(credential, scopes);
		}
		return lazygraph;
	}
	
	@FunctionName("V1")
	public HttpResponseMessage v1(
			@HttpTrigger(
					name = "req", 
					methods = { HttpMethod.GET, HttpMethod.POST }, 
					authLevel = AuthorizationLevel.ANONYMOUS,
					route = "V1/{a=null}/{b=null}/{c=null}/{d=null}/{e=null}/{f=null}/{g=null}/{h=null}/{i=null}"
			) 
			HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context
		) {
		try {
			List<String> path =  asList(request.getUri().getPath().split("/"));
			path = path.subList(path.indexOf("V1")+1, path.size());
			if(path.size() ==2 && "avatar".equals(path.get(0))) {				
				return createAvatarResponse(request, path.get(1));
			}
			
			final StringTree child = graph().getOrg().getChild(path.toArray(new String[0]));
			
			return child.isLeaf() ?  createTeamResponse(request, context, child) : createOrganisationResponse(request, context, child);
			
		} catch (Throwable t) {
			context.getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(t.getLocalizedMessage()).build();
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
			private static final long serialVersionUID = 5782240485145186162L;
			@Override
			public String get(Object key) {
				return super.get(key.toString().toLowerCase());
			}
		};
		mixedMap.entrySet().stream().forEach(e->lowMap.put(e.getKey().toLowerCase(), e.getValue()));
		return lowMap;
	}

	private HttpResponseMessage createTeamResponse(HttpRequestMessage<Optional<String>> request, ExecutionContext context, StringTree team)
			throws JsonProcessingException {
	
		final Map<String, Object> body = graph().loadTeam(team.getProperty("id"), getRoles(request));
		body.put("name", team.getName());
		body.put("url", format("%s/%s", getPath(request),team.getName()));
		return createJSONResponse(request, body);
	}

	String[] getRoles(HttpRequestMessage<Optional<String>> request) {
		String roles = request.getQueryParameters().get("roles");
		return (roles == null) ? new String[0] : roles.split("\\s+,\\s+");
	}
	
	private HttpResponseMessage createOrganisationResponse(HttpRequestMessage<Optional<String>> request, ExecutionContext context, StringTree tree)
			throws JsonProcessingException {
		Map<String, List<Map<String, String>>> response = new HashMap<>();
		response.put("units", new ArrayList<Map<String,String>>());
		response.put("teams", new ArrayList<Map<String,String>>());
		for (StringTree child : tree.getChildren()) {
			if(child.isLeaf()) {
				response.get("teams").add(Map.of(
						"name", child.getName(),
						"url", format("%s/%s", getPath(request),child.getName())
						));
			} else {
				response.get("units").add(Map.of(
						"name", child.getName(),
						"url", format("%s/%s", getPath(request),child.getName())
						));				
			}
		}
		return createJSONResponse(request, response);
	}

	HttpResponseMessage createJSONResponse(HttpRequestMessage<Optional<String>> request,
			Object body) throws JsonProcessingException {
		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type","application/json; charset=UTF-8")
				.body(objectMapper.writeValueAsString(body)).build();
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

}
