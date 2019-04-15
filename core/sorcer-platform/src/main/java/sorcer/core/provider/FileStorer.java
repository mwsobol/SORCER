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

package sorcer.core.provider;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.transaction.InvalidTransactionException;

import sorcer.util.DocumentDescriptor;

/**
 * The file store interface to access remote directories.
 */
public interface FileStorer {

	/**
	 * Returns a document descriptor intialized for file read operations.
	 * 
	 * @param desc
	 *            an initial document descriptor
	 * @return a document descriptor initialized for read operations
	 * @throws RemoteException
	 * @throws IOException
	 * @throws InvalidTransactionException
	 */
	public DocumentDescriptor getInputDescriptor(DocumentDescriptor desc)
			throws RemoteException, IOException, InvalidTransactionException;

	/**
	 * Returns a document descriptor intialized for file appending operations.
	 * 
	 * @param desc
	 *            an initial document descriptor
	 * @return a document descriptor initialized for append operations
	 * @throws RemoteException
	 * @throws IOException
	 * @throws InvalidTransactionException
	 */
	public DocumentDescriptor getAppendDescriptor(DocumentDescriptor desc)
			throws RemoteException, IOException, InvalidTransactionException;

	/**
	 * Returns a document descriptor intialized for file writing operations.
	 * 
	 * @param desc
	 *            an initial document descriptor
	 * @return a document descriptor initialized for write operation
	 * @throws RemoteException
	 * @throws IOException
	 * @throws InvalidTransactionException
	 */
	public DocumentDescriptor getOutputDescriptor(DocumentDescriptor desc)
			throws RemoteException, IOException, InvalidTransactionException;

	/**
	 * Lists all directories available on the file store.
	 * <p>
	 * The format is:
	 * <ul>
	 * <li># is the path separator</li>
	 * <li>/ is the separator between args</li>
	 * </ul>
	 * <p>
	 * Example: <tt>/test/test#bla
	 * </p>
	 * 
	 * @param desc
	 *            Only Principal is used.
	 * @return a list of all directories.
	 */
	public String getDirectories(DocumentDescriptor docDesc)
			throws RemoteException, IOException;

	/**
	 * Returns the content of a directory.
	 * <p>
	 * The components are separeted with a /. Only files and no directories are
	 * listed.
	 * 
	 * @param desc
	 *            DocumentDescriptor. FolderPath and Principal are user.
	 *            FolderPath uses # as a Path separator.
	 * @return
	 * @throws RemoteException
	 * @throws IOException
	 */
	public String getDirectoryContent(DocumentDescriptor desc)
			throws RemoteException, IOException;

	public String listDirectories(DocumentDescriptor desc)
			throws RemoteException, IOException;

}
