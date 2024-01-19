package com.baloise.azure;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static java.util.logging.Logger.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class DevServer {
	
	private class ParameterMapping {
		
		private Parameter[] parameters;
		Map<String, SimpleEntry<Integer, String>> bindings = new HashMap<>();

		public ParameterMapping(Method method) {
			parameters = method.getParameters();
			for (Parameter p: parameters) {
				HttpTrigger trigger = p.getAnnotation(HttpTrigger.class);
				if(trigger != null) {
					int i = 1;
					for (String pathElement: parsePath(trigger.route())) {
						if(pathElement.startsWith("{") && pathElement.endsWith("}")) {
							pathElement=pathElement.substring(1, pathElement.length()-1);
							String[] nameAndDefault = pathElement.split("=", 2);
							bindings.put(nameAndDefault[0], new SimpleEntry<Integer, String>(i, nameAndDefault.length >1 ? nameAndDefault[1] : null));
						}
						i++;
					}
					break;
				}
			}
		}
		
		public Object[] map(LinkedList<String> path, HttpExchange exg) {
			return stream(parameters).map(p->{
				if(p.getType().isAssignableFrom(ExecutionContextImpl.class)) {
					return new ExecutionContextImpl();
				}
				if(p.getType().isAssignableFrom(HttpRequestMessageImpl.class)) {
					return new HttpRequestMessageImpl(exg);
				}
				BindingName bindingName = p.getAnnotation(BindingName.class);
				if(bindingName != null) {
					SimpleEntry<Integer, String> binding = bindings.get(bindingName.value());
					if(binding == null) {
						throw new IllegalArgumentException("Binding not found for "+bindingName);
					}
					return path.size() > binding.getKey() ? path.get(binding.getKey()) : binding.getValue();
				}
				throw new IllegalArgumentException("Don't know how to map parameter "+p);
			}).toArray();
		}
		
	}
	
	private Map<String, Method> functionMapping = new HashMap<>();
	private Map<Class<?>, Object> instanceMapping = new HashMap<>();
	private Map<Method, ParameterMapping> parameterMappings = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	public DevServer(Class<?> ... functionClasses) {
		stream(functionClasses).distinct()
		.map(clazz ->{
			return stream(clazz.getMethods())
				.mapMulti((method, consumer)-> {
					FunctionName functionName = method.getAnnotation(FunctionName.class);
					if(!isNull(functionName)) {
						parameterMappings.put(method, new ParameterMapping(method));
						consumer.accept(new SimpleEntry<String, Method>(functionName.value(), method));
					} 
				})
				.collect(Collectors.toList());
		})
		.flatMap(Collection::stream)
		.forEach(e -> functionMapping.put(((SimpleEntry<String, Method>)e).getKey(), ((SimpleEntry<String, Method>)e).getValue()));
	}

	private final class ExecutionContextImpl implements ExecutionContext {
		private String invocationId = randomUUID().toString();

		@Override
		public Logger getLogger() {
			return logger;
		}

		@Override
		public String getInvocationId() {
			return invocationId;
		}

		@Override
		public String getFunctionName() {
			//TODO return name from annotation
			return name;
		}
	}

	private final class HttpRequestMessageImpl implements HttpRequestMessage<Optional<String>> {
		private HttpExchange exg;
		private Map<String, String> queryParameters;
		private Map<String, String> headers;

		public HttpRequestMessageImpl(HttpExchange exg) {
			this.exg = exg;
		}

		@Override
		public URI getUri() {
			return exg.getRequestURI();
		}

		@Override
		public Map<String, String> getQueryParameters() {
			if(queryParameters == null) {
				 queryParameters = new LinkedHashMap<String, String>();
					try {
						String rawQuery = exg.getRequestURI().getRawQuery();
						if(rawQuery!=null) {
							for (String pair : rawQuery.split("&")) {
								int idx = pair.indexOf("=");
								if(idx>-1) {
									queryParameters.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
								}
							}
						}
					} catch (UnsupportedEncodingException e) {
						throw new IllegalStateException(e);
					}
			}
			return queryParameters;
		}

		@Override
		public HttpMethod getHttpMethod() {
			return HttpMethod.value(exg.getRequestMethod());
		}

		@Override
		public Map<String, String> getHeaders() {
			if(headers == null) {
				headers = new HashMap<>();
				exg.getRequestHeaders().forEach((k,vs)-> headers.put(k, vs.get(0)));
			}
			return headers;
		}

		@Override
		public Optional<String> getBody() {
			 try (Scanner scanner = new Scanner(exg.getRequestBody(), StandardCharsets.UTF_8.name())) {
			       return Optional.of(scanner.useDelimiter("\\A").next());
			 }
		}

		@Override
		public Builder createResponseBuilder(HttpStatusType status) {
			return new ResponseBuilderImpl(status);
		}

		@Override
		public Builder createResponseBuilder(HttpStatus status) {

			return createResponseBuilder((HttpStatusType) status);
		}
	}

	private final class ResponseBuilderImpl implements Builder {
		private final class HttpResponseMessageImpl implements HttpResponseMessage {
			@Override
			public HttpStatusType getStatus() {
				return status;
			}

			@Override
			public String getHeader(String key) {
				return headers.getOrDefault(key, "undefined");
			}
			
			public Map<String, String> getHeaders() {
				return headers;
			}

			@Override
			public Object getBody() {
				return body;
			}
		}

		private HttpStatusType status;
		private Object body;
		Map<String, String> headers = new HashMap<>();
	
		private ResponseBuilderImpl(HttpStatusType status) {
			this.status = status;
		}
	
		@Override
		public Builder status(HttpStatusType status) {
			this.status = status;
			return this;
		}
	
		@Override
		public Builder header(String key, String value) {
			headers.put(key, value);
			return this;
		}
	
		@Override
		public Builder body(Object body) {
			this.body = body;
			return this;
		}
	
		@Override
		public HttpResponseMessage build() {
			return new HttpResponseMessageImpl();
		}
	}

	String name = getClass().getSimpleName();
	Logger logger = getLogger(name);

	public void start() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(7071), 0);

		server.createContext("/api", exg -> {
			try(OutputStream out = exg.getResponseBody()) {
				LinkedList<String> path = parsePath(exg.getRequestURI().toString());
				path.pop(); // api
				Method method = functionMapping.get(path.peek());
				ResponseBuilderImpl.HttpResponseMessageImpl resp = (ResponseBuilderImpl.HttpResponseMessageImpl) method.invoke(getInstance(method), parameterMappings.get(method).map(path, exg));
				resp.getHeaders().forEach((k,v)-> exg.getResponseHeaders().add(k,v));
				byte response[] = getResponseBytes(resp);
				exg.sendResponseHeaders(resp.getStatusCode(), response.length);
				out.write(response);
			} catch (Throwable t) {
				logger.log(Level.WARNING, t.getLocalizedMessage(), t);
			}
		});

		server.start();
	}

	private byte[] getResponseBytes(com.baloise.azure.DevServer.ResponseBuilderImpl.HttpResponseMessageImpl resp) throws UnsupportedEncodingException {
		Object body = resp.getBody();
		if(body instanceof byte[]) {
			return (byte[]) body;
		}
		return body.toString().getBytes("UTF-8");
	}

	private LinkedList<String> parsePath(String string) {
		String[] pathAndQuery = string.split(Pattern.quote("?"),2);
		LinkedList<String> path = new LinkedList<String>(asList(pathAndQuery[0].split("/")));
		path.pop(); // empty
		return path;
	}

	private Object getInstance(Method method) throws Exception {
		Class<?> declaringClass = method.getDeclaringClass();
		Object object = instanceMapping.get(declaringClass);
		if(object == null) {
			object = declaringClass.getDeclaredConstructor().newInstance();
			instanceMapping.put(declaringClass, object);
		}
		return object;
	}
}