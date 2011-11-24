/*
 * Copyright 2006-2007,2009 National Institute of Advanced Industrial Science
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

package ow.messaging;

import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//import ow.tool.emulator.RemoteControlPipeTable;

/**
 * The factory which provides a messaging provider.
 */
public class MessagingFactory {
	private final static Logger logger = Logger.getLogger("messaging");

	private final static Class/*<MessagingProvider>*/[] PROVIDERS = {
		ow.messaging.udp.UDPMessagingProvider.class,				// "UDP"
		ow.messaging.tcp.TCPMessagingProvider.class,				// "TCP"
		ow.messaging.emulator.EmuMessagingProvider.class,		// "Emulator"
	//	ow.messaging.distemulator.DEmuMessagingProvider.class	// "DistributedEmulator"
	};

	public final static String EMULATOR_PROVIDER_NAME = "Emulator";
	public final static String DISTRIBUTED_EMULATOR_PROVIDER_NAME = "DistributedEmulator";
	private static boolean FORCE_EMULATOR = false;
	private static boolean FORCE_DISTRIBUTED_EMULATOR = false;
	public static int INITIAL_EMULATOR_HOST_ID = 0;
	//public static RemoteControlPipeTable HOST_TABLE_FOR_DIST_EMULATOR = null;

	private static HashMap<String,MessagingProvider> providerTable;

	public final static String DEFAULT_POOLED_THREAD_NAME = "A pooled messaging thread";
	private static ExecutorService nonDaemonThreadPool;
	protected static ExecutorService daemonThreadPool;

	static {
		// instantiate a thread pool
		nonDaemonThreadPool = Executors.newCachedThreadPool(new NonDaemonThreadFactory());
		daemonThreadPool = Executors.newCachedThreadPool(new DaemonThreadFactory());

		ThreadPoolExecutor ex;
		ex = (ThreadPoolExecutor)nonDaemonThreadPool;
		ex.setCorePoolSize(0);
		ex.setKeepAliveTime(5L, TimeUnit.SECONDS);

		ex = (ThreadPoolExecutor)daemonThreadPool;
		ex.setCorePoolSize(0);
		ex.setKeepAliveTime(5L, TimeUnit.SECONDS);

		// register providers
		providerTable = new HashMap<String,MessagingProvider>();
		for (Class clazz: PROVIDERS) {
			Object o;
			try {
				o = clazz.newInstance();
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Could not instantiate an object of the class: " + clazz, e);
				continue;
			}

			if (o instanceof MessagingProvider) {
				MessagingProvider provider = (MessagingProvider)o;

				// set thread pool to provider
				provider.setThreadPool(nonDaemonThreadPool);

				providerTable.put(provider.getName(), (MessagingProvider)provider);
			}
		}
	}

	/**
	 * Return a messaging provider associated to the given name.
	 * The name should be one of the following names: "TCP", "UDP" or "Emulator".
	 * There is an utility class {@link Signature Signature} to generate a signature.
	 *
	 * @param messagingType name of a messaging provider.
	 * @param messageSignature signature embedded to every message. 
	 * @return a message provider.
	 * @throws NoSuchProviderException
	 */
	public static MessagingProvider getProvider(String messagingType, byte[] messageSignature) throws NoSuchProviderException {
		return getProvider(messagingType, messageSignature, false);
	}

	public static MessagingProvider getProvider(String messagingType, byte[] messageSignature, boolean notForced) throws NoSuchProviderException {
		if (!notForced) {
			if (FORCE_DISTRIBUTED_EMULATOR) {
				messagingType = DISTRIBUTED_EMULATOR_PROVIDER_NAME;
			}
			else if (FORCE_EMULATOR) {
				messagingType = EMULATOR_PROVIDER_NAME;
			}
		}

		MessagingProvider provider = providerTable.get(messagingType);
		if (provider == null) {
			throw new NoSuchProviderException("No such provider: " + messagingType);
		}

		// A workaround for Emulator
		MessagingProvider substitutedProvider = provider.substitute();
		if (substitutedProvider != null) {
			provider = substitutedProvider;
		}

		// set message signature to provider
		if (messageSignature != null) {
			provider.setMessageSignature(messageSignature);
		}

		return provider;
	}

	/**
	 * Enforce returning the emulator provider on the factory.
	 *
	 * @param initialEmulatorHostID the first ID of virtual host names (emuXX) in an emulator.
	 */
	public static void forceEmulator(int initialEmulatorHostID) {
		FORCE_EMULATOR = true;
		INITIAL_EMULATOR_HOST_ID = initialEmulatorHostID;
	}

	/**
	 * Enforce returning the distributed emulator provider on the factory.
	 *
	 * @param initialEmulatorHostID the first ID of virtual host names (emuXX) in an emulator.
	 */
/*	public static void forceDistributedEmulator(int initialEmulatorHostID, RemoteControlPipeTable hostTable) {
		FORCE_DISTRIBUTED_EMULATOR = true;
		INITIAL_EMULATOR_HOST_ID = initialEmulatorHostID;
		HOST_TABLE_FOR_DIST_EMULATOR = hostTable;
	}
*/
	private final static class NonDaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName(DEFAULT_POOLED_THREAD_NAME);
			t.setDaemon(false);

			return t;
		}
	}

	private final static class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName(DEFAULT_POOLED_THREAD_NAME);
			t.setDaemon(true);

			return t;
		}
	}
}
