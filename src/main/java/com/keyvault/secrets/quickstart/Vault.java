package com.keyvault.secrets.quickstart;

import java.util.stream.Collectors;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.SecretProperties;

public class Vault {
	static String keyVaultName = "balgrpkvprodfunorg"; //System.getenv("KEY_VAULT_NAME");
	static String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";
	static SecretClient secretClient = new SecretClientBuilder()
			.vaultUrl(keyVaultUri)
			.credential(new DefaultAzureCredentialBuilder().build())
			.buildClient();
    
	public static String getSecret(String name) {
        return secretClient.getSecret(name).getValue();
    
    }
    
    public static String list() {
    	return secretClient.listPropertiesOfSecrets().stream().map(SecretProperties::getName).collect(Collectors.joining(", "));
    	
    }
}