REM naming conventions see https://confluence.baloisenet.com/display/GITSP/Naming+Convention
REM you may need to run az login before this script
set businessServiceShortCode=deop
set envCode=prod
set id=funorg
set location=westeurope
set locationShortCode=euw
set companyShortCode=balgrp
set sbuShortCode=git
set tenantShortCode=ps
set functionApp=functional-org-baloise
set gitrepo=https://github.com/baloise/functional-org-baloise.git
set functionsVersion=4
set runtime=Java
REM with Windows the github action deploy does not work
set osType=Linux

set resourceGroup=%businessServiceShortCode%-rg-%envCode%-%locationShortCode%-%sbuShortCode%-%tenantShortCode%-%id%
@echo Creating %resourceGroup% in %location% ...
az group create --name %resourceGroup% --location %location%

set storage=%companyShortCode%st%envCode%%id%
set skuStorage=Standard_ZRS
@echo Creating %storage%
az storage account create --name %storage% --location %location% --resource-group %resourceGroup% --sku %skuStorage%

@echo Creating %functionApp%
az functionapp create --name %functionApp% --storage-account %storage% --consumption-plan-location %location% --resource-group %resourceGroup% --deployment-source-url %gitrepo% --deployment-source-branch main --functions-version %functionsVersion% --runtime %runtime% --https-only --os-type %osType%

set keyvault=%companyShortCode%kv%envCode%%id%
@echo Creating %keyvault%
az keyvault create --name %keyvault% --resource-group %resourceGroup%


REM ---------------------------
set assignedId=%companyShortCode%id%envCode%%id%
az identity create --resource-group %resourceGroup% --name %assignedId%

REM here we need to read the <id> and <principalId> attribute of the response to feed it into 
REM ID: 
REM PRINCIPLE-ID: 

az functionapp identity assign --resource-group %resourceGroup% --name %functionApp% --identities <feed-id-here>

az keyvault set-policy --name %keyvault% --resource-group %resourceGroup% --object-id <feed-principalId-here> --secret-permissions get list


REM ----- Maintenance calls


REM az keyvault set-policy --name %keyvault% --resource-group %resourceGroup% --upn <YOU@baloise.com> --secret-permissions delete get list set purge

REM az keyvault secret set --vault-name %keyvault% --name 408f3c69-c6ce-42dd-8a8e-144f5e1b994e-secret --value <THE-SECRET-VALUE>
REM az keyvault secret list --vault-name %keyvault%


REM az group delete --name %resourceGroup%
REM az functionapp delete --name %functionApp% --resource-group %resourceGroup%
