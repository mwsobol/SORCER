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

import java.io.Serializable;
import java.security.Principal;

import sorcer.core.SorcerConstants;
import sorcer.util.SorcerUtil;

/**
 * Implementation of the Principal interface.
 */
public class SorcerPrincipal implements Principal, Serializable, SorcerConstants {
	protected String id = "0";

	protected String name = ANONYMOUS;

	protected String SSO = ANONYMOUS;

	protected String role = ANONYMOUS;

	// / protected String appRole = Const.ANONYMOUS;
	protected int roles = ANONYMOUS_CD;

	protected char[] pass = ANONYMOUS.toCharArray();

	protected String permissions = NONE;

	protected boolean isAuth = false;

	protected boolean useSSO = false;

	protected String sessionID;

	protected String project;

	protected String emailId;

	protected boolean exportControl = true;

	protected int accessClass = 1;

	public String firstName;

	public String lastName;

	public String phone;

	public SorcerPrincipal() {
		// do nothing
	}

	public SorcerPrincipal(String name) {
		this.name = name;
	}

	public boolean equals(java.lang.Object obj) {
		if (!(obj instanceof SorcerPrincipal)) {
			return false;
		}
		SorcerPrincipal other = (SorcerPrincipal) obj;
		if (name.equals(other.getName())) {
			return true;
		}
		return false;
	}

	public String getName() {
		return name;
	}

	public void setName(String principalName) {
		name = principalName;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getSSO() {
		return SSO;
	}

	public void setSSO(String principalSSO) {
		SSO = principalSSO;
	}

	public String getId() {
		return id;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public void setId(String principalId) {
		id = principalId;
	}

	public boolean isAuth() {
		return isAuth;
	}

	public void isAuth(boolean state) {
		isAuth = state;
	}

	public boolean useSSO() {
		return useSSO;
	}

	public void useSSO(boolean state) {
		useSSO = state;
	}

	public int hashCode() {
		return name.hashCode();
	}

	public java.lang.String toString() {
		return name;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public int getRoles() {
		return roles;
	}

	public void setRoles(int roles) {
		this.roles = roles;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	public char[] getPassword() {
		return pass;
	}

	public void setPassword(String password) {
		pass = password.toCharArray();
	}

	public void setPassword(char[] password) {
		pass = password;
	}

	public void clearPassword() {
		if (pass == null) {
			return;
		}
		for (int i = 0; i < pass.length; i++) {
			pass[i] = ' ';
		}
		pass = null;
	}

	public String asString() {
		return new StringBuffer("GAppPrincipal").append(SEP).append(id).append(
				SEP).append(name).append(SEP).append(role).append(SEP).append(
				SSO).append(SEP).append(emailId).append(SEP).append(
				exportControl ? "1" : "0").append(SEP).append(
				Integer.toString(accessClass)).append(SEP).append(firstName)
				.append(SEP).append(lastName).append(SEP).append(phone).append(
						SEP).append(new String(pass)).toString();

	}

	public static SorcerPrincipal fromString(String str) {
		if (str == null)
			return null;
		if (!str.startsWith("GAppPrincipal|"))
			return null;

		else {
			String[] tok = SorcerUtil.tokenize(str, SEP);
			SorcerPrincipal gp = new SorcerPrincipal();
			gp.setId(tok[1]);
			gp.setName(tok[2]);
			gp.setRole(tok[3]);
			gp.setSSO(tok[4]);
			gp.setEmailId(tok[5]);
			gp.setExportControl(!"0".equals(tok[6]));
			gp.setAccessClass(Integer.parseInt(tok[7]));
			gp.firstName = tok[8];
			gp.lastName = tok[9];
			gp.phone = tok[10];
			gp.setPassword(tok[11]);
			return gp;
		}
	}

	public boolean isExportControl() {
		return exportControl;
	}

	public void setExportControl(boolean exportControl) {
		this.exportControl = exportControl;
	}

	public void setAccessClass(int accessClass) {
		this.accessClass = accessClass;
	}

	public int getAccessClass() {
		return accessClass;
	}

	public void setSessionID(String sid) {
		sessionID = sid;
	}

	public String getSessionID() {
		return sessionID;
	}

	public boolean isAnonymous() {
		return ANONYMOUS.equals(name);

	}

}
