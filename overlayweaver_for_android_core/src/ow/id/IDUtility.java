/*
 * Copyright 2006-2008 National Institute of Advanced Industrial Science
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

package ow.id;

import java.io.UnsupportedEncodingException;

public final class IDUtility {
	
	public final static String ENCODING = "UTF-8";

	/**
	 * Parses an ID specified by a string or a hexadecimal number.
	 * A string is converted the corresponding ID by being hashed with SHA1.
	 */
	public static ID parseID(String arg, int size) {
		ID key = null;

		if (arg.startsWith("id:")) {
			key = ID.getID(arg.substring(3), size);
		}
		else {
			try {
				key = ID.getSHA1BasedID(arg.getBytes(ENCODING), size);
			}
			catch (UnsupportedEncodingException e) {
				// NOTREACHED
			}
		}

		return key;
	}
}
