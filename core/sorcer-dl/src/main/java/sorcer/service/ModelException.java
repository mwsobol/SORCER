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

import sorcer.service.ContextException;

/**
 * Created by sobolemw on 7/30/15.
 */
public class ModelException extends ContextException {



    private static final long serialVersionUID = -1L;


    public ModelException() {
    }

    public ModelException(Exception exception) {
        super(exception);
    }

    public ModelException(String msg, Exception e) {
        super(msg, e);
    }

    public ModelException(String msg) {
        super(msg);
    }

}
