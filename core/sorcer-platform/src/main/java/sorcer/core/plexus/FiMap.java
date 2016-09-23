/*
* Copyright 2016 SORCERsoft.org.
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

package sorcer.core.plexus;

import sorcer.service.FidelityList;
import sorcer.util.DataTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mike Sobolewski on 6/7/16.
 */
public class FiMap extends HashMap<Integer, FidelityList> {

	private String fiColumnName = "fis";

	private List<FidelityList> fiList;

	/**
	 * Constructs an empty <tt>FiMap</tt> with the specified initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param  initialCapacity the initial capacity.
	 * @throws IllegalArgumentException if the initial capacity is negative.
	 */
	public FiMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs an empty <tt>FiMap</tt> with the default initial capacity
	 * (16) and the default load factor (0.75).
	 */
	public FiMap() {
		super();
		fiList = new ArrayList();
	}

	public FiMap(DataTable table) {
		fiList = table.getColumn(fiColumnName);
		if (fiList != null && fiList.size() > 0) {
			for (int i = 0; i < fiList.size(); i++) {
				put(i, fiList.get(i));
			}
		}
	}

	public FiEntry put(FiEntry fiEnt) {
		put(fiEnt.getIndex(), fiEnt.getFidelities());
		return fiEnt;
	}

	public FiEntry add(FiEntry fiEnt) {
		put(fiEnt);
		return fiEnt;
	}

	public FiEntry remove(FiEntry fiEnt) {
		remove(fiEnt.getIndex());
		return fiEnt;
	}

	public List<FidelityList> getFiList() {
		return fiList;
	}

	public String getFiColumnName() {
		return fiColumnName;
	}

	public void setFiColumnName(String fiColumnName) {
		this.fiColumnName = fiColumnName;
	}

	public void populateFidelities(int size) {
		int maxIdex = size;

		for (int i = 0; i < maxIdex; i++) {
			if (get(i) != null && get(i + 1) == null) {
				put(i + 1, get(i));
				fiList.set(i+1, get(i));
			}
		}
	}

	public void populateFidelities(FidelityList fiConfig, int size) {
		int maxIdex = size;

		for (int i = 0; i < maxIdex; i++) {
			if (get(i) != null && get(i + 1) == null) {
				put(i + 1, fiConfig);
				fiList.set(i+1, fiConfig);
			}
		}
	}
}
