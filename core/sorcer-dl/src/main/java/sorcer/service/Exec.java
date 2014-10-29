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


public interface Exec {

	// the order is consistent with the ordinal of sorcer.service.Category enum
	final static int FAILED             =  0;
    final static int INITIAL           	=  1;
    final static int INSPACE           	=  2;
    final static int RUNNING           	=  3;
    final static int DONE              	=  4;
    final static int STOPPED           	=  5;
    final static int SUSPENDED         	=  6;
    final static int RESUMED 			=  7;
    final static int NEXT_STEP 			=  8;
    final static int INSPACE_FOR_SLA   	=  9;
    final static int RETURNED		   	=  10;
    final static int UPDATED		   	=  11;
    
    // other for compatibility with sorcer.service.State 
    final static int ASYNC		   		=  12;
    final static int NULL		   		=  13;
    final static int ASIS		   		=  14;
    
    final static int ERROR            	= -1;  
    public static final int POISONED	= -2;

    //legacy stuff for cache server
    public static final int INVALID_CMD       	= -3;
    public static final int TRANSACTION_ERROR 	= -4;
    public static final int LOCK_ERROR       	= -5;
   

    /**
	 * A State can be associated with the ExecState and other attributes of
	 * executing exertions (see sorcer.core.provider.exertmonitor.ExertMonitor)
	 */
	public enum State {
		FAILED, INITIAL, INSPACE, RUNNING, DONE, STOPPED, SUSPENDED, RESUMED, NEXT_STEP, INSPACE_FOR_SLA, RETURNED, UPDATED, ASYNC, NULL, ASIS;
		
		static public String name(int state) {
			for (State s : State.values()) {
				if (state == s.ordinal())
					return "" + s;
			}
			return null;
		}
	}
	
}

