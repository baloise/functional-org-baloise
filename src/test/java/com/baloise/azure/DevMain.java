package com.baloise.azure;

import java.io.IOException;

public class DevMain {
	public static void main(String args[]) throws IOException {
		new DevServer(FunctionalOrgEndpoint.class).start();
	}
}