The round trip is long (10 min), running locally does not help with integration / infrastructure problems
Documentation is sparse and forums (stack overflow ) out dated
Use Typed APIs for GraphQL
Monitoring / logging is far from real time
Strange exception handling / retry logic
No support for REST in Function -> it's low level HTTP
Debugging with mvn azure-functions:run -> slow start up & could get code hot swap to work

Open points
Use terraform?
Get all credentials out of the code -> into environment
Use spring web? 
	--> https://docs.spring.io/spring-cloud-function/docs/current/reference/html/azure.html
	--> https://github.com/spring-cloud/spring-cloud-function/tree/main/spring-cloud-function-samples/function-sample-azure-web
