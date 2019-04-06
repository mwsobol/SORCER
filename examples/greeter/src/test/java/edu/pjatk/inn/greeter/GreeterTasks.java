package edu.pjatk.inn.greeter;

import edu.pjatk.inn.greeter.one.FormalGreeter;
import edu.pjatk.inn.greeter.two.CasualGreeter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class GreeterTasks {
	private final static Logger logger = LoggerFactory.getLogger(GreeterTasks.class);

	private Greeter fg;
	private Greeter cg;

	@Before
	public void setUp() {
		fg = new FormalGreeter("Mike");
		cg = new CasualGreeter();
	}

	@Test
	public void formalGreeter() {
		logger.info("fg.sayHello(): " + fg.sayHello());
		assertEquals(fg.sayHello(), "Hello, Mike!");
	}

	@Test
	public void casualGreeter() {
		logger.info("cg.sayHello(): " + cg.sayHello());
		assertEquals(cg.sayHello(), "Hi, Man!");
	}
}
	
	
