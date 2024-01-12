package com.keyvault.secrets.quickstart;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.baloise.azure.AzureProperties;

public class Vault {
	static String keyVaultName = "balgrpkvprodfunorg"; //System.getenv("KEY_VAULT_NAME");
	static String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";
	SecretClient secretClient = new SecretClientBuilder()
			.vaultUrl(keyVaultUri)
			.credential(new DefaultAzureCredentialBuilder()
					.managedIdentityClientId(AzureProperties.balgrpidprodfunorgClientId())
					.build())
			.httpLogOptions(opts())
			.buildClient();
    
	Map<String, String> cache = new HashMap<>();
	
	public String getSecret(String name, boolean cache) {
		String value = this.cache.get(name);
		if(cache && value != null) {
			return value;
		}
		final String propKey = "vault."+name; 
		value = System.getProperty(propKey,System.getenv(propKey));
		if(value == null) {
			value = secretClient.getSecret(name).getValue();
		}
		this.cache.put(name, value);
		return value;
	}
    
	private HttpLogOptions opts() {
		HttpLogOptions opts = new HttpLogOptions();
		opts.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);
		opts.setPrettyPrintBody(true);
		return opts;
	}

	public String list() {
    	return secretClient.listPropertiesOfSecrets().stream().map(SecretProperties::getName).collect(Collectors.joining(", "));
    	
    }
    
    public static void main(String[] args) {
    	System.out.println(new Vault().list());
	}
}