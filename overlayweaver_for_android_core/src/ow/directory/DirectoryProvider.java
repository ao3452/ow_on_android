/*
 * Copyright 2006-2009 National Institute of Advanced Industrial Science
 * and Technology (AIST), and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ow.directory;

import ow.directory.expiration.ExpiringMultiValueDirectory;
import ow.directory.expiration.ExpiringSingleValueDirectory;
import ow.directory.expiration.ExpiringValue;

/**
 * The interface which a directory provider for various backend databases implements.
 */
public abstract class DirectoryProvider {
	/**
	 * Return the name of the provider.
	 */
	public abstract String getName();

	/**
	 * Create a directory and return it.
	 */
	public abstract <K,V> SingleValueDirectory<K,V> openSingleValueDirectory(Class typeK, Class typeV, String workingDir, String dbName) throws Exception;

	/**
	 * Create a directory and return it.
	 * The directory holds multiple values associated with the same key.
	 */
	public abstract <K,V> MultiValueDirectory<K,V> openMultiValueDirectory(Class typeK, Class typeV, String workingDir, String dbName) throws Exception;

	/**
	 * Create a directory in which key and value pairs expire.
	 *
	 * @param defaultExpirationTime default expiration time (msec).
	 */
	public <K,V> ExpiringSingleValueDirectory<K,V> openExpiringSingleValueDirectory(Class typeK, Class typeV, String workingDir, String dbName,
			long defaultExpirationTime) throws Exception {
		SingleValueDirectory<K,ExpiringValue<V>> directory =
			this.openSingleValueDirectory(typeK, ExpiringValue/*<V>*/.class, workingDir, dbName);
				// to be written as ExpiringValue<V>.class

		return new ExpiringSingleValueDirectory(directory, defaultExpirationTime);
			// TODO
			// should be parameterized.
	}

	/**
	 * Create a directory which hold multiple values associated with the same key.
	 * Key and value pairs expire in this directory.
	 *
	 * @param defaultExpirationTime default expiration time (msec).
	 */
	public <K,V> ExpiringMultiValueDirectory<K,V> openExpiringMultiValueDirectory(Class typeK, Class typeV, String workingDir, String dbName,
			long defaultExpirationTime) throws Exception {
		MultiValueDirectory<K,ExpiringValue<V>> directory =
			this.openMultiValueDirectory(typeK, ExpiringValue/*<V>*/.class, workingDir, dbName);
				// to be written as MultiValueExpiringDirectory<K,V>.ExpiringValue<V>.class

		return new ExpiringMultiValueDirectory(directory, defaultExpirationTime);
			// TODO
			// should be parameterized.
	}

	public abstract void removeDirectory(String dir, String dbName) throws Exception;
}
