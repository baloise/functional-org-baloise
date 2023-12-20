package com.keyvault.secrets.quickstart;

import java.util.stream.Collectors;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.SecretProperties;

public class Vault {
    public static String list() {
        String keyVaultName = "balgrpkvprodfunorg"; //System.getenv("KEY_VAULT_NAME");
        String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";

        System.out.printf("key vault name = %s and key vault URI = %s \n", keyVaultName, keyVaultUri);

        SecretClient secretClient = new SecretClientBuilder()
            .vaultUrl(keyVaultUri)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();


        return secretClient.listPropertiesOfSecrets().stream().map(SecretProperties::getName).collect(Collectors.joining(", "));
    
    }
}