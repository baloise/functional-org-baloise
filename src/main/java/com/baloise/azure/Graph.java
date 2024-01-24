package com.baloise.azure;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.EnumSet.noneOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.baloise.funorg.Role;
import com.baloise.funorg.Team;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.http.BaseCollectionRequestBuilder;
import com.microsoft.graph.http.BaseEntityCollectionRequest;
import com.microsoft.graph.http.BaseRequestBuilder;
import com.microsoft.graph.http.ICollectionResponse;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.Request;

public class Graph {
	final IAuthenticationProvider auth;
	final GraphServiceClient<Request> graphClient;
	Map<User, Set<Role>> lazyUserRoles;
	
	public Graph(IAuthenticationProvider auth) {
		this.auth = auth;
		graphClient = GraphServiceClient.builder()
				.authenticationProvider(auth)
				.buildClient();
	}
	
	public List<Group> getTeams() {
		return getTeams("");
	}
	public List<Group> getTeams(String filter) {
		return getGroups(Team.PREFIX.concat(filter));
	}

	private List<Group> getGroups(String filter) {
		return readAll(graphClient.groups().buildRequest(new QueryOption("$filter", format("startswith(displayName,'%s')", filter))));
	}
	
	private <T, T2 extends ICollectionResponse<T>, T3 extends BaseCollectionPage<T, ? extends BaseRequestBuilder<T>>> List<T> readAll(BaseEntityCollectionRequest<T, T2, T3> request) {
		return readAll(new ArrayList<>(), request);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T, T2 extends ICollectionResponse<T>, T3 extends BaseCollectionPage<T, ? extends BaseRequestBuilder<T>>> List<T> readAll(List<T> all, BaseEntityCollectionRequest<T, T2, T3> request) {
		T3 page = request.get();
		all.addAll(page.getCurrentPage());
		BaseCollectionRequestBuilder<?, ?, ?, ?, ?> builder = (BaseCollectionRequestBuilder) page.getNextPage();
		if (builder != null) {
			readAll(all, (BaseEntityCollectionRequest<T, T2, T3>) builder.buildRequest());
		}
		return all;
	}

	private List<DirectoryObject> getGroupMembersByDisplayName(String displayName) {
		return getGroupMembers(getGroups(displayName).get(0));
	}
	
	public List<DirectoryObject> getGroupMembers(Group group) {
		return readAll(graphClient.groups().byId(group.id).members().buildRequest());
	}
	
	public List<DirectoryObject> getGroupMembers(Role role) {
		return getGroupMembersByDisplayName(role.entitlement);
	}
	
	public byte[] avatar(String id) throws IOException {
		try(InputStream is = graphClient.users(id).photo().content().buildRequest().get()){
			return is.readAllBytes();
		}
	}

	public Set<Role> getRoles(User user) {
		if(lazyUserRoles == null) {
			lazyUserRoles = new TreeMap<User, Set<Role>>(comparing(u->u.id));
			EnumSet.allOf(Role.class).forEach(role-> {
				getGroupMembers(role).forEach(u-> lazyUserRoles.getOrDefault(user, noneOf(Role.class)).add(role));
			});
		}
		
		return lazyUserRoles.getOrDefault(user,noneOf(Role.class));
	}

}
