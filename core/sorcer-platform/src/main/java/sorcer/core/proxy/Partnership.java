/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.proxy;

import sorcer.core.provider.ProviderException;

/**
 * Smart proxies and other proxies containing inner proxies (partners) can
 * extend their functionalty via calls on partners. This interface complements
 * {@link Outer} interface for setting inner and admin proxies, while
 * {@link Outer} defines partnership accessors.
 * 
 * @author Mike Sobolewski
 */
public interface Partnership {

	/**
	 * Sets an inner Remote proxy for this proxy. The inner proxy usually is
	 * exported by the service provider setting the inner proxy. The outer proxy
	 * can invoke methods on its inner proxy to extend its functionality
	 * accordingly.
	 * <p>
	 * This function may be called multiple times for each inner proxy. In this
	 * case, the implementor should check the instance multitype of inner.
	 * 
	 * @param inner
	 *            an inner proxy object
	 * @throws ProviderException
	 */
	public void setInner(Object inner) throws ProviderException;

	/**
	 * Returns the inner proxy of this provider. Inner proxies can be provided
	 * by the registering provider of this proxy or by third party providers.
	 * This proxy extends its local functionality by invoking remote methods on
	 * its inner proxy.
	 * 
	 * @return an inner proxy of thos proxy
	 * @throws ProviderException
	 */

	public void setAdmin(Object admin) throws ProviderException;
}
