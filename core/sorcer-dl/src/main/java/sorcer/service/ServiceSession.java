/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Created by Mike Sobolewski
 */
public interface ServiceSession {

        /**
         * Returns the last time the requestor sent a request associated with this
         * session, as the number of milliseconds since midnight January 1, 1970
         * GMT, and marked by the time the container received the request.
         * <p>
         * Actions that your application takes, such as getting or setting a execute
         * associated with the session, do not affect the access time.
         *
         * @return a <code>long</code> representing the last time the client sent a
         *         request associated with this session, expressed in milliseconds
         *         since 1/1/1970 GMT
         * @exception ContextException
         *                if this method is called on an invalidated session
         */
        public long getLastAccessedTime() throws ContextException;


        /**
         * Specifies the time, in seconds, between client requests before the
         * service provider will invalidate this session. A zero or negative time
         * indicates that the session should never timeout.
         *
         * @param interval
         *            An integer specifying the number of seconds
         */
        public void setMaxInactiveInterval(int interval);

        /**
         * Returns the maximum time interval, in seconds, that the service provider
         * will keep this session open between requestor accesses. After this interval,
         * the service provider will invalidate the session. The maximum time
         * interval can be set with the <code>setMaxInactiveInterval</code> method.
         * A zero or negative time indicates that the session should never timeout.
         *
         * @return an integer specifying the number of seconds this session remains
         *         open between client requests
         * @see #setMaxInactiveInterval
         */
        public int getMaxInactiveInterval();

        /**
         * Returns the object bound with the specified name in this session, or
         * <code>null</code> if no object is bound under the name.
         *
         * @param name
         *            a string specifying the name of the object
         * @return the object with the specified name
         * @exception ContextException
         *                if this method is called on an invalidated session
         */
        public Object getAttribute(String name) throws ContextException;

        /**
         * Returns an <code>Enumeration</code> of <code>String</code> objects
         * containing the names of all the objects bound to this session.
         *
         * @return an <code>Enumeration</code> of <code>String</code> objects
         *         specifying the names of all the objects bound to this session
         * @exception ContextException
         *                if this method is called on an invalidated session
         */
        public Iterator<String> getAttributeNames() throws ContextException;

        /**
         * Binds an object to this session, using the name specified. If an object
         * of the same name is already bound to the session, the object is replaced.
         * <p>
         * After this method executes, and if the new object implements
         * <code>net.jini.core.event.RemoteEventListener</code>, the provider calls
         * <code>net.jini.core.event.RemoteEventListener.notify</code>. The requestor then
         * notifies any related provider in the service federation.
         * <p>
         * If an object was already bound to this session of this name that
         * implements <code>net.jini.core.event.RemoteEventListener</code>, its
         * <code>ProviderSessionBindingListener.valueUnbound</code> method is called
         * to indicate the unbound execute of this session.<p>
         * If the execute passed in is null, this has the same effect as calling
         * <code>removeAttribute()</code>.
         *
         * @param name
         *            the name to which the object is bound; cannot be null
         * @param value
         *            the object to be bound
         * @exception ContextException
         *                if this method is called on an invalidated session
         */
        public void setAttribute(String name, Object value) throws ContextException;

        /**
         * Removes the object bound with the specified name from this session. If
         * the session does not have an object bound with the specified name, this
         * method does nothing.
         * <p>
         * After this method executes, and if the object implements
         * <code>net.jini.core.event.RemoteEventListener</code>, the provider calls
         * <code>net.jini.core.event.RemoteEventListener.notify/code>. The container then
         * notifies any related service in the exerting federation to indicate the unbound 
         * execute of this session.
         *
         * @param name
         *            the name of the object to remove from this session
         * @exception ContextException
         *                if this method is called on an invalidated session
         */
        public void removeAttribute(String name) throws ContextException;
    

        /**
         * Invalidates this session then unbinds any objects bound to it.
         */
        public void invalidate();

        /**
         * Returns <code>true</code> if the caller does not yet know about the
         * session or if the requestor chooses not to join the session. For example, if
         * the requestor had disabled the session then a session would be new on each request.
         *
         * @return <code>true</code> if the server has created a session, but the
         *         client has not yet joined
         */
        public boolean isNew();
    }

