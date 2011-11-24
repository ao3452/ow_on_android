/*
 * Copyright 2006-2007 National Institute of Advanced Industrial Science
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

package ow.directory.hashmap;

import ow.directory.SingleValueDirectory;
import ow.directory.DirectoryProvider;
import ow.directory.MultiValueDirectory;

public final class VolatileMapProvider extends DirectoryProvider {
	private final static String NAME = "VolatileMap";

	public String getName() { return NAME; }

	public <K,V> SingleValueDirectory<K,V> openSingleValueDirectory(Class typeK, Class typeV, String dir, String dbName) throws Exception {
		return new SingleValueHashDirectory<K,V>(typeK, typeV, dir, dbName, null, -1);
	}

	public <K,V> MultiValueDirectory<K,V> openMultiValueDirectory(Class typeK, Class typeV, String dir, String dbName) throws Exception {
		return new MultipleValueHashDirectory<K,V>(typeK, typeV, dir, dbName, null, -1);
	}

	public void removeDirectory(String dir, String dbName) throws Exception {
		// do nothing
	}
}
