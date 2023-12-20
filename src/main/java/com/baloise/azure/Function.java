package com.baloise.azure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
	/**
	 * This function listens at endpoint "/api/Hello". Two ways to invoke it using
	 * "curl" command in bash: 1. curl -d "HTTP Body" {your host}/api/Hello 2. curl
	 * "{your host}/api/Hello?name=HTTP%20Query"
	 */
	
	ClientCredentialGrant clientCredentialGrant = new ClientCredentialGrant();
	
	@FunctionName("Hello")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = { HttpMethod.GET,
			HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {
		context.getLogger().info("Java HTTP trigger processed a request.");

		// Parse query parameter
		final String query = request.getQueryParameters().get("name");
		final String name = request.getBody().orElse(query);

		if (name == null) {
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body("Please pass a name on the query string or in the request body").build();
		} else {
			return request.createResponseBuilder(HttpStatus.OK).body(String.format("Hello, %s. \n Vault contains \n%s", name, callMicrosoftGraphMeEndpoint())).build();
		}
	}

	private String callMicrosoftGraphMeEndpoint(){

		try {

			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
					.uri(URI.create("https://graph.microsoft.com/v1.0/me")).timeout(Duration.ofSeconds(7))
					.header("Content-Type", "application/json").header("Accept", "application/json")
					.header("Authorization", "Bearer " + clientCredentialGrant.getToken())
					.build();

			final HttpClient client = HttpClient.newHttpClient();
			return client.send(request, BodyHandlers.ofString()).body();
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}
}
