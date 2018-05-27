/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
package sorcer.service.modeling;

import sorcer.service.Context;

@SuppressWarnings("rawtypes")
public class ExploreException extends Exception {

    static final long serialVersionUID = 5042679880302487033L;

    private Context context;

    public ExploreException() {
    }

    public ExploreException(String msg) {
        super(msg);
    }

    public ExploreException(Context context) {
        this.context = context;
    }

    public ExploreException(String msg, Context context) {
        super(msg);
        this.context = context;
    }

    public ExploreException(Exception exception) {
        super(exception);
    }

    public ExploreException(String msg, Exception e) {
        super(msg, e);

    }

    public ExploreException(String msg, Context context, Exception e) {
        super(msg, e);
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}
