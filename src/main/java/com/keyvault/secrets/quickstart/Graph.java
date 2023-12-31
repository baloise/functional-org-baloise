package com.keyvault.secrets.quickstart;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.http.BaseCollectionRequestBuilder;
import com.microsoft.graph.http.BaseEntityCollectionRequest;
import com.microsoft.graph.http.BaseRequestBuilder;
import com.microsoft.graph.http.ICollectionResponse;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.Request;

public class Graph {
	final IAuthenticationProvider auth;
	final GraphServiceClient<Request> graphClient;
	
	public Graph(IAuthenticationProvider auth) {
		this.auth = auth;
		graphClient = GraphServiceClient.builder()
				.authenticationProvider(auth)
				.buildClient();
	}
	
	public List<Group> getTeams() {
		return readAll(graphClient.groups().buildRequest(new QueryOption("$filter", "startswith(displayName,'F-AAD-TEAM-')")));
	}
	
	private <T, T2 extends ICollectionResponse<T>, T3 extends BaseCollectionPage<T, ? extends BaseRequestBuilder<T>>> List<T> readAll(BaseEntityCollectionRequest<T, T2, T3> request) {
		return readAll(new ArrayList<>(), request);
	}

	private <T, T2 extends ICollectionResponse<T>, T3 extends BaseCollectionPage<T, ? extends BaseRequestBuilder<T>>> List<T> readAll(List<T> all, BaseEntityCollectionRequest<T, T2, T3> request) {
		T3 page = request.get();
		all.addAll(page.getCurrentPage());
		BaseCollectionRequestBuilder<?, ?, ?, ?, ?> builder = (BaseCollectionRequestBuilder) page.getNextPage();
		if (builder != null) {
			readAll(all, (BaseEntityCollectionRequest<T, T2, T3>) builder.buildRequest());
		}
		return all;
	}

}
