/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/*
 * CVSStringTokenizer is a class that controls simple linear tokenization
 * of a String in csv formatted strings. The set of delimiters, which 
 * defaults to common whitespace characters, may be specified at creation 
 *time or on a per-token basis.<p>
 */
public class CSVStringTokenizer implements Enumeration {
	private int currentPosition;
	private int maxPosition;
	private String str;
	private String delimiters;
	private String quote;
	private boolean isQuote, isLastEmpty = false;;

	public CSVStringTokenizer(String str, String delim, String quotes) {
		currentPosition = 0;
		this.str = str;
		maxPosition = str.length();
		delimiters = delim;
		quote = quotes;
		isQuote = false;
	}

	public CSVStringTokenizer(String str, String delim) {
		this(str, delim, "\"");
	}

	public CSVStringTokenizer(String str) {
		this(str, " \t\n\r", "\"");
	}

	/**
	 * Skips delimites and quotes.
	 */
	private void skipDelimiters() {
		if (isQuote) {
			while ((currentPosition < maxPosition)
					&& (quote.indexOf(str.charAt(currentPosition)) >= 0)) {
				currentPosition++;
				isQuote = false;
			}
		}
		if ((currentPosition < maxPosition)
				&& (delimiters.indexOf(str.charAt(currentPosition)) >= 0))
			currentPosition++;
	}

	/**
	 * Returns true if more tokens exist.
	 */
	public boolean hasMoreTokens() {
		// skipDelimiters();
		return (currentPosition < maxPosition);
	}

	/**
	 * Returns true if the Enumeration has more elements.
	 */
	public boolean hasMoreElements() {
		return hasMoreTokens();
	}

	/**
	 * Returns the next element in the Enumeration.
	 * 
	 * @exception NoSuchElementException
	 *                If there are no more elements in the enumeration.
	 */
	public Object nextElement() {
		return nextToken();
	}

	/**
	 * Returns the next token of the String.
	 * 
	 * @exception NoSuchElementException
	 *                If there are no more tokens in the String.
	 */
	public String nextToken() {
		if (isLastEmpty) {
			isLastEmpty = false;
			currentPosition = currentPosition + 1;
			return "";
		}

		if (currentPosition >= maxPosition) {
			throw new NoSuchElementException();
		}

		if ((currentPosition < maxPosition)
				&& (quote.indexOf(str.charAt(currentPosition)) >= 0)) {
			currentPosition++;
			isQuote = true;
		}

		int start = currentPosition;

		if (isQuote) {
			while ((currentPosition < maxPosition)
					&& (quote.indexOf(str.charAt(currentPosition)) < 0)) {
				currentPosition++;
			}
		} else {
			while ((currentPosition < maxPosition)
					&& (delimiters.indexOf(str.charAt(currentPosition)) < 0)) {
				currentPosition++;
			}
		}
		String nstr = str.substring(start, currentPosition);

		// provide for the nextToken to return an empty string if
		// last char is delimiter
		if ((currentPosition == maxPosition - 1)
				&& delimiters.charAt(0) == str.charAt(maxPosition - 1)) {
			isLastEmpty = true;
		} else
			skipDelimiters();
		return nstr;
	}
}
