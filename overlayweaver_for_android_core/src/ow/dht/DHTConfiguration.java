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

package ow.dht;

import ow.id.ID;
import ow.id.IDAddressPair;

public class DHTConfiguration {
	public final static String DEFAULT_IMPL_NAME = "ChurnTolerantDHT";
//	public final static String DEFAULT_IMPL_NAME = "BasicDHT";
//	public final static String DEFAULT_IMPL_NAME = "CHT";	// Centralized Hash Table

	// for Routing
	public final static String DEFAULT_ROUTING_STYLE = "Iterative";
//	public final static String DEFAULT_ROUTING_STYLE = "Recursive";
		// "Iterative" or "Recursive"
	public final static String DEFAULT_ROUTING_ALGORITHM = "Chord";
//	public final static String DEFAULT_ROUTING_ALGORITHM = "Kademlia";
//	public final static String DEFAULT_ROUTING_ALGORITHM = "Koorde";
//	public final static String DEFAULT_ROUTING_ALGORITHM = "LinearWalker";
//	public final static String DEFAULT_ROUTING_ALGORITHM = "Pastry";
//	public final static String DEFAULT_ROUTING_ALGORITHM = "Tapestry";
		// "Chord", "Kademlia", "Koorde", "LinearWalker", "Tapestry" or "Pastry"

	// for Messaging
	public final static String DEFAULT_MESSAGING_TRANSPORT = "UDP";
//	public final static String DEFAULT_MESSAGING_TRANSPORT = "TCP";
		// "UDP" or "TCP"
	public final static int DEFAULT_SELF_PORT = 3997;
	public final static int DEFAULT_SELF_PORT_RANGE = 100;
	public final static int DEFAULT_CONTACT_PORT = 3997;
	public final static boolean DEFAULT_DO_UPNP_NAT_TRAVERSAL = true;

	// for Directory
	public final static String DEFAULT_DIRECTORY_TYPE = "VolatileMap";
		// "BerkeleyDB", "PersistentMap" or "VolatileMap"
	public final static Class DEFAULT_VALUE_CLASS = String.class;
	public final static String DEFAULT_WORKING_DIR = ".";

	public final static boolean DEFAULT_DO_EXPIRE = true;
	public final static long DEFAULT_MAXIMUM_TTL = 7 * 24 * 60 * 60 * 1000L;	// 7 days
	public final static long DEFAULT_DEFAULT_TTL = 3 * 60 * 60 * 1000L;		// 3 hours

	// for DHT
	public final static boolean DEFAULT_MULTIPLE_VALUES_FOR_A_SINGLE_KEY = true;
	public final static int DEFAULT_NUM_SPARE_ROOT_CANDIDATES = 3;

	public final static int DEFAULT_NUM_REPLICA = 1;
	public final static boolean DEFAULT_ROOT_DOES_REPLICATION = true;
	public final static int DEFAULT_NUM_NODES_ASKED_TO_TRANSFER = 0;
	public final static int DEFAULT_NUM_TIMES_GETS = 1;

	public final static boolean DEFAULT_DO_REPUT_ON_REPLICAS = false;
	public final static boolean DEFAULT_DO_REPUT_ON_REQUESTER = false;
	public final static long DEFAULT_REPUT_INTERVAL = 30 * 1000L;
	public final static double DEFAULT_REPUT_INTERVAL_PLAY_RATIO = 0.3;
	public static final boolean DEFAULT_USE_TIMER_INSTEAD_OF_THREAD = true;

	public final static String DEFAULT_VALUE_ENCODING = "UTF-8";
	public final static long DEFAULT_REQUIRED_FREE_MEMORY_TO_PUT = 128 * 1024L;	// 128 KB


	private String implName = DEFAULT_IMPL_NAME;
	public String getImplementationName() { return this.implName; }
	public String setImplementationName(String name) {
		String old = this.implName;
		this.implName = name;
		return old;
	}

	private String routingStyle = DEFAULT_ROUTING_STYLE;
	public String getRoutingStyle() { return this.routingStyle; }
	public String setRoutingStyle(String type) {
		String old = this.routingStyle;
		this.routingStyle = type;
		return old;
	}

	private String algo = DEFAULT_ROUTING_ALGORITHM;
	public String getRoutingAlgorithm() { return this.algo; }
	public String setRoutingAlgorithm(String algo) {
		String old = this.algo;
		this.algo = algo;
		return old;
	}

	private String messagingTransport = DEFAULT_MESSAGING_TRANSPORT;
	public String getMessagingTransport() { return this.messagingTransport; }
	public String setMessagingTransport(String transport) {
		String old = this.messagingTransport;
		this.messagingTransport = transport;
		return old;
	}

	private int selfPort = DEFAULT_SELF_PORT;
	public int getSelfPort() { return this.selfPort; }
	public int setSelfPort(int port) {
		int old = this.selfPort;
		this.selfPort = port;
		return old;
	}

	private int selfPortRange = DEFAULT_SELF_PORT_RANGE;
	public int getSelfPortRange() { return this.selfPortRange; }
	public int setSelfPortRange(int range) {
		int old = this.selfPortRange;
		this.selfPortRange = range;
		return old;
	}

	private int contactPort = DEFAULT_CONTACT_PORT;
	public int getContactPort() { return this.contactPort; }
	public int setContactPort(int port) {
		int old = this.contactPort;
		this.contactPort = port;
		return old;
	}

	private boolean doUPnPNATTraversal = DEFAULT_DO_UPNP_NAT_TRAVERSAL;
	public boolean getDoUPnPNATTraversal() { return this.doUPnPNATTraversal; }
	public boolean setDoUPnPNATTraversal(boolean flag) {
		boolean old = this.doUPnPNATTraversal;
		this.doUPnPNATTraversal = flag;
		return old;
	}

	private String directoryType = DEFAULT_DIRECTORY_TYPE;
	public String getDirectoryType() { return this.directoryType; }
	public String setDirectoryType(String type) {
		String old = this.directoryType;
		this.directoryType = type;
		return old;
	}

	private Class valueClass = DEFAULT_VALUE_CLASS;
	public Class getValueClass() { return this.valueClass; }
	public Class setValueClass(Class clz) {
		Class old = this.valueClass;
		this.valueClass = clz;
		return old;
	}

	private String workingDirectory = DEFAULT_WORKING_DIR;
	public String getWorkingDirectory() { return this.workingDirectory; }
	public String setWorkingDirectory(String dir) {
		String old = this.workingDirectory;
		this.workingDirectory = dir;
		return old;
	}

	private boolean doExpire = DEFAULT_DO_EXPIRE;
	public boolean getDoExpire() { return this.doExpire; }
	public boolean setDoExpire(boolean flag) {
		boolean old = this.doExpire;
		this.doExpire = flag;
		return old;
	}

	private long maximumTTL = DEFAULT_MAXIMUM_TTL;
	public long getMaximumTTL() { return this.maximumTTL; }
	public long setMaximumTTL(long ttl) {
		long old = this.maximumTTL;
		this.maximumTTL = ttl;
		return old;
	}

	private long defaultTTL = DEFAULT_DEFAULT_TTL;
	public long getDefaultTTL() { return this.defaultTTL; }
	public long setDefaultTTL(long ttl) {
		long old = this.defaultTTL;
		this.defaultTTL = ttl;
		return old;
	}

	private boolean multipleValuesForASingleKey = DEFAULT_MULTIPLE_VALUES_FOR_A_SINGLE_KEY;
	public boolean getMultipleValuesForASingleKey() { return this.multipleValuesForASingleKey; }
	public boolean setMultipleValuesForASingleKey(boolean flag) {
		boolean old = this.multipleValuesForASingleKey;
		this.multipleValuesForASingleKey = flag;
		return old;
	}

	private int numSpareRootCandidates = DEFAULT_NUM_SPARE_ROOT_CANDIDATES;
	public int getNumSpareRootCandidates() { return this.numSpareRootCandidates; }
	public int setNumSpareRootCandidates(int num) {
		int old = this.numSpareRootCandidates;
		this.numSpareRootCandidates = num;
		return old;
	}

	private int numReplica = DEFAULT_NUM_REPLICA;
	public int getNumReplica() { return this.numReplica; }
	public int setNumReplica(int num) {
		int old = this.numReplica;
		this.numReplica = num;
		return old;
	}

	private boolean rootDoesReplication = DEFAULT_ROOT_DOES_REPLICATION;
	public boolean getRootDoesReplication() { return this.rootDoesReplication; }
	public boolean setRootDoesReplication(boolean flag) {
		boolean old = this.rootDoesReplication;
		this.rootDoesReplication = flag;
		return old;
	}

	private int numNodesAskedToTransfer = DEFAULT_NUM_NODES_ASKED_TO_TRANSFER;
	public int getNumNodesAskedToTransfer() { return this.numNodesAskedToTransfer; }
	public int setNumNodesAskedToTransfer(int num) {
		int old = this.numNodesAskedToTransfer;
		this.numNodesAskedToTransfer = num;
		return old;
	}

	private int numTimesGets = DEFAULT_NUM_TIMES_GETS;
	public int getNumTimesGets() { return this.numTimesGets; }
	public int setNumTimesGets(int num) {
		int old = this.numTimesGets;
		this.numTimesGets = num;
		return old;
	}

	// Note:
	// Replication does not work correctly with "memcached" if reputOnReplica is false,
	// because local cache does not support "memcached".
	private boolean doReputOnReplicas = DEFAULT_DO_REPUT_ON_REPLICAS;
	public boolean getDoReputOnReplicas() { return this.doReputOnReplicas; }
	public boolean setDoReputOnReplicas(boolean flag) {
		boolean old = this.doReputOnReplicas;
		this.doReputOnReplicas = flag;
		return old;
	}

	private boolean doReputOnRequester = DEFAULT_DO_REPUT_ON_REQUESTER;
	public boolean getDoReputOnRequester() { return this.doReputOnRequester; }
	public boolean setDoReputOnRequester(boolean flag) {
		boolean old = this.doReputOnRequester;
		this.doReputOnRequester = flag;
		return old;
	}

	private long reputInterval = DEFAULT_REPUT_INTERVAL;
	/**
	 * Get the interval between reputs.
	 */
	public long getReputInterval() { return this.reputInterval; }
	/**
	 * Set the interval between reputs.
	 *
	 * @param interval interval in msec. 0 or less than 0 prohibits reputs.
	 */
	public long setReputInterval(long interval) {
		long old = this.reputInterval;
		this.reputInterval = interval;
		return old;
	}

	private double reputIntervalPlayRatio = DEFAULT_REPUT_INTERVAL_PLAY_RATIO;
	/**
	 * Get the play of interval between reputs.
	 */
	public double getReputIntervalPlayRatio() { return this.reputIntervalPlayRatio; }
	/**
	 * Set the play of interval between reputs.
	 *
	 * @param ratio play of interval in msec.
	 */
	public double setReputIntervalPlayRatio(double ratio) {
		double old = this.reputIntervalPlayRatio;
		this.reputIntervalPlayRatio = ratio;
		return old;
	}

	private boolean useTimer = DEFAULT_USE_TIMER_INSTEAD_OF_THREAD;
	public boolean getUseTimerInsteadOfThread() { return this.useTimer; }
	public boolean setUseTimerInsteadOfThread(boolean flag) {
		boolean old = this.useTimer;
		this.useTimer = flag;
		return old;
	}

	private String valueEncoding = DEFAULT_VALUE_ENCODING;
	public String getValueEncoding() { return this.valueEncoding; }
	public String setValueEncoding(String encoding) {
		String old = this.valueEncoding;
		this.valueEncoding = encoding;
		return old;
	}

	private long reqFreeMem = DEFAULT_REQUIRED_FREE_MEMORY_TO_PUT;
	public long getRequiredFreeMemoryToPut() { return this.reqFreeMem; }
	public long setRequiredFreeMemoryToPut(long mem) {
		long old = this.reqFreeMem;
		this.reqFreeMem = mem;
		return old;
	}

	private String selfHost = null;
		// does not have the default value and could be set by an application
	public String getSelfAddress() { return this.selfHost; }
	public String setSelfAddress(String host) {
		String old = this.selfHost;
		this.selfHost = host;
		return old;
	}
	
	/////////////////////////////////////////////////////////
	//    廣瀬が追加
	//    通信方法変更用のフラグ管理
	////////////////////////////////////////////////////////
	public enum commFlag{
		Permit,//通常通信
		Relay,//中継のみ許可
		Reject//通信拒否
	}
	private commFlag communicateMethodFlag=commFlag.Permit;
	public commFlag getCommunicateMethodFlag(){ return this.communicateMethodFlag; }
	public commFlag setCommunicateMethodFlag(commFlag newFlag){
		commFlag old = this.communicateMethodFlag;
		this.communicateMethodFlag = newFlag;
		return old;
	}
	
	public final int REJECT_NODE_NUMBER = 10;
	private int rejectNodeNumber = 0;
	private ID rejectID[] = new ID[REJECT_NODE_NUMBER];
	public int getRejectNodeNumber(){ return rejectNodeNumber; }
	public void setRejectID(ID newID){
		rejectID[rejectNodeNumber++] = newID;
	}
	public boolean checkRejectNode(ID checkID){
		for(int i = 0 ; i < rejectNodeNumber ; i++){
			if(rejectID[i].equals(checkID))
				return true;
		}
		return false;
	}
	
}
