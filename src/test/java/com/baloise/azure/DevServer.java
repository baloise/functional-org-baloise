package com.baloise.azure;

import static java.util.UUID.randomUUID;
import static java.util.logging.Logger.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class DevServer {
	private final class ExecutionContextImpl implements ExecutionContext {
		private String invocationId = randomUUID().toString();
		private HttpExchange exg;

		public ExecutionContextImpl(HttpExchange exg) {
			this.exg = exg;
		}

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
				exg.getRequestHeaders().forEach((k,vs)-> headers.put(k, vs.getFirst()));
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

	public static void main(String args[]) throws IOException {
		new DevServer().start();
	}

	private void start() throws IOException {
		Function function = new Function();

		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

		server.createContext("/", exg -> {
			
			ResponseBuilderImpl.HttpResponseMessageImpl resp = (ResponseBuilderImpl.HttpResponseMessageImpl) function.run(new HttpRequestMessageImpl(exg), new ExecutionContextImpl(exg));
			byte response[] = resp.getBody().toString().getBytes("UTF-8");
			resp.getHeaders().forEach((k,v)-> exg.getResponseHeaders().add(k,v));
			exg.sendResponseHeaders(resp.getStatusCode(), response.length);

			OutputStream out = exg.getResponseBody();
			out.write(response);
			out.close();
		});

		server.start();
	}
}