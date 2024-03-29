package com.baloise.azure;

import static java.lang.String.format;

//TODO: how can we get rid of these hard coded properties?
public class AzureProperties {
	public final static String defaultScope() {return "https://graph.microsoft.com/.default";}
	public final static String clientId() {return "408f3c69-c6ce-42dd-8a8e-144f5e1b994e";}
	public final static String clientSecretName() {return clientId()+"-secret";}
	//TODO how to read this at runtime?
	public final static String tenantId() {return "eb3c68b9-0935-4046-8550-8bcaa4167e2e";}
	public final static String authority() {return format("https://login.microsoftonline.com/%s/", tenantId());}
	public final static String balgrpidprodfunorgClientId() {return "6584d69e-086d-4b3a-968a-54999110e3bf";}
}
