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

package ow.messaging.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Establish an outgoing connection, pool it and return it.
 */
final class ConnectionPool {
	private final static Logger logger = Logger.getLogger("messaging");

	private final int size;
	private final Map<SocketAddress,SocketChannel> connectionMap;
	private final Random rnd;

	ConnectionPool(int size) {
		this.size = size;
		this.connectionMap = Collections.synchronizedMap(new HashMap<SocketAddress,SocketChannel>());
		this.rnd = new Random();
	}

	/**
	 * Look for a Socket connected to dest, if found remove it from the table and return it.
	 * Otherwise connect.
	 * Note that the returned Socket is possible to be already closed.
	 */
	public SocketChannel get(SocketAddress dest) throws IOException {
		SocketChannel sock = connectionMap.remove(dest);	// retrieve a Socket
		if (sock != null) {
			logger.log(Level.INFO, "A Socket found in the hash table: ", sock);
			return sock;
		}
		else {
			try {
				sock = SocketChannel.open(dest);
				logger.log(Level.INFO, "A new Socket created: " + dest);
			}
			catch (IOException e) {
				logger.log(Level.INFO, "A Socket could not be created: " + dest);
				throw e;
			}

			// put
			if (connectionMap.size() + 1 >= this.size) {
				logger.log(Level.INFO, "Connection pool is full. Remove an entry.");

				// remove an entry randomly
				int removeIdx = rnd.nextInt(this.size);
				SocketAddress removedKey = null;
				synchronized (connectionMap) {
					for (SocketAddress key: connectionMap.keySet()) {
						if (removeIdx == 0) {
							removedKey = key;
							break;
						}
						removeIdx--;
					}

					connectionMap.remove(removedKey);
				}
			}

			return sock;
		}
	}

	public void put(SocketAddress addr, SocketChannel sock) {
		synchronized (connectionMap) {
			SocketChannel existingChannel = connectionMap.remove(addr);
			if (existingChannel != null) {
				// disposes an exisiting connection
				try {
					existingChannel.close();
				}
				catch (IOException e) { /* ignore */ }
				existingChannel = null;
			}

			// put
			connectionMap.put(addr, sock);
		}
	}

	public void clear() {
		synchronized (connectionMap) {
			for (SocketChannel sock: connectionMap.values()) {
				try {
					sock.close();
				}
				catch (IOException e) {
					// ignore
				}
			}

			connectionMap.clear();
		}
	}
}
