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

import sorcer.service.Signature;
import sorcer.service.Strategy.Flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Sobolewski
 */
public class QueueStrategy implements Serializable {
	static final long serialVersionUID = -2199530268313502745L;

	private Signature builder;

	private boolean isRebuilt = true;

	// multiple thread pools
	private List<Pool> pools = new ArrayList<Pool>();

	private Flow flow = Flow.SEQ;

	// producer blocking queue producerCapacity
	private int producerCapacity = 10;

	// streaming blocking queue producerCapacity
	private int streamingCapacity = 100;

	// negative number - no wait time
	private int consumerWaitTime = -1;

	// the ratio of task queue size of the ThreadPoolExecutor
	// to the maximum size of thread pool; if negative the unbounded task queue
	private float consumerQueueFactor = -1;

	public Signature getBuilder() {
		return builder;
	}

	public void setBuilder(sorcer.service.Signature signature) {
		builder = signature;
	}

	public int getProducerCapacity() {
		return producerCapacity;
	}

	public void setProducerCapacity(int producerCapacity) {
		this.producerCapacity = producerCapacity;
	}


	public int getStreamingCapacity() {
		return streamingCapacity;
	}

	public void setStreamingCapacity(int streamingCapacity) {
		this.streamingCapacity = streamingCapacity;
	}

	public int getConsumerWaitTime() {
		return consumerWaitTime;
	}

	public void setConsumerWaitTime(int consumerWaitTime) {
		this.consumerWaitTime = consumerWaitTime;
	}

	public float getConsumerQueueFactor() {
		return consumerQueueFactor;
	}

	public void setConsumerQueueFactor(float consumerQueueFactor) {
		this.consumerQueueFactor = consumerQueueFactor;
	}

	public Flow getFlow() {
		return flow;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	public List<Pool> getPools() {
		return pools;
	}

	public void setPools(List<Pool> pools) {
		this.pools = pools;
	}

	public boolean isRebuilt() {
		return isRebuilt;
	}

	public void setRebuilt(boolean rebuilt) {
		isRebuilt = rebuilt;
	}

	public String toString() {
		return "[flow=" + flow + ", producerCapacity=" + producerCapacity + ", streamingCapacity="
				+ streamingCapacity + ", consumerQueueFactor="
				+ consumerQueueFactor + ", consumerWaitTime="
				+ consumerWaitTime + ", builder=" + builder + "]";
	}
}
