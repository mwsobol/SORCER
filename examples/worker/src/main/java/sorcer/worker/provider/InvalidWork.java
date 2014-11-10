package sorcer.worker.provider;

public class InvalidWork extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private String message;

	public InvalidWork(String s) {
		message = s;
	}

	public String getMessage() {
		return message;
	}
}