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

import java.io.Serializable;

/**
 * The Stopwatch class</b>
 */

public final class Stopwatch implements Serializable {
	public String name; // Name of this stopwatch
	private long startTime; // Time when this stopwatch started
	private long stopTime; // Time when it stopped
	private boolean running; // Flag to indicate run state

	/**
	 * Create a new named Stopwatch.
	 * 
	 * @param name
	 *            the name of this Stopwatch object
	 */
	public Stopwatch(String name) {
		this.name = name;
	}

	/**
	 * Create a new Stopwatch.
	 */
	public Stopwatch() {
	}

	/**
	 * (Re-)start the stopwatch.
	 */
	public void start() {
		startTime = System.currentTimeMillis();
		running = true;
	}

	/**
	 * Stop the stopwatch.
	 */
	public void stop() {
		stopTime = System.currentTimeMillis();
		running = false;
	}

	/**
	 * Stop the stopwatch with reset of start time.
	 */
	public void stop(long startTime) {
		stopTime = System.currentTimeMillis();
		startTime = startTime;
		running = false;
	}

	/**
	 * Get the current stopwatch time in milliseconds. This method can be used
	 * as a lap timer.
	 * 
	 * @return The time elapsed so far in milliseconds. Stopped stopwatches will
	 *         return the time when they were stopped.
	 */
	public long get() {
		long elapsed;
		if (running == true)
			elapsed = System.currentTimeMillis() - startTime;
		else
			elapsed = stopTime - startTime;
		return elapsed;
	}

	/**
	 * Get the difference between the current system time and startTime in
	 * milliseconds.
	 */
	public static long get(long startTime) {
		return System.currentTimeMillis() - startTime;
	}

	/**
	 * Get the current stopwatch time as a string.
	 */
	public String getTime() {
		return getTimeString(get());
	}

	/**
	 * * Get the difference between the current system time and startTime as a
	 * string.
	 */
	public String getTime(long startTime) {
		return getTimeString(System.currentTimeMillis() - startTime);
	}

	/**
	 * Get the time as a string.
	 */
	public static String getTimeString(long time) {
		if (time < 1000)
			return "" + time + " msec";
		else if (time < 60000)
			return "" + time / 1000 + " sec";
		else {
			long min = time / 60000;
			long sec = time - (min * 60000);
			if (sec > 0)
				return "" + min + " min " + sec / 1000 + " sec";
			else
				return "" + min + " min";
		}
	}

	/**
	 * Get the current time in milliseconds.
	 */
	public long getCurrentTime() {
		return System.currentTimeMillis();
	}
}
