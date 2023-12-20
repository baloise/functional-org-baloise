package com.baloise.azure;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import com.keyvault.secrets.quickstart.Vault;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

class ClientCredentialGrant {

	IAuthenticationResult cache;
	
    public String getToken() throws Exception {
    	
    	if(cache == null || cache.expiresOnDate().after(new Date())) {
    		// With client credentials flows the scope is ALWAYS of the shape "resource/.default", as the
    		// application permissions need to be set statically (in the portal), and then granted by a tenant administrator
    		ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
    				Collections.singleton(AzureProperties.defaultScope()))
    				.build();
    		
    		ConfidentialClientApplication app = ConfidentialClientApplication.builder(
    				AzureProperties.clientId(),
    				ClientCredentialFactory.createFromSecret(Vault.getSecret(AzureProperties.clientSecretName())))
    				.authority(AzureProperties.authority())
    				.build();	
    		CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
    		cache = future.get();    		
    	}
    	
    	return cache.accessToken();
    }

}
