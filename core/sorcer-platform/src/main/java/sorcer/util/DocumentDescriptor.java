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

package sorcer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import sorcer.core.SorcerConstants;
import sorcer.security.util.ACLConvertor;
import sorcer.security.util.SorcerPrincipal;

public class DocumentDescriptor implements SorcerConstants, Serializable {

	// String defaultWarningDate = "SysDate + 7"; //7 days ahead
	// String defaultGoodUntil = "ADD_MONTHS(SysDate, 12)"; //12 months ahead
	// String dueDate, warningDate, goodUntil;

	private static final long serialVersionUID = -423568073848185377L;

	// commonInfo
	public static String project = "GApp";

	// status of document
	// GApp.DELETED / GApp.NEW / GApp.MODIFIED / GApp.UNMODIFIED
	private int status;

	// Document Info
	private String documentID;
	private String documentName;

	private String description;
	private SorcerPrincipal principal;
	private String accessClass;
	private Boolean isExportControlled;

	transient private Object documentACL;
	private Hashtable nonTransientACL;

	// Folder Info
	private String folderPath;
	private String folderID;

	// private Object folderACL
	// fwang
	private boolean isdir;
	private boolean move;
	private boolean root;
	private String nodeName; // createNode()/deleteNode() use
	private Map<String, Object> properties;
	private int srcseq;
	private int destseq;

	// Version Info
	private String activeVersionID;

	// key -> versionID eval -> (accessName + GApp.sep + VersionName)
	// accessName :This is the access key of the current version. ie, the
	// physical key by which it's store in FileSystem.
	// versionName :The logical key given to the file by the user.
	private Hashtable versions;

	// Information on the new version to be uploaded
	// By default it's the fileName
	public String newVerName;
	public String newVerDesc;

	// handles for upload and download
	// prc RemoteFileStore.getInputStream(DocumentDescriptor) to
	// getValue inputStream and similarlly for Output
	public InputStream in;
	public OutputStream out;

	// By default we do not overwrite a version
	private boolean overWriteVersion = false;

	// If client needs to download a file, there should be a activeVersionId
	// which he's trying to download. OR
	// He can give the following triplet < folderPath, documentName,
	// downloadVersionName > downloadVersionName is
	// optional. If not given, it's assumed you are looking for current version
	// of the document.
	private String downloadVersionName;
	public static ACLConvertor convertor;

	// the http url of file to be uploaded or downloaded setValue by the file store
	// provider
	public URL fileURL;

	public DocumentDescriptor() {
		// setDefaults
		isExportControlled = new Boolean(false);
		accessClass = PUBLIC;
		folderPath = "tmp";
		versions = new Hashtable();
		// accessClass = "1";
		isExportControlled = new Boolean(false);
	}

	// handleVersions
	// public void setactiveVersionID(String id) {
	// activeVersionID = id;
	// }

	public String getActiveVersionID() {
		return activeVersionID;
	}

	public void addVersion(String versionID, String accessName,
			String versionName) {
		if (versions.isEmpty())
			addActiveVersion(versionID, accessName, versionName);
		else
			versions.put(versionID, accessName + SEP + versionName);
	}

	public void addActiveVersion(String versionID, String accessName,
			String versionName) {
		versions.put(versionID, accessName + SEP + versionName);
		activeVersionID = versionID;
	}

	// Just for the client to upload new stuffs...
	public void addNewVersion(String versionName, String versionDescription) {
		newVerName = versionName;
		newVerDesc = versionDescription;
	}

	public String getActiveVersionAccessName() {
		if (activeVersionID == null) {
			System.out
					.println("Strange! This doc does not have a current version. Check from web client");
			return null;
		} else {
			String versionInfo = (String) versions.get(activeVersionID);
			return (versionInfo != null) ? versionInfo.substring(0, versionInfo
					.lastIndexOf(SEP)) : null;
		}
	}

	public void setDownloadVersionName(String versionName) {
		downloadVersionName = versionName;
	}

	public String getDownloadVersionName() {
		return downloadVersionName;
	}

	public void overWriteVersion(boolean isOverWrite) {
		overWriteVersion = isOverWrite;
	}

	public boolean isOverWriteVersion() {
		return overWriteVersion;
	}

	// getValue and sets
	// fwang ------start--------------
	public boolean isFolder() {
		return isdir;
	}

	public void setIsFolder(boolean isdirarg) {
		isdir = isdirarg;
	}

	public boolean isMove() {
		return move;
	}

	public void setIsMove(boolean mvarg) {
		this.move = mvarg;
	}

	public boolean isRoot() {
		return this.root;
	}

	public void setIsRoot(boolean rtarg) {
		this.root = rtarg;
	}

	public void setSrcSeq(int seqin) {
		this.srcseq = seqin;
	}

	public void setDestSeq(int seqin) {
		this.destseq = seqin;
	}

	public int getSrcSeq() {
		return this.srcseq;
	}

	public int getDestSeq() {
		return this.destseq;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String arg) {
		this.nodeName = arg;
	}

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, Object> prop) {
		this.properties = prop;
	}

	// fwang ----end-------------

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String id) {
		documentID = id;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String path) {
		folderPath = path;
	}

	public String getFolderID() {
		return folderID;
	}

	public void setFolderID(String id) {
		folderID = id;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String namE) {
		documentName = namE;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String desc) {
		description = desc;
	}

	public SorcerPrincipal getPrincipal() {
		return principal;
	}

	public void setPrincipal(SorcerPrincipal gp) {
		principal = gp;
	}

	public String getAccessClass() {
		return (accessClass == null) ? PUBLIC : accessClass;
	}

	public void setAccessClass(String ac) {
		accessClass = ac;
	}

	public void isExportControlled(boolean b) {
		isExportControlled = new Boolean(b);
	}

	public boolean isExportControlled() {
		return isExportControlled.booleanValue();
	}

	public DocumentDescriptor fromString(String descriptor) {
		return null;
	}

	public String asString(Document document) {
		return null;
	}

	public String toString() {
		return folderPath + SEP + documentName;
	}

	public Object getACL() {
		return documentACL;
	}

	public void setACL(Object aclImpl) {
		documentACL = aclImpl;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		if (convertor != null)
			out.writeObject(convertor.pack(documentACL));
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		if (convertor != null)
			documentACL = convertor.unpack((Hashtable) in.readObject());
	}

}
