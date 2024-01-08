package com.baloise.azure;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.util.Optional;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.keyvault.secrets.quickstart.Graph;
import com.keyvault.secrets.quickstart.Vault;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
	/**
	 * This function listens at endpoint "/api/Hello". Two ways to invoke it using
	 * "curl" command in bash: 1. curl -d "HTTP Body" {your host}/api/Hello 2. curl
	 * "{your host}/api/Hello?name=HTTP%20Query"
	 */
	
	Vault vault = new Vault();
	Graph lazygraph = null;
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
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = { HttpMethod.GET,
			HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {
		context.getLogger().info("Java HTTP trigger processed a request.");

		if(request.getQueryParameters().containsKey("dump")) {
			return request.createResponseBuilder(HttpStatus.OK).body(
					listEnv() + listProps()
					).build();
		} 
		if(request.getQueryParameters().containsKey("name")) {
			return request.createResponseBuilder(HttpStatus.OK).body( 
					"Hello "+request.getQueryParameters().get("name")
					).build();
		}
		if(request.getQueryParameters().containsKey("vault")) {
			return request.createResponseBuilder(HttpStatus.OK).body( 
					vault.list()
					).build();
		}
		if(request.getQueryParameters().containsKey("graph")) {
			
			
			return request.createResponseBuilder(HttpStatus.OK).body( 
//					new Graph(new TokenCredentialAuthProvider(new ManagedIdentityCredentialBuilder()
//							.clientId(AzureProperties.clientId())
//							.build()))
//					.getTeams().stream().map(g-> g.displayName).collect(joining("<br>"))
					
					graph().getTeams().stream().map(g-> g.displayName).collect(joining("<br>"))
					).build();
		}
		return request.createResponseBuilder(HttpStatus.OK).body( 
					"Hi there. What's your ?name="
					).build();
	}

	private String listProps() {
		return "\nProps\n\n"+ System.getProperties().entrySet().stream().map(e -> format("%s = %s", e.getKey(), e.getValue())).collect(joining("\n"));
	}

	private String listEnv() {
		return "\nEnv\n\n"+System.getenv().entrySet().stream().map(e -> format("%s = %s", e.getKey(), e.getValue())).collect(joining("\n"));
	}
	
}
