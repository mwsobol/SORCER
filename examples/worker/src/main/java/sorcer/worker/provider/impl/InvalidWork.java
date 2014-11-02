package sorcer.worker.provider.impl;

public class InvalidWork extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private String message;

	InvalidWork(String s) {
		message = s;
	}

	public String getMessage() {
		return message;
	}
}