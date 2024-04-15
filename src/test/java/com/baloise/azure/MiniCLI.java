package com.baloise.azure;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class MiniCLI extends Thread {
	public static record Command (String cmdLine, String description, Runnable action) {}
	final Map<String, Command> commands;
	
	public MiniCLI(Command ... commands) {
		this.commands = new TreeMap<>();
		for (Command command : commands) {
			add(command);
		}
		setDaemon(true);
		setPriority(MIN_PRIORITY);
	}

	public MiniCLI add(Command command) {
		this.commands.put(command.cmdLine, command);
		return this;
	}

	@Override
	public void run() {
		print();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				String cmdLine = br.readLine().toLowerCase();
				Command cmd = commands.get(cmdLine);
				if(cmd!=null ) {
					cmd.action.run();
				} else {
					print();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void print() {
		System.out.flush();
		System.err.flush();
		for (Command command : commands.values()) {
			System.out.println(format("[%s] %s", command.cmdLine, command.description));
		}
	}
}
