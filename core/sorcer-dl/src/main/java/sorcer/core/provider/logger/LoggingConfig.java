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

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Stores the name of a logger and the Level of that logger. This class is used
 * by the {@link LogManager} and associated classes to store and configure how
 * it works.
 */
public class LoggingConfig implements Serializable {
	private String logger;
	private Level lev;

	/**
	 * Compare logger names
	 */
	public boolean equals(Object obj) {
		if (obj instanceof LoggingConfig == false)
			return false;
		return ((LoggingConfig) obj).logger.equals(logger);
	}

	/**
	 * computes hashCode() using the logger name
	 */
	public int hashCode() {
		return logger.hashCode();
	}

	/**
	 * Create a description of this instance
	 */
	public String toString() {
		return logger + "@" + lev;
	}

	/**
	 * Construct an instance using the supplied info
	 */
	public LoggingConfig(String logger, Level lev) {
		this.logger = logger;
		this.lev = lev;
	}

	/**
	 * Get the name of the logger
	 */
	public String getLogger() {
		return logger;
	}

	/**
	 * Get the associated level for this logger
	 */
	public Level getLevel() {
		return lev;
	}

	/**
	 * Set the new associated level for this logger
	 */
	public void setLevel(Level lev) {
		this.lev = lev;
	}
}