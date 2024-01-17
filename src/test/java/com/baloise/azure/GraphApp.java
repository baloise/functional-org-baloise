package com.baloise.azure;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.keyvault.secrets.quickstart.Graph;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;

public class GraphApp {
	static ClientSecretCredential tokenCredential = new ClientSecretCredentialBuilder()
			.authorityHost(AzureProperties.authority())
			.tenantId(AzureProperties.tenantId()).clientId(AzureProperties.clientId())
			.clientSecret("Wz38Q~mm7aiT6Iqnqm9W7Q0IUSHQFIfptKtt6cmY").build();

	public static void main(String[] args) {
		IAuthenticationProvider auth = new TokenCredentialAuthProvider(
				tokenCredential);
		new Graph(auth).getTeams().stream().forEach(g -> System.out.println(g.displayName));
	}

}
