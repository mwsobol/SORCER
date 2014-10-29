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

package sorcer.jini.lookup.entry;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import net.jini.entry.AbstractEntry;

public class CertificateEntry extends AbstractEntry {
	public byte[] encodedCert;
	public String certType = "X.509";

	public CertificateEntry() {
	}

	public CertificateEntry(Certificate cert)
			throws CertificateEncodingException {
		encodedCert = cert.getEncoded();
	}

	public Certificate getCertificate() throws CertificateException {
		ByteArrayInputStream bais = new ByteArrayInputStream(encodedCert);

		CertificateFactory cf = CertificateFactory.getInstance(certType);

		Certificate cert = cf.generateCertificate(bais);

		return cert;
	}

	public void setCertificate(Certificate cert)
			throws CertificateEncodingException {
		encodedCert = cert.getEncoded();
	}
}
