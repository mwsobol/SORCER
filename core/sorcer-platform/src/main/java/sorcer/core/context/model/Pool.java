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

package sorcer.core.context.model;

import java.io.Serializable;

import sorcer.service.modeling.Modeling.ParType;

	public class Pool implements Serializable {
		static final long serialVersionUID = -2702606663815246214L;
		public int maxThreads;
		/**
		 * loadFactor threshold for creating new threads. A new thread is
		 * created if the total number of runnable tasks (both active and
		 * pending) exceeds the number of threads times the loadFactor, and the
		 * maximum number of threads has not been reached.
		 */
		public float loadFactor;
		ParType type;

		public Pool() {
			type = ParType.THREAD;
		}

		public Pool(ParType type) {
			this.type = type;
			maxThreads = 10;
			loadFactor = 3.0f;
		}

		public Pool(ParType type, int size) {
			this(type, size, 0);
		}

		public Pool(ParType type, int size, float batchSize) {
			this.type = type;
			this.maxThreads = size;
			this.loadFactor = batchSize;
		}

		public int getSize() {
			return maxThreads;
		}

		public ParType getType() {
			return type;
		}

		public float getBatchSize() {
			return loadFactor;
		}
	}
