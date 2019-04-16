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

/**
 * The <code>MogramException</code> represents an exception that is thrown
 * if a mogram is evaluated or performed by a service provider failing to process it
 * correctly. A complementary related throwable and/or ill behaving mogram can
 * be embedded into this exception.
 *
 * @author Mike Sobolewski
 */
public class MogramException extends ServiceException {

    private static final long serialVersionUID = -1L;

    /**
     * The exertion relevant to this exception.
     */
    protected Mogram mogram;

    public MogramException() {
    }

    public MogramException(Mogram mogram) {
        this.mogram = mogram;
    }

    public MogramException(String msg) {
        super(msg);
    }

    public MogramException(Throwable e) {
        super(e);
    }

    /**
     * Constructs a <code>RoutineException</code> with the specified detailed
     * message and the relevant exertion.
     *
     * @param message
     *            the detailed message
     * @param mogram
     *            the embedded mogram
     */
    public MogramException(String message, Mogram mogram) {
        super(message);
        this.mogram = mogram;
    }

    public MogramException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a <code>RoutineException</code> with the specified detail
     * message and nested exception.
     *
     * @param message
     *            the detailed message
     * @param cause
     *            the nested throwable cause
     */
    public MogramException(String message, Mogram mogram, Throwable cause) {
        super(message, cause);
        this.mogram = mogram;
    }

    /**
     * Returns the embedded mogram causing this exception.
     *
     * @return embedded mogram
     */
    public Mogram getMogram() {
        return mogram;
    }

}
