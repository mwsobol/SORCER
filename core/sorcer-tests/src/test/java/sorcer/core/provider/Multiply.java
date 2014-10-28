package junit.sorcer.core.provider;

import java.io.Serializable;

public class Multiply implements Serializable {

	public double multiply(double... args) {
		double result = args[0];
		for (int i = 1; i < args.length; i++) {
			result = result * args[i];
		}
		return result;
	}
	
	public double multiply(Double... args) {
		double result = args[0];
		for (int i = 1; i < args.length; i++) {
			result = result * args[i];
		}
		return result;
	}
}