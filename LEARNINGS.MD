A function does not cost a thing ;-)
The round trip is long (10 min), running locally does not help with integration / infrastructure problems
Documentation is sparse and forums (stack overflow ) out dated
Use Typed APIs for GraphQL
Monitoring / logging is far from real time and difficult to find in portal
	--> https://portal.azure.com/#view/WebsitesExtension/FunctionMenuBlade/~/monitor/resourceId/%2Fsubscriptions%2F07a656a5-d78c-4256-b752-8649af6303eb%2FresourceGroups%2Fdeop-rg-prod-euw-git-ps-funorg%2Fproviders%2FMicrosoft.Web%2Fsites%2Ffunctional-org-baloise%2Ffunctions%2FV1
Strange exception handling / retry logic
Debugging with mvn azure-functions:run -> slow start up & could get code hot swap to work
There are no standards

Open points
Use terraform?
Use spring web? 
	--> https://docs.spring.io/spring-cloud-function/docs/current/reference/html/azure.html
	--> https://github.com/spring-cloud/spring-cloud-function/tree/main/spring-cloud-function-samples/function-sample-azure-web
Remove com/baloise/azure/AzureProperties.java