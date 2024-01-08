package com.keyvault.secrets.quickstart;

import java.time.Duration;
import java.util.stream.Collectors;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.baloise.azure.AzureProperties;

import reactor.core.publisher.Mono;

public class Vault {
	static String keyVaultName = "balgrpkvprodfunorg"; //System.getenv("KEY_VAULT_NAME");
	static String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";
	static SecretClient secretClient = new SecretClientBuilder()
			.vaultUrl(keyVaultUri)
			.credential(new DefaultAzureCredentialBuilder()
					.managedIdentityClientId(AzureProperties.balgrpidprodfunorgClientId())
					.build())
			.httpLogOptions(opts())
			//.httpClient(createClient())
			.buildClient();
    
	public static String getSecret(String name) {
        return secretClient.getSecret(name).getValue();
    
    }
    
    private static HttpClient createClient() {
    	final HttpClient delegate = new NettyAsyncHttpClientBuilder()
    			.responseTimeout(Duration.ofSeconds(10))
    			.build();
    	return new HttpClient() {
			@Override
			public Mono<HttpResponse> send(HttpRequest request) {
				System.out.println(request);
				return delegate.send(request);
			}
		};
	}

	private static HttpLogOptions opts() {
		HttpLogOptions opts = new HttpLogOptions();
		opts.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);
		opts.setPrettyPrintBody(true);
		return opts;
	}

	public static String list() {
    	return secretClient.listPropertiesOfSecrets().stream().map(SecretProperties::getName).collect(Collectors.joining(", "));
    	
    }
    
    public static void main(String[] args) {
    	System.out.println(list());
	}
}