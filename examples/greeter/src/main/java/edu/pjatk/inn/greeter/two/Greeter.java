package edu.pjatk.inn.greeter.two;

import java.util.logging.Logger;

public class Greeter {
	private final static Logger logger = Logger.getLogger("ttu.cs.CS3365");
	private String name;

	public Greeter(String aName) {
		name = aName;
	}

	public String sayHello() {
		String message = "Hello, " + name + "!";
		logger.exiting(this.getClass().getName(), "sayHello", message);
		return message;
	}

}
