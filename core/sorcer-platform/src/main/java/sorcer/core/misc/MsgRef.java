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

package sorcer.core.misc;

import java.io.Serializable;

import net.jini.id.Uuid;

public class MsgRef implements Serializable {

	private Uuid taskID;

	private Uuid jobID;

	private String msgID;

	private String ownerID;

	private Integer msgType;

	private String source;

	private String msgData;

	private Uuid sessionID;

	public MsgRef(Uuid taskId_p, Uuid JobId_p, String ownerId_p,
			int MsgType_p, String source_p, String msgData_p, Uuid sessionID_p) {
		taskID = taskId_p;
		if (JobId_p != null)
			jobID = JobId_p;
		else
			jobID = null;

		// msgID = new String(MsgId_p);
		if (ownerId_p != null)
			ownerID = ownerId_p;
		else
			ownerID = null;
		msgType = new Integer(MsgType_p);
		source = source_p;
		msgData = msgData_p;
		sessionID = sessionID_p;
	}

	public MsgRef(Uuid tID, int mType, String src, String mData, Uuid sessID) {

		taskID = tID;
		jobID = null;
		ownerID = null;
		msgType = new Integer(mType);
		source = src;
		msgData = mData;
		sessionID = sessID;
	}

	public void setMsgID(String id) {
		msgID = id;
	}

	public void setJobID(Uuid id) {
		jobID = id;
	}

	public void setOwnerID(String id) {
		ownerID = id;
	}

	public Uuid getTaskID() {
		return taskID;
	}

	public Uuid getJobID() {
		return jobID;
	}

	public String getMsgID() {
		return msgID;
	}

	public String getOwnerID() {
		return ownerID;
	}

	public Integer getMsgType() {
		return msgType;
	}

	public String getSource() {
		return source;
	}

	public String getMsgData() {
		return msgData;
	}

	public Uuid getSessionID() {
		return sessionID;
	}

	public void sesSessionID(Uuid sid) {
		sessionID = sid;
	}

}
