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

import sorcer.core.context.ServiceContext;
import sorcer.core.provider.DatabaseStorer.Store;
import sorcer.service.Exerter;
import sorcer.core.provider.StorageManagement;
import sorcer.service.Accessor;
import sorcer.service.Context;
import sorcer.util.bdb.objects.SorcerDatabaseViews;
import sorcer.util.bdb.objects.UuidObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Mike Sobolewski
 *
 * sdb URL = sos://serviceInfo/providerName#objectType=Uuid
 * 
 * objectType = context, exertion, dataTable, var, varModel, object
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
		//Provider provider = (Provider)ProviderLookup.getout(providerName, serviceInfo);
        try {
            Exerter provider = (Exerter) Accessor.get().getService(providerName, Class.forName(serviceType));
            store = (StorageManagement)provider;
            connected = true;
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not access StorageManagement implementation " + serviceType, e);
        }
	}

	@Override
	public Object getContent() throws IOException {
		Context outContext;
		if (!connected)
			connect();
		if (store == null)
			throw new IOException("Could not access StorageManagement implementation " + serviceType);
		try {
			outContext = null;
			if (store != null) {
				Context cxt = new ServiceContext();
//				TODO
				cxt.putInValue(StorageManagement.object_type, Store.object);
//				cxt.putInValue(StorageManagement.object_type, storeType);
				cxt.putInValue(StorageManagement.object_uuid, uuid);
				outContext = store.contextRetrieve(cxt);
			}
			Object obj =  outContext.getValue(StorageManagement.object_retrieved);
			if (obj instanceof UuidObject)
				return ((UuidObject)obj).getObject();
			else
				return obj;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
