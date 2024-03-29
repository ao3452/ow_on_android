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

package ow.dht.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;

import ow.SessionID.MessageInfo;
import ow.SessionID.SessionID;
import ow.dht.ByteArray;
import ow.dht.DHT;
import ow.dht.DHTConfiguration;
import ow.dht.ValueInfo;
import ow.directory.DirectoryFactory;
import ow.directory.DirectoryProvider;
import ow.directory.MultiValueAdapterForSingleValueDirectory;
import ow.directory.MultiValueDirectory;
import ow.directory.SingleValueDirectory;
import ow.id.ID;
import ow.id.IDAddressPair;
import ow.messaging.Message;
import ow.messaging.MessageHandler;
import ow.messaging.MessagingAddress;
import ow.messaging.Relayhandler;
import ow.messaging.Tag;
import ow.routing.RoutingAlgorithm;
import ow.routing.RoutingException;
import ow.routing.RoutingResult;
import ow.routing.RoutingService;
import ow.util.ClockJumpAwareSystem;
import ow.util.Timer;

/**
 * A churn-tolerant implementation of DHT service. This implementations adds
 * churn tolerance techniques to the basic implementation. Those techniques
 * include replication, join-time transfer, multiple get and repeated implicit
 * put.
 */
public class ChurnTolerantDHTImpl<V extends Serializable> extends BasicDHTImpl<V> {
	private final static String LOCAL_DB_NAME = "local";

	// members specific to DHT

	private MultiValueDirectory<ID, ValueInfo<V>> localDir = null; // just for
																	// reputting
	private static Timer timer = null;
	private Thread reputterThread = null;

	public ChurnTolerantDHTImpl(short applicationID, short applicationVersion, DHTConfiguration config, ID selfID /*
																												 * possibly
																												 * null
																												 */)
			throws Exception {
		super(applicationID, applicationVersion, config, selfID);
	}

	public ChurnTolerantDHTImpl(DHTConfiguration config, RoutingService routingSvc) throws Exception {
		super(config, routingSvc);
	}

	protected void init(DHTConfiguration config, RoutingService routingSvc) throws Exception {
		super.init(config, routingSvc);

		// initialize directories
		DirectoryProvider dirProvider = DirectoryFactory.getProvider(config.getDirectoryType());
		if (this.config.getDoReputOnRequester()) {
			if (config.getMultipleValuesForASingleKey()) {
				if (config.getDoExpire()) {
					this.localDir = dirProvider.openExpiringMultiValueDirectory(ID.class, ValueInfo.class,
							config.getWorkingDirectory(), LOCAL_DB_NAME, config.getDefaultTTL());
				}
				else {
					this.localDir = dirProvider.openMultiValueDirectory(ID.class, ValueInfo.class,
							config.getWorkingDirectory(), LOCAL_DB_NAME);
				}
			}
			else {
				SingleValueDirectory<ID, ValueInfo<V>> singleValueDir;
				if (config.getDoExpire()) {
					singleValueDir = dirProvider.openExpiringSingleValueDirectory(ID.class, config.getValueClass(),
							config.getWorkingDirectory(), LOCAL_DB_NAME, config.getDefaultTTL());
				}
				else {
					singleValueDir = dirProvider.openSingleValueDirectory(ID.class, config.getValueClass(),
							config.getWorkingDirectory(), LOCAL_DB_NAME);
				}
				this.localDir = new MultiValueAdapterForSingleValueDirectory<ID, ValueInfo<V>>(singleValueDir);
			}
		}

		// initialize a Reputter
		this.startReputter();
	}

	private synchronized void startReputter() {
		if (config.getReputInterval() > 0
				&& (this.config.getDoReputOnRequester() || this.config.getDoReputOnReplicas())) {
			Runnable r = new Reputter();

			if (config.getUseTimerInsteadOfThread()) {
				synchronized (BasicDHTImpl.class) {
					if (timer == null) {
						timer = new Timer("Reputting timer", true /* daemon */);
					}
				}

				timer.schedule(r, System.currentTimeMillis(), true /* isDaemon */);
			}
			else {
				this.reputterThread = new Thread(r);
				this.reputterThread.setName("Reputter on " + this.getSelfIDAddressPair().getAddress());
				this.reputterThread.setDaemon(true);
				this.reputterThread.start();
			}
		}
	}

	private synchronized void stopReputter() {
		if (this.reputterThread != null) {
			this.reputterThread.interrupt();
			this.reputterThread = null;
		}
	}

	public void clearDHTState() {
		super.clearDHTState();

		if (localDir != null) {
			synchronized (localDir) {
				localDir.clear();
			}
		}
	}

	public Set<ValueInfo<V>>[] get(ID[] keys) {
		Set<ValueInfo<V>>[] results = new Set/* <ValueInfo<V>> */[keys.length];

		RoutingResult[] routingRes = super.getRemotely(keys, results);

		// get replicas from root candidates
		int numTimesGets = config.getNumTimesGets() - 1;
		if (numTimesGets > 0) {
			Queue<IDAddressPair>[] rootCands = new Queue/* <IDAddressPair> */[keys.length];

			for (int i = 0; i < keys.length; i++) {
				if (routingRes[i] == null)
					continue;

				for (IDAddressPair p : routingRes[i].getRootCandidates()) {
					if (rootCands[i] == null) // skip the first element
						rootCands[i] = new LinkedList<IDAddressPair>();
					else
						rootCands[i].add(p);
				}
			}

			this.requestReplicas(results, keys, rootCands, numTimesGets);
		}

		return results;
	}

	private void requestReplicas(Set<ValueInfo<V>>[] resultSet, ID[] keys, Queue<IDAddressPair>[] rootCands,
			int numTimesGets) {
		// System.out.print("requestReplicas:");
		// for (ID k: keys) System.out.print(" " + k);
		// System.out.println();
		int succeed[] = new int[keys.length];

		retry: while (true) {
			Set<IDAddressPair> contactSet = new HashSet<IDAddressPair>();
			for (int i = 0; i < keys.length; i++) {
				if (rootCands[i] == null)
					continue;

				IDAddressPair p = rootCands[i].peek();
				if (p == null)
					rootCands[i] = null;
				else {
					contactSet.add(p);
				}
			}

			if (contactSet.isEmpty())
				break;

			for (IDAddressPair contact : contactSet) {
				// System.out.println("  contact: " + contact);
				List<Integer> indexList = new ArrayList<Integer>();
				for (int i = 0; i < keys.length; i++) {
					if (rootCands[i] == null)
						continue;

					if (contact.equals(rootCands[i].peek())) {
						indexList.add(i);
						rootCands[i].poll();
					}
				}

				int size = indexList.size();
				ID[] packedKeys = new ID[size];
				for (int i = 0; i < indexList.size(); i++) {
					packedKeys[i] = keys[indexList.get(i)];
				}

				Message request = DHTMessageFactory.getGetMessage(this.getSelfIDAddressPair(), packedKeys);
				Message reply = null;
				try {
					reply = sender.sendAndReceive(contact.getAddress(), request);
				}
				catch (IOException e) {
					continue retry;
				}

				if (reply.getTag() != Tag.DHT_REPLY.getNumber()) {
					logger.log(Level.WARNING,
							"Reply to a GET req is not DHT_REPLY: " + Tag.getNameByNumber(reply.getTag()) + " from "
									+ reply.getSource().getAddress());
					continue retry;
				}

				Serializable[] contents = reply.getContents();
				Set<ValueInfo<V>>[] s = (Set<ValueInfo<V>>[]) contents[0];

				for (int i = 0; i < indexList.size(); i++) {
					int index = indexList.get(i);

					if (++succeed[index] >= numTimesGets) {
						rootCands[index] = null; // clear to avoid retry
					}

					if (s[i] != null) {
						if (resultSet[index] == null)
							resultSet[index] = new HashSet<ValueInfo<V>>();
						resultSet[index].addAll(s[i]);
						// System.out.print("  key[" + i + "]");
						// for (ValueInfo<V> v: s[i]) System.out.print(" " +
						// v.getValue());
						// System.out.println();
					}
				}
			} // for (IDAddressPair contact: contactSet)
		} // while (true)
	}

	public Set<ValueInfo<V>> put(ID key, V[] values) throws IOException {
		// local
		if (localDir != null) {
			synchronized (localDir) {
				for (V v : values) {
					if (localDir.isInMemory()
							&& Runtime.getRuntime().freeMemory() < config.getRequiredFreeMemoryToPut())
						continue;

					try {
						localDir.put(key, new ValueInfo<V>(v, this.ttlForPut, this.hashedSecretForPut));
					}
					catch (Exception e) {/* ignore */
					}
				}
			}
		}

		// remote
		DHT.PutRequest<V>[] requests = new DHT.PutRequest/* <V> */[1];
		requests[0] = new DHT.PutRequest<V>(key, values);

		int numReplica, repeat;
		if (config.getRootDoesReplication()) {
			numReplica = config.getNumReplica();
			repeat = 1;
		}
		else {
			numReplica = 1;
			repeat = config.getNumReplica();
		}

		Set<ValueInfo<V>>[] ret = this.putOrRemoveRemotely(requests, false, this.ttlForPut, this.hashedSecretForPut,
				true, numReplica, repeat, false);

		if (ret[0] == null)
			throw new RoutingException();

		return ret[0];
	}

	public Set<ValueInfo<V>>[] put(DHT.PutRequest<V>[] requests) throws IOException {
		// local
		if (localDir != null) {
			synchronized (localDir) {
				for (DHT.PutRequest<V> req : requests) {
					for (V v : req.getValues()) {
						if (localDir.isInMemory()
								&& Runtime.getRuntime().freeMemory() < config.getRequiredFreeMemoryToPut())
							continue;

						try {
							localDir.put(req.getKey(), new ValueInfo<V>(v, this.ttlForPut, this.hashedSecretForPut));
						}
						catch (Exception e) {/* ignore */
						}
					}
				}
			}
		}

		// remote
		int numReplica, repeat;
		if (config.getRootDoesReplication()) {
			numReplica = config.getNumReplica();
			repeat = 1;
		}
		else {
			numReplica = 1;
			repeat = config.getNumReplica();
		}

		return this.putOrRemoveRemotely(requests, false, this.ttlForPut, this.hashedSecretForPut, true, numReplica,
				repeat, false);
	}

	public Set<ValueInfo<V>>[] remove(DHT.RemoveRequest<V>[] requests, ByteArray hashedSecret) {
		// remove locally from localDir
		if (localDir != null) {
			for (int i = 0; i < requests.length; i++) {
				DHT.RemoveRequest<V> req = requests[i];
				try {
					if (req.getValues() != null) {
						synchronized (localDir) {
							for (V v : req.getValues()) {
								localDir.remove(req.getKey(), new ValueInfo<V>(v, -1, hashedSecret));
							}
						}
					}
					else {
						Set<ValueInfo<V>> localValues;

						localValues = localDir.get(req.getKey());
						if (localValues != null) {
							for (ValueInfo<V> v : localValues) {
								ID h = null;
								try {
									h = ID.getSHA1BasedID(v.getValue().toString().getBytes(config.getValueEncoding()));
								}
								catch (UnsupportedEncodingException e) {
									// NOTREACHED
									logger.log(Level.SEVERE, "Encoding not supported: " + config.getValueEncoding());
								}

								if ((req.getValueHash() == null || h.equals(req.getValueHash()))
										&& hashedSecret.equals(v.getHashedSecret())) {
									synchronized (localDir) {
										localDir.remove(req.getKey(), v);
									}
								}
							}
						}
					}
				}
				catch (Exception e) {
					// NOTREACHED
					logger.log(Level.WARNING, "An Exception thrown by Directory#remove().", e);
				}
			} // for (int i = 0; i < requests.length; i++)
		} // if (localDir != null)

		// remote
		int numReplica, repeat;
		if (config.getRootDoesReplication()) {
			numReplica = config.getNumReplica();
			repeat = 1;
		}
		else {
			numReplica = 1;
			repeat = config.getNumReplica();
		}

		Set<ValueInfo<V>>[] results = this.putOrRemoveRemotely(requests, true, 0L, hashedSecret, true, numReplica,
				repeat, false);

		return results;
	}

	public synchronized void stop() {
		// TODO: transfer indices which this node has to other nodes

		// stop reputter daemon
		this.stopReputter();

		super.stop();

		// close directories
		if (this.localDir != null) {
			this.localDir.close();
			this.localDir = null;
		}
	}

	public synchronized void suspend() {
		// stop a daemon
		this.stopReputter();

		super.suspend();
	}

	public synchronized void resume() {
		super.resume();

		// resume a daemon
		this.startReputter();
	}

	//
	// methods specific to DHT
	//

	public Set<ID> getLocalKeys() {
		if (this.localDir == null)
			return null;

		return this.localDir.keySet();
	}

	public Set<ValueInfo<V>> getLocalValues(ID key) {
		if (this.localDir == null)
			return null;

		Set<ValueInfo<V>> ret = null;
		try {
			ret = this.localDir.get(key);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "An Exception thrown when retrieve from the localDir.", e);
			return null;
		}

		return ret;
	}

	protected void prepareHandlers(RoutingService routingSvc) {
		super.prepareHandlers0(routingSvc);

		MessageHandler handler;
		handler = new PutMessageHandler();
		routingSvc.addMessageHandler(Tag.PUT.getNumber(), handler);

		handler = new RemoveMessageHandler();
		routingSvc.addMessageHandler(Tag.REMOVE.getNumber(), handler);

		// -------------------Communicate用のハンドラ
		handler = new RelayMessageHandler();
		routingSvc.addMessageHandler(Tag.RELAY.getNumber(), handler);
		// ---------------------------------

		// for value transfer
		handler = new MessageHandler() {
			public Message process(Message msg) {
				// get key-value pairs to be transferred to the requesting node
				Map<ID, Set<ValueInfo<V>>> valueMap = getValueLocallyToBeTransferredTo(msg.getSource().getID());

				MessagingAddress src = msg.getSource().getAddress();
				Message putValueInfoMsg = DHTMessageFactory.getPutValueInfoMessage(
						ChurnTolerantDHTImpl.this.getSelfIDAddressPair(), valueMap);

				try {
					sender.send(src, putValueInfoMsg);
				}
				catch (IOException e) {
					logger.log(Level.WARNING, "failed to send a PUT_VALUEINFO msg: " + src);
				}

				return null;
			}
		};
		routingSvc.addMessageHandler(Tag.REQ_TRANSFER.getNumber(), handler);

		handler = new PutValueInfoMessageHandler();
		routingSvc.addMessageHandler(Tag.PUT_VALUEINFO.getNumber(), handler);
	}

	protected class PutMessageHandler extends BasicDHTImpl.PutMessageHandler {
		public Message process(Message msg) {
			// put locally
			Message resultMsg = super.process(msg);

			Serializable[] contents = msg.getContents();

			int numReplica = (Integer) contents[3];

			Set<ValueInfo<V>>[] ret = (Set<ValueInfo<V>>[]) (resultMsg.getContents()[0]);

			// put remotely
			if (numReplica > 1) {
				final DHT.PutRequest<V>[] requests = (DHT.PutRequest<V>[]) contents[0];
				long ttl = (Long) contents[1];
				final ByteArray hashedSecret = (ByteArray) contents[2];

				Set<ValueInfo<V>>[] existedValue = putOrRemoveRemotely(requests, false, ttl, hashedSecret, false, 1,
						numReplica - 1, true);

				if (existedValue != null) {
					for (int i = 0; i < requests.length; i++) {
						Set<ValueInfo<V>> s = existedValue[i];
						if (s != null) {
							if (ret[i] == null)
								ret[i] = new HashSet<ValueInfo<V>>();
							ret[i].addAll(s);
						}
					}
				}
			} // if (numReplica > 1)

			return resultMsg;
		}
	}

	private class RemoveMessageHandler extends BasicDHTImpl.RemoveMessageHandler {
		public Message process(Message msg) {
			// remove locally
			Message resultMsg = super.process(msg);

			Serializable[] contents = msg.getContents();

			int numReplica = (Integer) contents[2];

			Set<ValueInfo<V>>[] ret = (Set<ValueInfo<V>>[]) (resultMsg.getContents()[0]);

			// remove remotely
			if (numReplica > 1) {
				DHT.RemoveRequest<V>[] requests = (DHT.RemoveRequest<V>[]) contents[0];
				ByteArray hashedSecret = (ByteArray) contents[1];

				Set<ValueInfo<V>>[] existedValue = putOrRemoveRemotely(requests, true, 0L, hashedSecret, false, 1,
						numReplica - 1, true);

				if (existedValue != null) {
					for (int i = 0; i < requests.length; i++) {
						Set<ValueInfo<V>> s = existedValue[i];
						if (s != null) {
							if (ret[i] == null)
								ret[i] = new HashSet<ValueInfo<V>>();
							ret[i].addAll(s);
						}
					}
				}
			} // if (numReplica > 1)

			return resultMsg;
		}
	}

	private class PutValueInfoMessageHandler implements MessageHandler {
		public Message process(Message msg) {
			Serializable[] contents = msg.getContents();

			Map<ID, Set<ValueInfo<V>>> valueMap = (Map<ID, Set<ValueInfo<V>>>) contents[0];

			if (valueMap != null) {
				for (Map.Entry<ID, Set<ValueInfo<V>>> entry : valueMap.entrySet()) {
					/*
					 * System.out.println("PUT_VALUEINFO:");
					 * System.out.println("  from: " +
					 * msg.getSource().getAddress());
					 * System.out.println("  to  : " + getSelfIDAddressPair());
					 * System.out.println("  key : " + entry.getKey());
					 */
					ID key = entry.getKey();
					Set<ValueInfo<V>> valSet = entry.getValue();
					for (ValueInfo<V> val : valSet) {
						if (globalDir.isInMemory()
								&& Runtime.getRuntime().freeMemory() < config.getRequiredFreeMemoryToPut())
							continue;

						try {
							synchronized (globalDir) {
								globalDir.put(key, val, val.getTTL());
							}
						}
						catch (Exception e) { /* ignore */
						}
					}
				} // for
			} // if (valueMap != null)

			return null;
		}
	}

	// for value transfer
	private Map<ID, Set<ValueInfo<V>>> getValueLocallyToBeTransferredTo(ID otherID) {
		ID selfID = this.getSelfIDAddressPair().getID();
		RoutingAlgorithm algo = this.routingSvc.getRoutingAlgorithm();

		Map<ID, Set<ValueInfo<V>>> results = null;

		// System.out.println("joining node: " + otherID);
		ID[] keys = null;
		synchronized (globalDir) {
			Set<ID> keySet = globalDir.keySet();
			if (keySet != null) {
				keys = new ID[keySet.size()];
				keySet.toArray(keys);
			}
		}

		for (ID k : keys) {
			// System.out.println("  key: " + k);
			IDAddressPair[] betterRoot = algo.rootCandidates(k, config.getNumReplica() + 1 /*
																							 * means
																							 * the
																							 * joining
																							 * node
																							 */);
			if (betterRoot != null && betterRoot.length > 0) {
				for (IDAddressPair p : betterRoot) {
					if (otherID.equals(p.getID())) {
						// System.out.println("    -> transfer.");
						try {
							Set<ValueInfo<V>> s = globalDir.get(k);

							if (s != null) {
								if (results == null)
									results = new HashMap<ID, Set<ValueInfo<V>>>();

								results.put(k, s);
							}
						}
						catch (Exception e) { /* ignore */
						}
					}
					else if (selfID.equals(p.getID())) {
						break;
					}
				}
			}
		}

		return results;
	}

	private class Reputter implements Runnable {
		private final Random rnd = new Random();
		private final boolean reputOnRequester;

		public Reputter() {
			this.reputOnRequester = config.getDoReputOnRequester();
		}

		public void run() {
			logger.log(Level.INFO, "Reputter woke up.");

			try {
				// initial sleep
				if (!config.getUseTimerInsteadOfThread()) {
					ClockJumpAwareSystem.sleep((long) (config.getReputInterval() * 0.5));
				}

				while (true) {
					if (stopped || suspended)
						break;

					MultiValueDirectory<ID, ValueInfo<V>> dir;

					if (this.reputOnRequester)
						dir = localDir;
					else
						dir = globalDir;

					// reput values
					ID[] keys = null;
					synchronized (dir) {
						Set<ID> keySet = dir.keySet();
						if (keySet != null && keySet.size() > 0) {
							keys = new ID[keySet.size()];
							keySet.toArray(keys);
						}
					}

					if (keys != null) {
						for (ID key : keys) {
							Set<ValueInfo<V>> valueSet = getValueLocally(key, dir);
							if (valueSet == null)
								continue;

							Set<ValueInfo.Attributes> attrSet = new HashSet<ValueInfo.Attributes>();
							for (ValueInfo<V> v : valueSet) {
								attrSet.add(v.getAttributes());
							}

							for (ValueInfo.Attributes attr : attrSet) {
								Set<V> vSet = new HashSet<V>();
								for (ValueInfo<V> v : valueSet) {
									if (attr.equals(v.getAttributes())) {
										vSet.add(v.getValue());
									}
								}
								V[] values = (V[]) new Serializable[vSet.size()];
								vSet.toArray(values);

								DHT.PutRequest<V>[] reqs = new DHT.PutRequest/*
																			 * <V
																			 * >
																			 */[1];
								reqs[0] = new DHT.PutRequest<V>(key, values);

								int numReplica, repeat;
								if (config.getRootDoesReplication()) {
									numReplica = config.getNumReplica();
									repeat = 1;
								}
								else {
									numReplica = 1;
									repeat = config.getNumReplica();
								}

								Set<ValueInfo<V>>[] ret = putOrRemoveRemotely(reqs, false, attr.getTTL(),
										attr.getHashedSecret(), false, numReplica, repeat, false);

								for (int i = 0; i < reqs.length; i++) {
									if (ret[i] == null) {
										logger.log(Level.WARNING, "put() failed: " + reqs[i].getKey());
									}
								}
							}
						}
					}

					// sleep
					double playRatio = config.getReputIntervalPlayRatio();
					double intervalRatio = 1.0 - playRatio + (playRatio * 2.0 * rnd.nextDouble());
					long sleepPeriod = (long) (config.getReputInterval() * intervalRatio);

					if (config.getUseTimerInsteadOfThread()) {
						timer.schedule(this, System.currentTimeMillis() + sleepPeriod, true /* isDaemon */);
						return;
					}
					else {
						ClockJumpAwareSystem.sleep((long) (sleepPeriod));
					}
				} // while (true)
			}
			catch (InterruptedException e) {
				logger.log(Level.WARNING, "Reputter interrupted and die.", e);
			}
		}
	}
}
