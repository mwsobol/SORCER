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

package sorcer.security.util;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import sorcer.core.SorcerConstants;
import sorcer.util.Crypt;

public class Auth {

	private Auth() {
		// Utility class
	}

	public static byte[] getSalt() {
		SecureRandom sr = new SecureRandom();
		byte[] salt = new byte[12];
		sr.nextBytes(salt);
		return salt;
	}

	public static String getEncriptedPassword(char[] password) {
		return (new Crypt().crypt(String.copyValueOf(password),
				SorcerConstants.SEED));
	}

	public static String getEncriptedPassword1(char[] password) {
		return encriptPassword(password, getSalt());
	}

	public static String encriptPassword(char[] password, byte[] salt) {
		return new String(salt)
				+ (new Crypt().crypt(String.copyValueOf(password), new String(
						salt)));
	}

	public static boolean isPasswordSame(String oldPass, String newPass) {
		byte[] salt = oldPass.substring(0, 12).getBytes();
		String pass = encriptPassword(newPass.toCharArray(), salt);
		return oldPass.equals(pass);
	}

	public static Subject createSubject(Principal principal) {
		Subject subject = null;
		if (principal == null)
			return null;
		Set principals = new HashSet();
		principals.add(principal);
		subject = new Subject(true, principals, new HashSet(), new HashSet());
		return subject;
	}

	public static SorcerPrincipal getGAppPrincipal(Subject subject) {
		// Util.debug(this,"Subject is ="+subject);
		if (subject == null)
			return null;
		java.util.Set principals = subject.getPrincipals();
		java.util.Iterator iterator = principals.iterator();
		while (iterator.hasNext()) {
			Principal p = (Principal) iterator.next();
			if (p instanceof SorcerPrincipal)
				return (SorcerPrincipal) p;
		}
		return null;
	}

}
