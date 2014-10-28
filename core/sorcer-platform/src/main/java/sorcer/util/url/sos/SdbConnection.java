/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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
package sorcer.util.url.sos;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import sorcer.core.context.ServiceContext;
import sorcer.core.provider.DatabaseStorer.Store;
import sorcer.core.provider.Provider;
import sorcer.core.provider.StorageManagement;
import sorcer.service.Context;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLookup;
import sorcer.util.bdb.objects.SorcerDatabaseViews;

/**
 * @author Mike Sobolewski
 *
 * sdb URL = sos://serviceType/providerName#objectType=Uuid
 * 
 * objectType = context, exertion, table, var, varModel, object
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SdbConnection extends URLConnection {

	private StorageManagement store;
	
	private String serviceType;
	
	private String providerName;
	
	private Store storeType;
	
	private String uuid;
	
	public  SdbConnection(URL url) {
		super(url);
		serviceType = getURL().getHost();
		providerName = getURL().getPath().substring(1);
		String reference = getURL().getRef();
		int index = reference.indexOf('=');
		storeType = SorcerDatabaseViews.getStoreType(reference.substring(0, index));
		uuid = reference.substring(index + 1);
	}

	/* (non-Javadoc)
	 * @see java.net.URLConnection#connect()
	 */
	@Override
	public void connect() throws IOException {
		//Provider provider = (Provider)ProviderLookup.getProvider(providerName, serviceType);
        try {
            Provider provider = (Provider) ProviderAccessor.getProvider(providerName, Class.forName(serviceType));
            store = (StorageManagement)provider;
            connected = true;
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not access StorageManagement implementation " + serviceType, e);
        }
	}

	@Override
	public Object getContent() throws IOException {
		Context outContext = null;
		if (!connected)
			connect();
		try {
			if (store != null) {
				Context cxt = new ServiceContext();
				cxt.putInValue(StorageManagement.object_type, storeType);
				cxt.putInValue(StorageManagement.object_uuid, uuid);
				outContext = store.contextRetrieve(cxt);
			}

		} catch (Exception e) {
			throw new IOException(e);
		}
		try {
			return outContext.getValue(StorageManagement.object_retrieved);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
