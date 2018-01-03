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

package sorcer.service;


import java.util.List;

public interface Fi<T> extends Identifiable, Arg {

    final static int e = 0;
    final static int s = 1;
    final static int r = 2;
    final static int c = 3;
    final static int m = 4;
    final static int v =  5;
    final static int vFi = 6;
    final static int ev =  7;
    final static int gt =  8;
    final static int st =  9;
    final static int gr = 10;
    final static int dVar = 11;

    public enum Type implements Arg {
        VAL, ENTRY, SIG, REF, COMPONENT, MORPH, VAR, VAR_FI, PROC, SRV, NEO, EVALUATOR, GETTER, SETTER, GRADIENT, DERIVATIVE,
        MULTI, REQUEST, RESPONSE, UPDATE, ADD, REPLACE, DELETE, SELECT, META, NAME, SOA, IF, IF_SOA, SYS, CONTEXT, MODEL, MDA, CONFIG;

		static public String name(int fiType) {
			for (Type s : Type.values()) {
				if (fiType == s.ordinal())
					return "" + s;
			}
			return null;
		}

        static public Type type(int fiType) {
            for (Type s : Type.values()) {
                if (fiType == s.ordinal())
                    return s;
            }
            return null;
        }
        public String getName() {
			return toString();
		}
	}

    public T getSelect();

    public T get(int index);

    public T selectSelect(String fiName);

    public List<T> getSelects();

    public void addSelect(T select);

    public String getPath();

    public void setPath(String path);

    public void setSelect(T select);

    public boolean isValid();

    public Type getFiType();

    public boolean isChanged();

    public void setChanged(boolean state);

}

