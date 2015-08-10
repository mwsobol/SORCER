package edu.pjatk.inn.greeter.one;

import java.util.logging.Logger;

public class Greeter {
	private final static Logger logger = Logger.getLogger("ttu.cs.CS3365");
	
	public String sayHello() {
		String message = "Hello, World!";
		logger.exiting(this.getClass().getName(), "sayHello", message);
		return message;
	}
}
