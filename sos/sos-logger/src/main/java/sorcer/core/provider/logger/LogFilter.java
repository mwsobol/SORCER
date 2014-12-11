/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.core.provider.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogFilter {
	private List<String> lineList;
	private List<String> input;
	public final static Level[] levels = { Level.FINEST, Level.FINER,
			Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE };

	public LogFilter(File file) {
		lineList = new ArrayList<String>();
	}

	public LogFilter(List<String> input) {
		this.input = input;
	}

	/**
	 * Filters out the list of <code>String</code> lines including the parameter text.
	 * 
	 * @param text
	 *            The text to filter from the log records
	 *            
	 * @return the list of matching log records
	 */
	public List<String> textFilter(String text, String level) {
		List<String> temp = new ArrayList<String>();
		lineList = new ArrayList<String>();
		List<Pattern> patterns = new ArrayList<Pattern>();
		Pattern tp = null;
		Pattern p = null;

		if (text != null && text.length() > 0) {
			tp = Pattern.compile(text);
		}
		Level il = Level.parse(level);
		for (Level l : levels) {
			if (l.intValue() >= il.intValue()) {
				p = Pattern.compile("" + l + ":");
				patterns.add(p);
			}
		}
		Matcher m;
		String l;
		boolean matchExpression = (tp != null);
		for (int i = 0; i < input.size(); i++) {
			l = input.get(i);
			for (Pattern pat : patterns) {
				m = pat.matcher(l);
				if (m.find()) {
					if (matchExpression) {
						temp = getLines(i);
						for (String tl : temp) {
							m = tp.matcher(tl);
							if (m.find()) {
								lineList.addAll(temp);
								break;
							}
						}
					} else {
						lineList.addAll(getLines(i));
					}
					break;
				}
			}
		}
		return lineList;
	}

	private List<String> getLines(int index) {
		List<String> lines = new ArrayList<String>();
		lines.add(input.get(index - 1));
		lines.add(input.get(index));
		int end = getEndRecord(index);
		for (int k = index + 1; k < end - 1; k++) {
			lines.add(input.get(k));
		}
		return lines;
	}

	private int getEndRecord(int recordStartIndex) {
		String line;
		for (int j = recordStartIndex + 1; j < input.size(); j++) {
			line = input.get(j);
			for (Level l : levels) {
				if (line.indexOf("" + l + ":") == 0)
					return j;
			}
		}
		return recordStartIndex;
	}
}