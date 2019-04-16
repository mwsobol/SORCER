package edu.pjatk.inn.greeter.one;

import edu.pjatk.inn.greeter.Greeter;

public class FormalGreeter implements Greeter {

	private String name;

	public FormalGreeter(String name) {
		this.name = name;
	}

	public String sayHello() {
		return "Hello, " + name + "!";
	}

}
