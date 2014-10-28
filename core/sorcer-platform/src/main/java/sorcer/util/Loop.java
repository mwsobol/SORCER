package sorcer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Loop {
	private int from = 0;
	private int to;
	private String target;
	private String template;
	private List<String> templates;
	private List<Loop> loops;
	
	final static private char delimiter = '$';
	
	public Loop() {
		// do nothing
	}

	public Loop(int to) {
		this.to = to;
		this.from = 1;
	}
	
	public Loop(int from, int to) {
		this.to = to;
		this.from = from;
	}
	
	public Loop(String template, int from, int to) {
		this.to = to;
		this.from = from;
		this.template = template;

	}

	public Loop(List<String> templates, int to) {
		this.to = to;
		this.templates = templates;
	}
	
	private List<Loop> getLoops() {
		List<Loop> loops = new ArrayList<Loop>();
		if (templates != null && templates.size() > 0) {
			for (String template : templates) {
				String[] tokens = toArray(template);
				int from = new Integer(tokens[1]);
				Loop l = new Loop(tokens[0].trim(), from, to + from-1);
				l.setTarget(target);
				loops.add(l);
			}
		} else
			loops.add(this);
		this.loops = loops;
		return loops;
	}
	
	public int count() {
		return to - from + 1;
	}
	
	public List<String> getNames(String name) {
		if (templates != null && templates.size() > 0 ) {
			if (loops == null)
				loops = getLoops();
			for (Loop l : loops)
				l.setTarget(name);			
			return getUpdatedNames(loops.get(0).getLoopInstances(), loops);
		}
		target = name;
		List<String> instances = null;
		if (template != null) {
			return getLoopInstances();
		} else {
			instances = new ArrayList<String>(to - from + 1);
			for (int i = from; i <= to; i++) {
				instances.add(name + i);
			}
		}
		return instances;
	}

	private List<String> getUpdatedNames(List<String> names, List<Loop> loops) {
		for (int i = 1; i < loops.size(); i++) {
			names = loops.get(i).getNames(names);
		}
		return names;
	}
	
	public List<String> getNames(List<String> names) {
		List<String> upadtedNames = new ArrayList<String>(names.size());
		for (int i = from; i <= to; i++) {
			upadtedNames.add(names.get(i-from).replaceAll("\\" + delimiter  + template + "\\" + delimiter, "" + i));
			
		}
		return upadtedNames;
	}
	
	public List<String> getLoopInstances() {
		List<String> instances = new ArrayList<String>(to - from + 1);
		String token = getToken();
		for (int i = from; i <= to; i++) {
			instances.add(target.replaceAll("\\" + delimiter  + token + "\\" + delimiter, "" + i));
		}
		return instances;
	}

	public String update(String input, int index) {
		String pattern = template;
		if (template == null) {
			pattern = "\\" + delimiter  + getToken() + "\\" + delimiter;
		}
		String out = input.replaceAll(pattern, "" + index);
		return out;
	}
	
	private String getToken() {
		if (template != null)
			return template;
		int start = target.indexOf(delimiter);
		int end = target.indexOf(delimiter, start+1);			
		return target.substring(start+1, end);
	}
	
	public static String getRegexTemplate(String input) {
		return "\\" + delimiter  + input + "\\" + delimiter;
	}
	
	public static String getTemplate(String input) {
		int start = input.indexOf(delimiter);
		int end = input.indexOf(delimiter, start+1);			
		return "\\" + delimiter  + input.substring(start+1, end) + "\\" + delimiter;
	}
	
	private static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, ":");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("Loop: ");
		sb.append(" from=" + from).append(" to=" + to)
		.append(" target=" + target).append(" template=" + template)
		.append(" templates=" + templates);
		return sb.toString();
	}
}