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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runtime;

import javax.crypto.BadPaddingException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import edu.cityu.ibe.PublicKeyGenerator;
import edu.cityu.util.Point;

import mypackage.*;

import ow.dht.ByteArray;
import ow.dht.DHT;
import ow.dht.DHTConfiguration;
import ow.dht.DHTConfiguration.commFlag;

import ow.dht.ValueInfo;
import ow.dht.memcached.Memcached;
import ow.directory.DirectoryFactory;
import ow.directory.DirectoryProvider;
import ow.directory.MultiValueAdapterForSingleValueDirectory;
import ow.directory.MultiValueDirectory;
import ow.directory.SingleValueDirectory;
import ow.id.ID;
import ow.id.IDAddressPair;
import ow.messaging.Message;
import ow.messaging.MessageHandler;
import ow.messaging.MessageReceiver;
import ow.messaging.MessageSender;
import ow.messaging.MessagingAddress;
import ow.messaging.MessagingConfiguration;
import ow.messaging.MessagingFactory;
import ow.messaging.MessagingProvider;
import ow.messaging.Signature;
import ow.messaging.Tag;
import ow.routing.CallbackOnRoute;
import ow.routing.CallbackResultFilter;
import ow.routing.RoutingAlgorithmConfiguration;
import ow.routing.RoutingAlgorithmFactory;
import ow.routing.RoutingAlgorithmProvider;
import ow.routing.RoutingException;
import ow.routing.RoutingResult;
import ow.routing.RoutingService;
import ow.routing.RoutingServiceFactory;
import ow.routing.RoutingServiceProvider;

/**
 * A basic implementation of DHT service over a routing service.
 */
public class BasicDHTImpl<V extends Serializable> implements DHT<V> {
	final static Logger logger = Logger.getLogger("dht");

	private final static String GLOBAL_DB_NAME = "global";

	// members common to higher level services (DHT and Mcast)

	protected DHTConfiguration config;
	private RoutingAlgorithmConfiguration algoConfig;

	protected RoutingService routingSvc;
	protected MessageSender sender;

	protected boolean stopped = false;
	protected boolean suspended = false;

	// members specific to DHT

	protected MultiValueDirectory<ID, ValueInfo<V>> globalDir;

	// members for put operations
	protected ByteArray hashedSecretForPut;
	protected long ttlForPut;

	/*
	 * Nozomu Hatta 匿名通信（多段中継）ために追加した変数
	 */
	// 中継ノード用
	private Map<Integer, RelayProcessSet> relayProcessMap; // 受け取るヘッダとヘッダを受け取ったときに行う処理のまとまりを保持するマップ
	// 送信ノード用
	private Map<Integer, AnonymousRouteInfo> senderProcessMap; //
	private Map<ID, Integer> senderMap; // 送信先ID(String)と対応する

	private Point myPrivateKey;
	private Point MPK;

	// need mutex
	long recv_time;

	public BasicDHTImpl(short applicationID, short applicationVersion, DHTConfiguration config, ID selfID /*
																										 * possibly
																										 * null
																										 */)
			throws Exception {
		// obtain messaging service
		byte[] messageSignature = Signature
				.getSignature(RoutingServiceFactory.getRoutingStyleID(config.getRoutingStyle()),
						RoutingAlgorithmFactory.getAlgorithmID(config.getRoutingAlgorithm()), applicationID,
						applicationVersion);

		MessagingProvider msgProvider = MessagingFactory.getProvider(config.getMessagingTransport(), messageSignature);
		if (config.getSelfAddress() != null) {
			msgProvider.setSelfAddress(config.getSelfAddress());
		}

		MessagingConfiguration msgConfig = msgProvider.getDefaultConfiguration();
		msgConfig.setDoUPnPNATTraversal(config.getDoUPnPNATTraversal());

		MessageReceiver receiver = msgProvider.getReceiver(msgConfig, config.getSelfPort(), config.getSelfPortRange());
		config.setSelfPort(receiver.getPort()); // correct config

		// obtain routing service
		RoutingAlgorithmProvider algoProvider = RoutingAlgorithmFactory.getProvider(config.getRoutingAlgorithm());
		this.algoConfig = algoProvider.getDefaultConfiguration();

		RoutingServiceProvider svcProvider = RoutingServiceFactory.getProvider(config.getRoutingStyle());
		RoutingService routingSvc = svcProvider.getService(svcProvider.getDefaultConfiguration(), msgProvider,
				receiver, algoProvider, this.algoConfig, selfID);

		// instantiate a RoutingAlgorithm in the routing service
		algoProvider.initializeAlgorithmInstance(this.algoConfig, routingSvc);
		this.init(config, routingSvc);

		SetUp();
	}

	public BasicDHTImpl(DHTConfiguration config, RoutingService routingSvc) throws Exception {
		this.init(config, routingSvc);
	}

	protected void init(DHTConfiguration config, RoutingService routingSvc) throws Exception {

		this.config = config;
		this.routingSvc = routingSvc;
		this.sender = this.routingSvc.getMessageSender();

		this.hashedSecretForPut = null;
		this.ttlForPut = config.getDefaultTTL();

		// prepare working directory
		File workingDirFile = new File(config.getWorkingDirectory());
		workingDirFile.mkdirs();

		// initialize directories
		DirectoryProvider dirProvider = DirectoryFactory.getProvider(config.getDirectoryType());
		if (config.getMultipleValuesForASingleKey()) {
			if (config.getDoExpire()) {
				this.globalDir = dirProvider.openExpiringMultiValueDirectory(ID.class, config.getValueClass(),
						config.getWorkingDirectory(), GLOBAL_DB_NAME, config.getDefaultTTL());
			}
			else {
				this.globalDir = dirProvider.openMultiValueDirectory(ID.class, config.getValueClass(),
						config.getWorkingDirectory(), GLOBAL_DB_NAME);
			}
		}
		else {
			SingleValueDirectory<ID, ValueInfo<V>> singleValueDir;
			if (config.getDoExpire()) {
				singleValueDir = dirProvider.openExpiringSingleValueDirectory(ID.class, config.getValueClass(),
						config.getWorkingDirectory(), GLOBAL_DB_NAME, config.getDefaultTTL());
			}
			else {
				singleValueDir = dirProvider.openSingleValueDirectory(ID.class, config.getValueClass(),
						config.getWorkingDirectory(), GLOBAL_DB_NAME);
			}
			this.globalDir = new MultiValueAdapterForSingleValueDirectory<ID, ValueInfo<V>>(singleValueDir);
		}

		// initialize message handlers and callbacks
		prepareHandlers(this.routingSvc);
		prepareCallbacks(this.routingSvc);

		/*
		 * 追加じゃぁぁぁああああああああああああああ な部分 全てのマップをスレッドセーフにする
		 */
		this.relayProcessMap = Collections.synchronizedMap(new HashMap<Integer, RelayProcessSet>());
		this.senderProcessMap = Collections.synchronizedMap(new HashMap<Integer, AnonymousRouteInfo>());
		this.senderMap = Collections.synchronizedMap(new HashMap<ID, Integer>());
	}

	public MessagingAddress joinOverlay(String hostAndPort, int defaultPort) throws UnknownHostException,
			RoutingException {
		MessagingAddress addr = this.routingSvc.getMessagingProvider().getMessagingAddress(hostAndPort, defaultPort);
		this.joinOverlay(addr);

		return addr;
	}

	public MessagingAddress joinOverlay(String hostAndPort) throws UnknownHostException, RoutingException {
		MessagingAddress addr = this.routingSvc.getMessagingProvider().getMessagingAddress(hostAndPort);
		this.joinOverlay(addr);

		return addr;
	}

	private void joinOverlay(MessagingAddress addr) throws RoutingException {
		logger.log(Level.INFO, "DHTImpl#joinOverlay: " + addr);

		IDAddressPair selfIDAddress = this.getSelfIDAddressPair();
		RoutingResult routingRes = this.routingSvc.join(addr); // throws
		// RoutingException

		this.lastKey = selfIDAddress.getID();
		this.lastRoutingResult = routingRes.stripRoutingContext();

		// value transfer
		int nodeCount = config.getNumNodesAskedToTransfer();
		IDAddressPair[] rootCands = routingRes.getRootCandidates();

		if (nodeCount > 0 && rootCands != null) {
			int i = 0;

			while (nodeCount > 0 && i < rootCands.length) {
				IDAddressPair transferringNode = rootCands[i++];
				if (this.getSelfIDAddressPair().equals(transferringNode))
					continue;

				Message request = DHTMessageFactory.getReqTransferMessage(selfIDAddress);
				try {
					sender.send(transferringNode.getAddress(), request);
				}
				catch (IOException e) {
					logger.log(Level.WARNING, "failed to send: " + transferringNode.getAddress());
				}

				nodeCount--;
			}
		}
	}

	public void clearRoutingTable() {
		this.routingSvc.leave();

		this.lastKey = null;
		this.lastRoutingResult = null;
	}

	public void clearDHTState() {
		synchronized (this.globalDir) {
			globalDir.clear();
		}
	}

	public Set<ValueInfo<V>> get(ID key) throws RoutingException {
		ID[] keys = { key };

		Set<ValueInfo<V>>[] results = new Set/* <ValueInfo<V>> */[keys.length];

		RoutingResult[] routingRes = this.getRemotely(keys, results);

		if (routingRes[0] == null)
			throw new RoutingException();

		return results[0];
	}

	public Set<ValueInfo<V>>[] get(ID[] keys) {
		Set<ValueInfo<V>>[] results = new Set/* <ValueInfo<V>> */[keys.length];

		RoutingResult[] routingRes = this.getRemotely(keys, results);

		return results;
	}

	protected RoutingResult[] getRemotely(ID[] keys, Set<ValueInfo<V>>[] results) {
		Serializable[][] args = new Serializable[keys.length][1];
		for (int i = 0; i < keys.length; i++) {
			args[i][0] = keys[i];
		}

		Serializable[][] callbackResultContainer = new Serializable[keys.length][1];

		// get from the root node by RoutingService#invokeCallbacksOnRoute()
		RoutingResult[] routingRes = this.routingSvc.invokeCallbacksOnRoute(keys,
				config.getNumTimesGets() + config.getNumSpareRootCandidates(), callbackResultContainer, null, -1, args);

		this.preserveRoute(keys, routingRes);

		for (int i = 0; i < keys.length; i++) {
			if (routingRes[i] == null)
				continue;

			if (callbackResultContainer[i] != null)
				results[i] = (Set<ValueInfo<V>>) callbackResultContainer[i][0];

			if (results[i] == null)
				results[i] = new HashSet<ValueInfo<V>>();
			// routing succeeded and results[i] should not be null.
		}

		return routingRes;
	}

	public Set<ValueInfo<V>> put(ID key, V value) throws IOException {
		V[] values = (V[]) new Serializable[1];
		values[0] = value;

		return this.put(key, values);
	}

	public Set<ValueInfo<V>> put(ID key, V[] values) throws IOException {
		DHT.PutRequest<V>[] req = new DHT.PutRequest/* <V> */[1];
		req[0] = new DHT.PutRequest<V>(key, values);

		Set<ValueInfo<V>>[] ret = this.putOrRemoveRemotely(req, false, this.ttlForPut, this.hashedSecretForPut, true);

		if (req[0] == null)
			throw new RoutingException();

		return ret[0];
	}

	public Set<ValueInfo<V>>[] put(DHT.PutRequest<V>[] requests) throws IOException {
		return putOrRemoveRemotely(requests, false, this.ttlForPut, this.hashedSecretForPut, true);
	}

	public Set<ValueInfo<V>> remove(ID key, V[] values, ByteArray hashedSecret) throws RoutingException {
		return remove(key, values, null, hashedSecret);
	}

	public Set<ValueInfo<V>> remove(ID key, ID[] valueHash, ByteArray hashedSecret) throws RoutingException {
		return remove(key, null, valueHash, hashedSecret);
	}

	private Set<ValueInfo<V>> remove(ID key, V[] values, ID[] valueHash, ByteArray hashedSecret)
			throws RoutingException {
		DHT.RemoveRequest<V>[] req = new DHT.RemoveRequest/* <V> */[1];
		if (values != null)
			req[0] = new DHT.RemoveRequest<V>(key, values);
		else
			req[0] = new DHT.RemoveRequest<V>(key, valueHash);

		Set<ValueInfo<V>>[] ret = this.remove(req, hashedSecret);

		if (req[0] == null)
			throw new RoutingException();

		return ret[0];
	}

	public Set<ValueInfo<V>> remove(ID key, ByteArray hashedSecret) throws RoutingException {
		DHT.RemoveRequest<V>[] req = new DHT.RemoveRequest/* <V> */[1];
		req[0] = new DHT.RemoveRequest<V>(key);

		Set<ValueInfo<V>>[] ret = this.remove(req, hashedSecret);

		if (req[0] == null)
			throw new RoutingException();

		return ret[0];
	}

	public Set<ValueInfo<V>>[] remove(DHT.RemoveRequest<V>[] requests, ByteArray hashedSecret) {
		return putOrRemoveRemotely(requests, true, 0L, hashedSecret, true);
	}

	// wrapper to supplement arguments
	private Set<ValueInfo<V>>[] putOrRemoveRemotely(DHT.PutRequest<V>[] requests, boolean doesRemove, long ttl,
			ByteArray hashedSecret, boolean preserveRoute) {
		return this.putOrRemoveRemotely(requests, doesRemove, ttl, hashedSecret, preserveRoute, 1, 1, false); // for
																												// replication
																												// (ChurnTolerantDHTImpl)
	}

	// Note:
	// This putOrRemoveRemotely method supports ChurnTolerantDHTImpl and
	// MemcachedImpl.
	// Such supports should be separated from this BasicDHTImpl class.
	protected Set<ValueInfo<V>>[] putOrRemoveRemotely(DHT.PutRequest<V>[] requests, boolean doesRemove, long ttl,
			ByteArray hashedSecret, boolean preserveRoute,
			/* for replication: */int numReplica, int repeat, boolean excludeSelf) {
		// for memcached
		boolean withCondition = (requests[0] instanceof Memcached.PutRequest);

		Set<ValueInfo<V>>[] results = new Set/* <ValueInfo<V>> */[requests.length];

		int numRootCands = repeat + config.getNumSpareRootCandidates();

		ID[] keys = new ID[requests.length];
		for (int i = 0; i < requests.length; i++) {
			keys[i] = requests[i].getKey();
		}

		RoutingResult[] routingRes = this.routingSvc.routeToRootNode(keys, numRootCands);

		Queue<IDAddressPair>[] rootCands = new Queue/* <IDAddressPair> */[requests.length];
		for (int i = 0; i < requests.length; i++) {
			if (routingRes[i] != null) {
				rootCands[i] = new LinkedList<IDAddressPair>();
				for (IDAddressPair p : routingRes[i].getRootCandidates()) {
					rootCands[i].offer(p);
				}
			}
		}

		int[] succeed = new int[requests.length];

		retryPut: while (true) {
			Set<MessagingAddress> targetSet = new HashSet<MessagingAddress>();
			for (int i = 0; i < rootCands.length; i++) {
				if (rootCands[i] == null) {
					continue;
				}

				IDAddressPair p = null;

				do {
					p = rootCands[i].peek();
					if (excludeSelf && getSelfIDAddressPair().equals(p)) {
						rootCands[i].poll();
						continue;
					}

					break;
				} while (true);

				if (p != null) {
					targetSet.add(p.getAddress());
				}
			}

			if (targetSet.isEmpty())
				break;

			for (MessagingAddress target : targetSet) {
				List<Integer> indexList = new ArrayList<Integer>();
				for (int i = 0; i < rootCands.length; i++) {
					if (rootCands[i] != null) {
						IDAddressPair p = rootCands[i].peek();
						if (p != null && target.equals(p.getAddress())) {
							rootCands[i].poll();
							indexList.add(i);
						}
					}
				}

				int size = indexList.size();
				DHT.PutRequest<V>[] packedRequests = (doesRemove ? new DHT.RemoveRequest/*
																						 * <
																						 * V
																						 * >
																						 */[size]
						: new DHT.PutRequest/* <V> */[size]);
				ID[] packedKeys = new ID[size];
				Serializable[][] packedValues = new Serializable[size][];
				for (int i = 0; i < size; i++) {
					packedRequests[i] = requests[indexList.get(i)];
				}

				Message request = null;
				if (!doesRemove) {
					if (!withCondition) {
						request = DHTMessageFactory.getPutMessage(this.getSelfIDAddressPair(), requests, ttl,
								hashedSecret, numReplica);
					}
					else { // for memcached
						request = ow.dht.memcached.impl.MemcachedMessageFactory.getPutOnConditionMessage(
								this.getSelfIDAddressPair(), (Memcached.PutRequest[]) requests, ttl, hashedSecret,
								numReplica);
					}
				}
				else {
					request = DHTMessageFactory.getRemoveMessage(getSelfIDAddressPair(),
							(DHT.RemoveRequest[]) requests, hashedSecret, numReplica);
				}

				Message reply;
				try {
					reply = this.requestPutOrRemove(target, request);
				}
				catch (IOException e) {
					continue retryPut;
				}

				Serializable[] contents = reply.getContents();
				Set<ValueInfo<V>>[] existedValues = (Set<ValueInfo<V>>[]) contents[0];
				if (existedValues != null) {
					for (int i = 0; i < indexList.size(); i++) {
						results[indexList.get(i)] = existedValues[i];
					}
				}

				for (int index : indexList) {
					if (++succeed[index] >= repeat) {
						rootCands[index] = null;
					}
				}
			} // for (MessagingAddress target: targetSet)
		} // while (true)

		// null in requests indicates that routing failure
		for (int i = 0; i < requests.length; i++) {
			if (routingRes[i] == null)
				requests[i] = null;
		}

		// null in results indicates that not put request succeeded
		for (int i = 0; i < requests.length; i++) {
			if (succeed[i] == 0) { // can be succeed[i] < repeat
				routingRes[i] = null;
				results[i] = null;
			}
		}

		if (preserveRoute)
			this.preserveRoute(keys, routingRes);

		return results;
	}

	private Message requestPutOrRemove(MessagingAddress target, Message request) throws IOException {
		Message reply = null;

		try {
			reply = sender.sendAndReceive(target, request);
			// throws IOException

			if (reply.getTag() == Tag.DHT_REPLY.getNumber()) {
				logger.log(Level.INFO, "put/remove succeeded on " + target);
			}
			else {
				reply = null;
			}
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Failed to send a put/remove message to " + target, e);
			throw e;
		}

		return reply;
	}

	/**
	 * Saves the last keys and routes.
	 */
	private void preserveRoute(ID[] keys, RoutingResult[] routingRes) {
		for (int i = keys.length - 1; i >= 0; i--) {
			if (routingRes[i] != null) {
				this.lastKey = keys[i];
				this.lastRoutingResult = routingRes[i].stripRoutingContext();

				return;
			}
		}

		this.lastKey = null;
		this.lastRoutingResult = null;
	}

	public ByteArray setHashedSecretForPut(ByteArray hashedSecret) {
		ByteArray old = this.hashedSecretForPut;
		this.hashedSecretForPut = hashedSecret;
		return old;
	}

	public long setTTLForPut(long ttl) {
		long old = this.ttlForPut;
		this.ttlForPut = ttl;
		return old;
	}

	public synchronized void stop() {
		logger.log(Level.INFO, "DHT#stop() called.");

		this.stopped = true;

		// stop routing service
		if (this.routingSvc != null) {
			this.routingSvc.stop();
			this.routingSvc = null;
		}

		// close directories
		if (this.globalDir != null) {
			this.globalDir.close();
			this.globalDir = null;
		}
	}

	public synchronized void suspend() {
		this.suspended = true;

		// suspend routing service
		this.routingSvc.suspend();
	}

	public synchronized void resume() {
		this.suspended = false;

		// resume routing service
		this.routingSvc.resume();
	}

	//
	// methods specific to DHT
	//

	public Set<ID> getLocalKeys() {
		return null;
	}

	public Set<ValueInfo<V>> getLocalValues(ID key) {
		return null;
	}

	public Set<ID> getGlobalKeys() {
		return this.globalDir.keySet();
	}

	public Set<ValueInfo<V>> getGlobalValues(ID key) {
		Set<ValueInfo<V>> ret = null;

		try {
			ret = globalDir.get(key);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "An Exception thrown when retrieve from the globalDir.", e);
		}

		return ret;
	}

	//
	// methods common to DHT and Mcast
	//

	public RoutingService getRoutingService() {
		return this.routingSvc;
	}

	public DHTConfiguration getConfiguration() {
		return this.config;
	}

	public RoutingAlgorithmConfiguration getRoutingAlgorithmConfiguration() {
		return this.algoConfig;
	}

	public IDAddressPair getSelfIDAddressPair() {
		return this.routingSvc.getSelfIDAddressPair();
	}

	public void setStatCollectorAddress(String host, int port) throws UnknownHostException {
		MessagingAddress addr = this.routingSvc.getMessagingProvider().getMessagingAddress(host, port);

		this.routingSvc.setStatCollectorAddress(addr);
	}

	private ID lastKey = null;

	public String getLastKeyString() {
		if (lastKey == null)
			return "null";
		return lastKey.toString();
	}

	private RoutingResult lastRoutingResult = null;

	public RoutingResult getLastRoutingResult() {
		return this.lastRoutingResult;
	}

	public String getRoutingTableString() {
		return this.routingSvc.getRoutingAlgorithm().getRoutingTableString();
	}

	/*
	 * 馬鹿め、かかったな！！ 実物は違うクラス(ChurnTolerantDHTImplだったはず)を参照してるのさ！！！
	 */
	protected void prepareHandlers(RoutingService routingSvc) {
		this.prepareHandlers0(routingSvc);

		MessageHandler handler;

		handler = new PutMessageHandler();
		routingSvc.addMessageHandler(Tag.PUT.getNumber(), handler);

		handler = new RemoveMessageHandler();
		routingSvc.addMessageHandler(Tag.REMOVE.getNumber(), handler);

		// ------------------- 匿名通信用のハンドラ
		handler = new RelayMessageHandler();
		routingSvc.addMessageHandler(Tag.RELAY.getNumber(), handler);
		// ---------------------------------

	}

	protected void prepareHandlers0(RoutingService routingSvc) {
		MessageHandler handler;

		handler = new MessageHandler() {
			public Message process(Message msg) {
				Serializable[] contents = msg.getContents();

				// get locally
				ID[] keys = (ID[]) contents[0];
				Set<ValueInfo<V>>[] valueSets = new Set/* <ValueInfo<V>> */[keys.length];

				for (int i = 0; i < keys.length; i++) {
					valueSets[i] = getValueLocally(keys[i], globalDir);
				}

				return DHTMessageFactory.getDHTReplyMessage(BasicDHTImpl.this.getSelfIDAddressPair(), valueSets);
			}
		};
		routingSvc.addMessageHandler(Tag.GET.getNumber(), handler);

	}

	protected class PutMessageHandler implements MessageHandler {
		public Message process(Message msg) {
			// System.out.println(getSelfIDAddressPair().getAddress() + ": " +
			// Tag.getNameByNumber(msg.getTag())+ " from " + msg.getSource());
			Serializable[] contents = msg.getContents();

			IDAddressPair selfIDAddress = BasicDHTImpl.this.getSelfIDAddressPair();

			final DHT.PutRequest<V>[] requests = (DHT.PutRequest<V>[]) contents[0];
			long ttl = (Long) contents[1];
			final ByteArray hashedSecret = (ByteArray) contents[2];

			logger.log(Level.INFO, "A PUT message received" + (requests[0] == null ? "(null)" : requests[0].getKey()));

			if (ttl > config.getMaximumTTL())
				ttl = config.getMaximumTTL();
			else if (ttl <= 0)
				ttl = 0;

			// put locally
			Set<ValueInfo<V>>[] ret = new Set/* <ValueInfo<V>> */[requests.length];

			try {
				ValueInfo.Attributes attr = new ValueInfo.Attributes(ttl, hashedSecret);

				for (int i = 0; i < requests.length; i++) {
					// System.out.println("  key[" + i + "]: " + keys[i]);
					if (requests[i] == null)
						continue;

					ret[i] = new HashSet<ValueInfo<V>>();

					for (V v : requests[i].getValues()) {
						// System.out.println("  value: " + v);
						if (v != null) {
							if (globalDir.isInMemory()
									&& Runtime.getRuntime().freeMemory() < config.getRequiredFreeMemoryToPut())
								continue;

							ValueInfo<V> old = null;
							synchronized (globalDir) {
								old = globalDir.put(requests[i].getKey(), new ValueInfo<V>(v, attr), ttl);
							}

							if (old != null) {
								ret[i].add(old);
							}
						}
					}
				}
			}
			catch (Exception e) {
				// NOTREACHED
				logger.log(Level.WARNING, "An Exception thrown by Directory#put().", e);
			}

			return DHTMessageFactory.getDHTReplyMessage(selfIDAddress, ret);
		}
	}

	protected class RemoveMessageHandler implements MessageHandler {
		public Message process(Message msg) {
			Serializable[] contents = msg.getContents();

			IDAddressPair selfIDAddress = BasicDHTImpl.this.getSelfIDAddressPair();

			DHT.RemoveRequest<V>[] requests = (DHT.RemoveRequest<V>[]) contents[0];
			ByteArray hashedSecret = (ByteArray) contents[1];

			logger.log(Level.INFO, "A REMOVE message received"
					+ (requests[0] == null ? "(null)" : requests[0].getKey()));

			// remove locally from global directory
			Set<ValueInfo<V>>[] ret = new Set/* <ValueInfo<V>> */[requests.length];

			if (hashedSecret == null) {
				logger.log(Level.WARNING, "A REMOVE request with no secret.");
				return null;
			}

			for (int i = 0; i < requests.length; i++) {
				if (requests[i] == null)
					continue;

				ID key = requests[i].getKey();
				V[] values = requests[i].getValues();
				ID[] valueHash = requests[i].getValueHash();

				try {
					if (values == null) {
						ret[i] = globalDir.get(requests[i].getKey());

						if (ret[i] != null) {
							ValueInfo<V>[] existedValueArray = new ValueInfo/*
																			 * <V
																			 * >
																			 */[ret[i].size()];
							ret[i].toArray(existedValueArray);

							ret[i] = new HashSet<ValueInfo<V>>();

							for (ValueInfo<V> v : existedValueArray) {
								ID h = ID.getSHA1BasedID(v.getValue().toString().getBytes(config.getValueEncoding()));

								if (hashedSecret.equals(v.getHashedSecret())) {
									boolean remove = false;

									if (valueHash == null)
										remove = true;
									else {
										for (ID valH : valueHash) {
											if (h.equals(valH))
												remove = true;
										}
									}

									if (remove) {
										synchronized (globalDir) {
											globalDir.remove(key, v);
										}

										ret[i].add(v);
									}
								}
							}
						}
					}
					else {
						ret[i] = new HashSet<ValueInfo<V>>();

						synchronized (globalDir) {
							if (values != null) {
								for (V val : values) {
									ValueInfo<V> v = globalDir.remove(key, new ValueInfo<V>(val, 0L, hashedSecret));
									if (v != null)
										ret[i].add(v);
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

			return DHTMessageFactory.getDHTReplyMessage(selfIDAddress, ret);
		}
	}

	private void prepareCallbacks(RoutingService routingSvc) {
		routingSvc.addCallbackOnRoute(new CallbackOnRoute() {
			public Serializable process(ID target, int tag, Serializable[] args, CallbackResultFilter filter,
					IDAddressPair lastHop, boolean onRootNode) {
				ID key;

				logger.log(Level.INFO, "A callback invoked: " + (ID) args[0]);

				// get
				key = (ID) args[0];

				return (Serializable) getValueLocally(key, globalDir);
			}
		});
	}

	protected Set<ValueInfo<V>> getValueLocally(ID key, MultiValueDirectory<ID, ValueInfo<V>> dir) {
		Set<ValueInfo<V>> returnedValues = null;

		try {
			returnedValues = dir.get(key);
		}
		catch (Exception e) {
			// NOTREACHED
			logger.log(Level.WARNING, "An Exception thrown by Directory#get().", e);
			return null;
		}

		return returnedValues;
	}

	// successorを返す(Chordのみ)
	public IDAddressPair[] getSuccessorlist() {
		return this.routingSvc.getRoutingAlgorithm().getSuccessorlist();
	}
	
	// routingTable(fingerTable)を返す(Chordのみ)
	public IDAddressPair[] getRoutingTable() {
		return this.routingSvc.getRoutingAlgorithm().getRoutingTable();
	}

	public byte[] readData(String filename) {
		try {
			//System.out.println("読み込み:" + filename);
			// ファイルを読み込む
			File file = new File(filename);
			byte[] buf = new byte[(int) file.length()];
			FileInputStream fi = new FileInputStream(file);

			// read
			fi.read(buf);
			fi.close();

			//System.out.println("read complete");
			return (buf);

		}
		catch (FileNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return (null);
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	//
	// The following code is added by Hiroyuki Tanaka
	//
	// ////////////////////////////////////////////////////////////////////////////////////

	/**
	 * MEMO ひとまずはNIA&PKGにアクセスしないで行っていく ある程度進んだらNIA&PKGを利用して実験
	 */
	public boolean SetUp() {
		//System.out.println("OverlayWeaver version 0.9.4");
		//System.out.println("Bifrost version 0.2.21");

		//System.out.println("my node ID : " + getSelfIDAddressPair().getID().toString());

		try {
			long start = System.currentTimeMillis();

			// /////////////////////////////////////////////////////////////////////////////////
			// communicate to PKG
			// send selfID and get corresponding private key
			/*
			 * ObjectInputStream ois; ObjectOutputStream oos; Socket sock;
			 *
			 * System.out.println("connect to PKG");
			 *
			 * sock = new Socket(C.ADDR_PKG , C.PORT_PKG); oos = new
			 * ObjectOutputStream(sock.getOutputStream()); ois = new
			 * ObjectInputStream(sock.getInputStream());
			 *
			 * System.out.println("request to PKG");
			 *
			 * String id = getSelfIDAddressPair().getID().toString();
			 * oos.writeObject(id); oos.flush();
			 *
			 * Serializable ret[] = new Serializable[2]; ret = (Serializable[])
			 * ois.readObject();
			 *
			 * myPrivateKey = (Point)MyUtility.bytes2Object((byte[])ret[0]); MPK
			 * = (Point)MyUtility.bytes2Object((byte[])ret[1]);
			 *
			 * oos.close(); ois.close(); sock.close();
			 */
			// end communicate to PKG
			// /////////////////////////////////////////////////////////////////////////////////
			PublicKeyGenerator PKG = (PublicKeyGenerator) MyUtility.bytes2Object(MyUtility.readData(C.KEY_SERVER
					+ "PKG001"));
			MPK = PKG.getMPK();
			myPrivateKey = PKG.getPrivateKey(getSelfIDAddressPair().getID().toString());
			// set Master Publickey and my Private Key
			// this.encrypt = new EncryptUsingIPkey(MPK);
			// this.decrypt = new DecryptUsingIPkey(myPrivateKey);

			// In advance, generate instance of shared key object for speed-up
			KeyGenerator keyGen_s = KeyGenerator.getInstance("AES");
			keyGen_s.init(128);
			SecretKey secretkey = keyGen_s.generateKey();
			// decrypt.DecryptDataNoPadding("testtesttesttesttesttesttesttest".getBytes(),
			// secretkey);

			start = System.currentTimeMillis() - start;
		//	System.out.println("test decryption time " + start);
		}
		// catch (BadPaddingException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// catch (InvalidKeySpecException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//System.out.println("setup is complete");

		return (true);

	}

	public void resetDHT(ID selfID) {
		IDAddressPair[] succlist = getSuccessorlist();
		IDAddressPair init_contact = succlist[0];

		/*
		 * try { System.out.println("owdhtshell " +
		 * init_contact.getAddress().getHostAddress() + ":" +
		 * init_contact.getAddress().getPort() +
		 * " -r Recursive -t TCP --no-upnp -i " + selfID);
		 * Runtime.getRuntime().exec("owdhtshell " +
		 * init_contact.getAddress().getHostAddress() + ":" +
		 * init_contact.getAddress().getPort() +
		 * "-r Recursive -t TCP --no-upnp -i " + selfID);
		 *
		 * } catch(IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } /* try {
		 * resetBasicDHTImpl((short)Signature.APPLICATION_ID_DHT_SHELL,
		 * (short)0x10000, DHTFactory.getDefaultConfiguration(), selfID); }
		 * catch(Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
	}

	public void resetBasicDHTImpl(short applicationID, short applicationVersion, DHTConfiguration config, ID selfID /*
																													 * possibly
																													 * null
																													 */)
			throws Exception {
		IDAddressPair[] succlist = getSuccessorlist();

		// obtain messaging service
		byte[] messageSignature = Signature
				.getSignature(RoutingServiceFactory.getRoutingStyleID(config.getRoutingStyle()),
						RoutingAlgorithmFactory.getAlgorithmID(config.getRoutingAlgorithm()), applicationID,
						applicationVersion);

		MessagingProvider msgProvider = MessagingFactory.getProvider(config.getMessagingTransport(), messageSignature);
		if (config.getSelfAddress() != null) {
			msgProvider.setSelfAddress(config.getSelfAddress());
		}

		MessagingConfiguration msgConfig = msgProvider.getDefaultConfiguration();
		msgConfig.setDoUPnPNATTraversal(config.getDoUPnPNATTraversal());

		MessageReceiver receiver = msgProvider.getReceiver(msgConfig, config.getSelfPort(), config.getSelfPortRange());
		config.setSelfPort(receiver.getPort()); // correct config

		// obtain routing service
		RoutingAlgorithmProvider algoProvider = RoutingAlgorithmFactory.getProvider(config.getRoutingAlgorithm());
		this.algoConfig = algoProvider.getDefaultConfiguration();

		RoutingServiceProvider svcProvider = RoutingServiceFactory.getProvider(config.getRoutingStyle());
		RoutingService routingSvc = svcProvider.getService(svcProvider.getDefaultConfiguration(), msgProvider,
				receiver, algoProvider, this.algoConfig, selfID);

		// instantiate a RoutingAlgorithm in the routing service
		algoProvider.initializeAlgorithmInstance(this.algoConfig, routingSvc);
		this.init(config, routingSvc);

		SetUp();

		joinOverlay(succlist[0].getAddress().getHostAddress(), succlist[0].getAddress().getPort());
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	//
	// The following code is added by Nozomu Hatta
	//
	// ////////////////////////////////////////////////////////////////////////////////////

	/*
	 * 匿名路構築情報を作成し、送信するクラス
	 */
	@Override
	public boolean construct(ID targetID, int relayCount) {
		// 引数から匿名路構築情報を作成する
		// 本当は最新群の値は経路表から取得する必要があるが面倒なので固定値の４でとりあえず進める
		// もちろん実験の場合は参加ノードの全てのIDレベルが４以上である必要がある
		int latestIDLevel = 2;
		AnonymousRouteInfo constructInfo = new AnonymousRouteInfo(targetID, latestIDLevel, relayCount, config);
		config.incrementConstructNumber();
		constructInfo.printRouteInfo();
		config.globalSb.append(constructInfo.toString());

		// 直接の送信先を取得
		ID nextID = constructInfo.getLocalTargetID();

		// 匿名路情報からヘッダ部を作成し、メッセージ作成
		byte[] header = constructInfo.getConstructMessage(MPK).getHeader();
		Message msg = DHTMessageFactory.getConstructMessage(this.getSelfIDAddressPair(), header);

		// 送信先取得（？）
		MessagingAddress dest = getNeighberAddress(nextID);

		// 確認
		//IDAddressPair myPair = getSelfIDAddressPair();
		//config.globalSb.append("I'm true sender : " + myPair.toString());
		config.globalSb.append("just send to dest : " + dest.toString()+"\n");
		
		//System.out.println("I'm true sender : " + myPair.toString());
		//System.out.println("just send to dest : " + dest.toString());
		try {
			sender.send(dest, msg);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			config.globalSb.append("ただしく送信できませんでした");
			return false;
		}

		// 匿名路情報をIDをキーにして保存
		Integer primalKey = Arrays.hashCode(header);
		config.globalSb.append("entry primalkey : " + primalKey + "\n");
		config.globalSb.append("entry targetID : " + targetID + "\n");
		this.senderMap.put(targetID, primalKey);
		this.senderProcessMap.put(primalKey, constructInfo);

		return true;
	}

	/*
	 * 匿名路構築情報を作成し、送信するクラス construct関数との違いは中継ノードもユーザが指定できる点である
	 * （通常のconstructでは中継ノードは無作為に選ぶ）
	 */
	@Override
	public boolean constructTest(ID targetID, ID[] relayID) {
		// TODO Auto-generated method stub
		AnonymousRouteInfo constructInfo = new AnonymousRouteInfo(targetID, relayID);
		constructInfo.printRouteInfo();

		// 直接の送信先を取得
		ID nextID = constructInfo.getLocalTargetID();

		// 匿名路情報からヘッダ部を作成し、メッセージ作成
		byte[] header = constructInfo.getConstructMessage(MPK).getHeader();
		Message msg = DHTMessageFactory.getConstructMessage(this.getSelfIDAddressPair(), header);

		// 送信先取得（？）
		MessagingAddress dest = getNeighberAddress(nextID);

		// 確認
		IDAddressPair myPair = getSelfIDAddressPair();
		config.globalSb.append("I'm true sender : " + myPair.toString());
		config.globalSb.append("just send to dest : " + dest.toString());
		try {
			sender.send(dest, msg);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			config.globalSb.append("ただしく送信できませんでした");
			return false;
		}

		// 匿名路情報をIDをキーにして保存
		Integer primalKey = Arrays.hashCode(header);
		this.senderMap.put(targetID, primalKey);
		this.senderProcessMap.put(primalKey, constructInfo);
		return false;
	}

	/*
	 * 本来はAnonymousRouteInfoの中では乱数を利用して中継ノードを決定する。
	 * しかし、それ（乱数を使う場合）ではテストの際に不便である（かもしれない）。
	 * この関数はAnonymousRouteInfoクラスを意図的に生成する。
	 *
	 * 未完成
	 *
	 * @return
	 */
	private AnonymousRouteInfo getIntentionalRouteInfo() {
		AnonymousRouteInfo retInfo = new AnonymousRouteInfo();
		return retInfo;
	}

	/**
	 * TODO 詳しい把握
	 *
	 * ぶっちゃけよく分かってないんです 誰か教えてくれ エラーが起きたとき上に渡すか、ここで処理するべきかが分からん
	 */
	private MessagingAddress getNeighberAddress(ID nextID) {
		try {
			RoutingResult routingRes = this.routingSvc.routeToRootNode(nextID, 1);
			IDAddressPair[] rootCands = routingRes.getRootCandidates();
			IDAddressPair neigberPair = rootCands[0];
			return neigberPair.getAddress();
		}
		catch (RoutingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 多段中継（匿名通信）用のメッセージを受け取った際に動作するハンドラ。
	 *
	 * @author nozomu
	 *
	 */
	protected class RelayMessageHandler implements MessageHandler {
		@Override
		public Message process(Message msg) throws Exception {
//			IDAddressPair pair = getSelfIDAddressPair();
			config.globalSb.append("\nreceive message");

			Serializable[] contents = msg.getContents();
			int type = (Integer) contents[C.MESSAGE_TYPE];

			switch (type) {
			case C.TYPE_CONSTRUCTION:
				constructProcess(msg);
				break;
			case C.TYPE_REPAIR:
				break;
			case C.TYPE_COMMUNICATION:
				communicateProcess(msg);
				break;
			case C.TYPE_COMMUNICATION_REJECT: // 廣瀬が追加 11/27
				//送信者のみ知ることが出来る
				//つまり、通信を変更したいノードは平文のmsgにこのタグをつける
				//うん、上のは無理。新しいタグを作ってそれに応じて新しいメッセージ経路を作成。
				communicateProcess(msg);
				break;
			case C.TYPE_COMMUNICATION_RELAY: // 廣瀬が追加 11/27
				//送信者のみ知ることが出来る
				//つまり、通信を変更したいノードは平文のmsgにこのタグをつける
				//うん、上のは無理。新しいタグを作ってそれに応じて新しいメッセージ経路を作成。
				communicateProcess(msg);
				break;
			default:
				config.globalSb.append("定義していない型のメッセージを受け取りました");
			}

			// 匿名通信では送信元ノードへの返信の必要が無いのでnullを返すだけ。
			return null;
		}

		/*
		 * 匿名路構築のためのメッセージが来た場合の処理
		 */
		private void constructProcess(Message recvMsg) {
			config.globalSb.append("\nconstructProcess\n");
			//廣瀬が変更 11/29
			commFlag flag = commFlag.Permit;
			try{
				flag = config.getCommunicateMethodFlag(config.getConstructNumber());
			} catch(Exception e){
				//flag = commFlag.Error;	
			}
			switch(flag){
			case Permit : // 通常の中継
				config.globalSb.append("construct permit start\n");
				ordinaryConstructMessage(recvMsg);
				config.globalSb.append("\nconstruct permit end\n");
				//構築している匿名路が増加したため構築数を増加
				config.incrementConstructNumber();
				break;
				
			case Relay : // 中継のみ許可
				//変更通知作成して送信者に送信
				constructChange(recvMsg,flag,config.getConstructNumber());
				//次のノードへ通常通りの中継を行う
				ordinaryConstructMessage(recvMsg);
				//構築している匿名路が増加したため構築数を増加
				config.incrementConstructNumber();
				break;
				
			case Reject : //中継拒否時
				//通信拒否のメッセージの送信
				constructChange(recvMsg,flag,config.getConstructNumber());
				break;
			default : // 通常の中継
				config.globalSb.append("Error\n");
				config.setCommunicateMethodFlag(commFlag.Permit, config.getConstructNumber());
				config.globalSb.append("Error2\n");
				ordinaryConstructMessage(recvMsg);
				config.incrementConstructNumber();
			}
			
		}

		/*
		 * 匿名路修復のためのメッセージが来た場合の処理
		 */
		private void repairProcess() {
		}

		/*
		 * 匿名通信のためのメッセージが来た場合の処理 関数が長くなってきたからどこかでまとめるべきかもしれない。
		 */
		private void communicateProcess(Message msg) {
			config.globalSb.append("\nCommunicate Process start\n");
			Serializable[] contents = msg.getContents();
			Integer key = (Integer) contents[C.MESSAGE_PRIMALKEY];

			// 処理内容を取得する
			config.globalSb.append("receive key : " + key+ "\n");

			// 送信ノードとしての動作
			if (senderProcessMap.containsKey(key)) {
				receiveMessageAsSender(msg);
			}

			// 中継ノード、または受信ノードとしての動作
			else if (relayProcessMap.containsKey(key)) {
				receiveMessageAsRelay(msg);
			}
			config.globalSb.append("Communicate Process end\n");
		}
		/*
		 * 匿名路の変更のためのメッセージが来た場合の処理
		 * 11/27 廣瀬が追加
		 */
		private void communicateChangeProcess(Message msg) {
			//まだ実装してない・・・というかこの関数ではダメだと思う
			//返信用の関数を用意して使わないと行けない。
			//ただ、これは返信用で八田さんが実装してあるはずなので、それを利用してやりたい。
			communicate(lastKey, MPK);
			//この関数ではダメ。ここでは新しい匿名路構築するための部分と作成したメッセージ再送の関数にしないと行けない
		}
	}

	/*
	 * 送信ノードとしてメッセージを受け取った場合
	 */
	private void receiveMessageAsSender(Message msg) {
		try {
			config.globalSb.append("reciveMessageAsSender start\n");
			Serializable[] contents = msg.getContents();
			byte[] body = (byte[]) contents[C.MESSAGE_BODY];
			Integer key = (Integer) contents[C.MESSAGE_PRIMALKEY];
			int type = (Integer) contents[C.MESSAGE_TYPE];
			
			AnonymousRouteInfo arInfo = senderProcessMap.get(key);
			int constructNumber = arInfo.getConstructNumber();
			ArrayList<SecretKey> keyList = arInfo.getKeyList();
			switch(type){
			case C.TYPE_COMMUNICATION :
				config.globalSb.append("Communicate Message\n");
				for (SecretKey secKey : keyList) {
					body = CipherTools.decryptDataPadding(body, secKey);
				}
				config.globalSb.append("recive Message : "+(String)MyUtility.bytes2Object(body)+"\n");
				break;
			case C.TYPE_COMMUNICATION_REJECT :
				config.globalSb.append("Reject Message\n");
				SecretKey tmpSecKey = null;
				for (SecretKey secKey : keyList) {
					body = CipherTools.decryptDataPadding(body, secKey);
					
					//
					//平文かどうか確認する機構を追加しないといけない
					//
					try{
						Object obj = MyUtility.bytes2Object(body);
						//平文だった場合
						tmpSecKey = secKey;	
					}catch(IOException e){
							continue;
					}catch(ClassNotFoundException e){
						continue;
					}
				}
				IDAddressPair rejectNode = (IDAddressPair)MyUtility.bytes2Object(body);
				//拒否するノードを登録
				config.setRejectID(rejectNode.getID());
				config.globalSb.append("recive Message : "+rejectNode+"\n");
				//了承通知を作成、及び送信
				String approvalMsg = "approval";
				communicate(arInfo.getGlobalTargetID(),approvalMsg,keyList.indexOf(tmpSecKey));
				config.globalSb.append("了承通知を送信しました\n");
				break;
			case C.TYPE_COMMUNICATION_RELAY :
				config.globalSb.append("Relay Message\n");
				SecretKey tmpSecKey1 = null;
				for (SecretKey secKey : keyList) {
					body = CipherTools.decryptDataPadding(body, secKey);
					
					//
					//平文かどうか確認する機構を追加しないといけない
					//
					try{
						Object obj = MyUtility.bytes2Object(body);
						//平文だった場合
						tmpSecKey1 = secKey;
						//識別タグを入手
						Integer tmpTag = config.getRelayTag(constructNumber, tmpSecKey1);
						//それを保存する
						config.setRelaySendNode(constructNumber, tmpSecKey1, tmpTag);
					}catch(IOException e){
						continue;
					}catch(ClassNotFoundException e){
						continue;
					}
				}
				//フラグ操作をする必要がある
				//フラグじゃなくてもいいけど、変更ノードの情報を登録する必要あり
				config.globalSb.append("recive Message : "+(Integer)MyUtility.bytes2Object(body)+"\n"+"constructNumber : "+constructNumber+"\n");
				//了承通知を作成、及び送信
				String approvalMsg1 = "approval";
				communicate(arInfo.getGlobalTargetID(),approvalMsg1,keyList.indexOf(tmpSecKey1));
				config.globalSb.append("了承通知を送信しました\n");
				break;
			}
			config.globalSb.append("送信先（受信者）からの返信を受け取りました\n");

			config.globalSb.append("reciveMessageAsSender start\n");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			config.globalSb.append("送信先からメッセージを受け取りましたが復号できませんでした");
			e.printStackTrace();
		}
	}

	/*
	 * 中継ノード、または受信ノードとしてメッセージを受け取った場合
	 */
	private void receiveMessageAsRelay(Message msg) {
		try {
			config.globalSb.append("receiveMessageAsRelay start\n");
			Serializable[] contents = msg.getContents();
			Integer key = (Integer) contents[C.MESSAGE_PRIMALKEY];
			config.globalSb.append("1");
			RelayProcessSet value = relayProcessMap.get(key);
			int constructNumber = value.getNumber();
			config.globalSb.append("2");
			//廣瀬が変更 11/29
			commFlag flag = commFlag.Permit;
			try{
				flag = config.getCommunicateMethodFlag(constructNumber);
			} catch(Exception e){
				//flag = commFlag.Error;	
			}
			config.globalSb.append("3\n");
			switch(flag){
			case Permit : // 通常の中継
				config.globalSb.append("permit\n");
				ordinaryCommunicateMessage(msg);
				break;
				
			case Relay : // 中継のみ許可
				config.globalSb.append("relay\n");
				//変更通知を作成しているかを確認
				if(!(config.checkCommFlag(constructNumber))){
					//変更通知作成して通常通りの中継も行う
					communicateChange(msg,flag);
					ordinaryCommunicateMessage(msg);
					
				}else{
					//了承通知を受信しているかを確認
					if(config.getApprovalChangeFlag(constructNumber)){
						//受信していたら識別タグを読み取り次のノードへ送信
						int distinctionTag = (Integer) contents[C.MESSAGE_DISTINCTION_TAG];
						MessagingAddress nextNode = config.getNextNodeAddress(distinctionTag, getNeighberAddress(msg.getSource().getID()));
						sender.send(nextNode, msg);
					}else{
						//受信していないなら通常通りの中継
						ordinaryCommunicateMessage(msg);
					}
				}
				break;
				
			case Reject : // 中継拒否
				config.globalSb.append("reject\n");
				//変更通知を作成しているかを確認
				if(!(config.checkCommFlag(constructNumber))){
					//変更通知作成して通常通りの中継も行う
					communicateChange(msg,flag);
					
					ordinaryCommunicateMessage(msg);
					
				}else{
					//了承通知を受信しているかを確認
					if(config.getApprovalChangeFlag(constructNumber)){
						//受信していたらconstructNumberを減らす
						config.decrementConstructNumber();
					}else{
						//受信していないなら通常通りの中継
						ordinaryCommunicateMessage(msg);
					}
				}
				break;
			}
			config.globalSb.append("receiveMessageAsRelay end\n");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			config.globalSb.append("\nreceiveMessageAsRelay error\n");
		}
	}

	/*
	 * 匿名路を構築した後に匿名通信を行うための関数
	 */
	@Override
	public boolean communicate(ID targetID, Object mail) {
		Integer mainKey = this.senderMap.get(targetID);
		
		config.globalSb.append("\ncommunicate start\n");
		config.globalSb.append("send Message : "+mail+"\n");
		AnonymousRouteInfo arInfo = this.senderProcessMap.get(mainKey);
		int constructNumber;
		int distinctionTag=-1;
		try{
			constructNumber = arInfo.getConstructNumber();			
		}catch (Exception e){
			constructNumber=0;
			config.globalSb.append("error in communicate \n"+e.toString()+"\n"+mainKey+"\n");
			config.globalSb.append("mainkey : "+mainKey+"\n");
			config.globalSb.append("targetID : "+targetID+"\n");
		}
		config.globalSb.append(arInfo.toKeyListString());
		ArrayList<SecretKey> keyList = arInfo.getKeyList();
		byte[] byteMail;
		try {
			byteMail = MyUtility.object2Bytes(mail);
			// 中継ノードとの共通鍵で暗号化
			for (int i = keyList.size() - 1; i >= 0; i--) {
				SecretKey secKey = keyList.get(i);
				//中継のみ行うノードが含まれていた場合、処理を変更
				if(config.checkRelayID(constructNumber, secKey)){
					distinctionTag = config.getRelayTag(constructNumber, secKey); 
				}else{
					byteMail = CipherTools.encryptDataPadding(byteMail, secKey);
				}
			}
			// 直接の送信者のための主キーを取得
			Integer primalKey = arInfo.getPrimaryKey();
			
			// 直接の送信先情報を取得
			ID localTargetID = arInfo.getLocalTargetID();
			MessagingAddress dest = getNeighberAddress(localTargetID);
			
			// メッセージ作成
			Message msg = DHTMessageFactory.getCommunicateMessage(this.getSelfIDAddressPair(), byteMail, primalKey,distinctionTag);
			sender.send(dest, msg);

			config.globalSb.append("send mainkey : " + mainKey + "\n");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			config.globalSb.append("error");
		}
		config.globalSb.append("communicate end\n");
		return true;
	}

	
	/*
	 * 匿名路を構築した後に匿名路の途中までメッセージを送信する場合に使用する関数
	 */
	public boolean communicate(ID targetID, Object mail,int number) {
		Integer mainKey = this.senderMap.get(targetID);
		config.globalSb.append("communicate change message send start\n");
		AnonymousRouteInfo arInfo = this.senderProcessMap.get(mainKey);
		int distinctionTag = -1;
		ArrayList<SecretKey> keyList = arInfo.getKeyList();
		config.globalSb.append("1");
		byte[] byteMail;
		try {
			byteMail = MyUtility.object2Bytes(mail);
			config.globalSb.append("2");
			// 中継ノードとの共通鍵で暗号化
			for (int i = number; i >= 0; i--) {
				SecretKey secKey = keyList.get(i);
				byteMail = CipherTools.encryptDataPadding(byteMail, secKey);
			}
			config.globalSb.append("3");
			// 直接の送信者のための主キーを取得
			Integer primalKey = arInfo.getPrimaryKey();

			// 直接の送信先情報を取得
			ID localTargetID = arInfo.getLocalTargetID();
			MessagingAddress dest = getNeighberAddress(localTargetID);

			// メッセージ作成
			Message msg = DHTMessageFactory.getCommunicateMessage(this.getSelfIDAddressPair(), byteMail, primalKey,distinctionTag);
			sender.send(dest, msg);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		config.globalSb.append("\ncommunicate change message send end\n");
		return true;
	}

	
	/**
	 * 目的先への匿名路がすでに存在するかどうかを判断する関数
	 *
	 * @param strID
	 *            目的先のノードのIDをあらわす文字列
	 * @return　匿名路が存在すればtrue、なければfalse
	 */
	public boolean containsRoute(ID targetID) {
		return this.senderMap.containsKey(targetID);
	}


	/*
	 * (non-Javadoc)
	 * @see ow.dht.DHT#notice(ow.id.ID)
	 * ノードの離脱を検知した場合の処理
	 */
	@Override
	public void notice(ID departureID) {
		// TODO Auto-generated method stub

	}

	/**
	 * 匿名路構築後に変更通知を送信者に送信する関数
	 *
	 * @param Message
	 *            受信しているメッセージ
	 * @param commFlag
	 *            現在の選択肢のフラグ状況
	 * @author Hirose
	 *
	 */
	public void constructChange(Message recvMsg,commFlag flag,int number){
		try{
			config.globalSb.append("construct change start\n");
			Serializable[] contents = recvMsg.getContents();
			byte[] encHeader = (byte[]) contents[C.MESSAGE_HEADER];

			// 自身の持つ秘密鍵で復号
			byte[] decHeader = CipherTools.decryptByIBEPadding(encHeader, myPrivateKey);
			Integer primeKey = Arrays.hashCode(decHeader);
			Object obj = MyUtility.bytes2Object(decHeader);
			AnonymousHeader headerSet = (AnonymousHeader) obj;
			
			// ヘッダから情報取得
			SecretKey secKey = headerSet.getSharedKey();
			
			// 匿名通信を行う際の処理情報を保存
			IDAddressPair src1 = recvMsg.getSource();
			MessagingAddress org1 = getNeighberAddress(src1.getID());
			
			
			//送信者に向けて変更通知用のメッセージを作成
			//Message sendMsg1 = DHTMessageFactory.getCommunicateMessage(src1, body, key);
			Message msg;
			switch(flag){
			case Relay :
				// メッセージ作成
				Integer reply = number;
				byte[] body = MyUtility.object2Bytes(reply);
				body = CipherTools.encryptDataPadding(body, secKey);
				
				ID nextID = headerSet.getNextID();
				MessagingAddress dest = getNeighberAddress(nextID);
				config.setRelayChangeNode(number, org1, dest);
				msg = DHTMessageFactory.getCommunicateRelayMessage(getSelfIDAddressPair(), body, primeKey);
				break;
			case Reject : 
				// メッセージ作成
				IDAddressPair reply1 = getSelfIDAddressPair();
				byte[] body1 = MyUtility.object2Bytes(reply1);
				body = CipherTools.encryptDataPadding(body1, secKey);
				
				msg = DHTMessageFactory.getCommunicateRejectMessage(getSelfIDAddressPair(), body, primeKey);
				break;
			default :
				msg = null;	
			}
			
			//了承通知用のフラグを初期化
			config.setApprovalChangeFlag(false, number);
			//送信
			sender.send(org1,msg);
			config.globalSb.append("\nconstruct change end\n");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 匿名路構築後に変更通知を送信者に送信する関数
	 *
	 * @param Message
	 *            受信しているメッセージ
	 * @param commFlag
	 *            現在の選択肢のフラグ状況
	 * @author Hirose
	 *
	 */
	public void communicateChange(Message recvMsg,commFlag flag){
		try{
			config.globalSb.append("commincate change start\n");
			Serializable[] contents = recvMsg.getContents();
			Integer key = (Integer) contents[C.MESSAGE_PRIMALKEY];
			
			RelayProcessSet value = relayProcessMap.get(key);
			SecretKey secKey = value.getSecretKey();
			// 匿名通信を行う際の処理情報を保存
			IDAddressPair src1 = recvMsg.getSource();
			MessagingAddress org1 = getNeighberAddress(src1.getID());
			
			
			//送信者に向けて変更通知用のメッセージを作成
			//Message sendMsg1 = DHTMessageFactory.getCommunicateMessage(src1, body, key);
			Message msg;
			switch(flag){
			case Relay :
				// メッセージ作成
				Integer reply = value.getNumber();
				byte[] body = MyUtility.object2Bytes(reply);
				body = CipherTools.encryptDataPadding(body, secKey);
				
				MessagingAddress dest = value.getDestMessagingAddress();
				config.setRelayChangeNode(reply, org1, dest);
				msg = DHTMessageFactory.getCommunicateRelayMessage(getSelfIDAddressPair(), body, key);
				break;
			case Reject : 
				// メッセージ作成
				IDAddressPair reply1 = getSelfIDAddressPair();
				byte[] body1 = MyUtility.object2Bytes(reply1);
				body = CipherTools.encryptDataPadding(body1, secKey);
				
				msg = DHTMessageFactory.getCommunicateRejectMessage(getSelfIDAddressPair(), body, key);
				break;
			default :
				msg = null;	
			}
			//送信
			sender.send(org1,msg);
			config.globalSb.append("\ncommincate change end\n");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 通常の中継を行うための関数
	 * 
	 * @param Message
	 *            受信しているメッセージ
	 * @author hirose
	 * 
	 */
	private void ordinaryCommunicateMessage(Message msg){
		try {
			config.globalSb.append("\nordinary commincate message start\n");
			Serializable[] contents = msg.getContents();
			byte[] body = (byte[]) contents[C.MESSAGE_BODY];
			Integer key = (Integer) contents[C.MESSAGE_PRIMALKEY];
			int type = (Integer) contents[C.MESSAGE_TYPE];
			int distinctionTag = (Integer) contents[C.MESSAGE_DISTINCTION_TAG];
			
			RelayProcessSet value = relayProcessMap.get(key);
			SecretKey secKey = value.getSecretKey();
		
			
			MessagingAddress dest = value.getDestMessagingAddress();
			
			Integer primalKey = value.getPrimalKey();
			
			switch (value.getDecOrEnc()) {
			case C.DECRYPT:
				body = CipherTools.decryptDataPadding(body, secKey);
				break;
			case C.ENCRYPT:
				body = CipherTools.encryptDataPadding(body, secKey);
				break;
			default:
				// 到達するはずのない、到達したらおかしい場所
		//		config.globalSb.append("来るはずのない地点に到着しました。プログラムを見直してください");
				return;
			}

			// もしも自身が受信者だった場合
			if (dest == null) {
				config.globalSb.append("I'm receiver.");
				Object mail = MyUtility.bytes2Object(body);
				if (mail instanceof String) {
					config.globalSb.append("mail : " + mail.toString()+"\n");

					// 返信のための処理
					// 直接の送信先入手
					IDAddressPair addr = msg.getSource();
					MessagingAddress dest2 = getNeighberAddress(addr.getID());

					// メッセージ作成
					String reply = mail.toString() + " ack";
					body = MyUtility.object2Bytes(reply);
					body = CipherTools.encryptDataPadding(body, secKey);
					msg = DHTMessageFactory.getCommunicateMessage(getSelfIDAddressPair(), body, key,distinctionTag);
					config.globalSb.append("send key hash : " + key.hashCode() + "\n");
					// 送信
					sender.send(dest2, msg);
				}
				return;
			}
			//受信したメッセージのタイプによって送信方法の変更
			switch(type){
			case C.TYPE_COMMUNICATION : 
				config.globalSb.append("send key : " + primalKey +"\n");
				msg = DHTMessageFactory.getCommunicateMessage(getSelfIDAddressPair(), body, primalKey,distinctionTag);
				sender.send(dest, msg);
				break;
				
				//中継拒否用の変更通知用
			case C.TYPE_COMMUNICATION_REJECT : 
				config.globalSb.append("send key : " + primalKey +"\n");
				msg = DHTMessageFactory.getCommunicateRejectMessage(getSelfIDAddressPair(), body, primalKey);
				sender.send(dest, msg);
				break;
				
				//暗号処理なし用の変更通知用
			case C.TYPE_COMMUNICATION_RELAY :
				config.globalSb.append("send key : " + primalKey +"\n");
				msg = DHTMessageFactory.getCommunicateRelayMessage(getSelfIDAddressPair(), body, primalKey);
				sender.send(dest, msg);
				break;
				
				//了承通知用
			case C.TYPE_CHANGE_APPROVE : 
				config.globalSb.append("send key : " + primalKey +"\n");
				msg = DHTMessageFactory.getChangeApproveMessage(getSelfIDAddressPair(), body, primalKey);
				sender.send(dest, msg);
				break;
			}
			config.globalSb.append("\n"+"just send to dest : " + dest.toString()+"\n");
			config.globalSb.append("\nordinary commincate message end\n");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 通常の中継を行うための関数
	 * 
	 * @param Message
	 *            受信しているメッセージ
	 * @author hirose
	 * 
	 */
	private void ordinaryConstructMessage(Message recvMsg){
		try {
			config.globalSb.append("ordinaryConstructMessage\n");
			Serializable[] contents = recvMsg.getContents();
			byte[] encHeader = (byte[]) contents[C.MESSAGE_HEADER];
			config.globalSb.append("encHeader : "+encHeader+"\n");
			
			// 自身の持つ秘密鍵で復号
			byte[] decHeader = CipherTools.decryptByIBEPadding(encHeader, myPrivateKey);
			Object obj = MyUtility.bytes2Object(decHeader);
			AnonymousHeader headerSet = (AnonymousHeader) obj;
			// ヘッダから情報取得
			ID nextID = headerSet.getNextID();
			byte[] nextHeader = headerSet.getNextHeader();
			SecretKey secKey = headerSet.getSharedKey();

			// byte配列からIntegerに変換
			Integer sendPrimalKey = Arrays.hashCode(nextHeader);
			Integer recvPrimalKey = Arrays.hashCode(encHeader);

			// 自身が受信者だった場合の処理（次のIDが無ければ受信者）
			if (nextID == null) {
				config.globalSb.append("\nI'm a actual receiver");

				// 送信者側への処理のための情報を保存
				IDAddressPair src = recvMsg.getSource();
				MessagingAddress org = getNeighberAddress(src.getID());
				RelayProcessSet toSenderSide = new RelayProcessSet(org, secKey, recvPrimalKey, C.ENCRYPT , config.getConstructNumber());
				relayProcessMap.put(sendPrimalKey, toSenderSide);

				// 復号するための情報を保存
				RelayProcessSet toReceiverSide = new RelayProcessSet(null, secKey, sendPrimalKey, C.DECRYPT , config.getConstructNumber());
				relayProcessMap.put(recvPrimalKey, toReceiverSide);

				return;
			}
			Message sendMsg = DHTMessageFactory.getConstructMessage(getSelfIDAddressPair(), nextHeader);
			MessagingAddress dest = getNeighberAddress(nextID);
			sender.send(dest, sendMsg);
			
			config.globalSb.append("\n"+"just send to dest : " + dest.toString()+"\n");

			// 匿名通信を行う際の処理情報を保存
			IDAddressPair src = recvMsg.getSource();
			MessagingAddress org = getNeighberAddress(src.getID());

			// 送信者側への処理のための情報を保存
			RelayProcessSet toSenderSide = new RelayProcessSet(org, secKey, recvPrimalKey, C.ENCRYPT , config.getConstructNumber());
			relayProcessMap.put(sendPrimalKey, toSenderSide);
			config.globalSb.append("nextHeader : " + nextHeader+"\n");
			config.globalSb.append("decHeader : " + decHeader);
			
			// 受信者側への処理のための情報を保存
			RelayProcessSet toReceiverSide = new RelayProcessSet(dest, secKey, sendPrimalKey, C.DECRYPT , config.getConstructNumber());
			relayProcessMap.put(recvPrimalKey, toReceiverSide);
		}
		catch (ClassNotFoundException e){
			config.globalSb.append("ClassNotFoundException\n"+e.toString()+"\n");
			e.printStackTrace();
		}
		catch (BadPaddingException e){
			config.globalSb.append("BadPaddingException\n"+e.toString()+"\n");
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
