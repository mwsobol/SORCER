/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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

package sorcer.core.dispatch;

/**
 * The SortingException is thrown when the ExertionSorter finds a problem with the
 * Flow or the dependencies in the analyzed job
 * 
 *
 * @author Pawel Rubach
 */

public class SortingException extends Exception {


    private static final long serialVersionUID = 253265266562095726L;

    public SortingException() {
        super();
    }

    public SortingException(String msg) {
        super(msg);
    }

    public SortingException(Exception exception) {
        super(exception);
    }

    public SortingException(String msg, Exception exception) {
        super(msg, exception);
    }
    
}

