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

package ow.mcast.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ow.id.ID;
import ow.id.IDAddressPair;
import ow.mcast.Mcast;
import ow.mcast.McastCallback;
import ow.mcast.McastConfiguration;
import ow.mcast.SpanningTreeChangedCallback;
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
import ow.routing.CallbackOnNodeFailure;
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
import ow.util.ClockJumpAwareSystem;
import ow.util.ExpiringMap;
import ow.util.ExpiringSet;

/**
 * An application-level multicast (ALM) implementation.
 * Delivery trees are constructed in the same way as Scribe,
 * but message dissemination is bi-directional, not one-way from a rendezvous point.
 */
public final class McastImpl implements Mcast {
	private final static Logger logger = Logger.getLogger("mcast");

	// messages
	private Message ackConnectMessage;
	private Message nackConnectMessage;

	// members common to higher level services (DHT and Mcast)

	private McastConfiguration config;
	private RoutingAlgorithmConfiguration algoConfig;

	private MessagingProvider msgProvider;
	private RoutingService routingSvc;
	private MessageReceiver receiver;
	private MessageSender sender;
	private boolean suspended = false;

	// members specific to McastImpl

	private NeighborTable neighborTable;
	protected GroupSet joinedGroupSet;
	private ExpiringSet<ID> connectRefuseGroupSet;
	private ExpiringMap<ID,MessagingAddress> connectProhibitedNeighborMap;

	private Thread refreshingThread = null;
	private Thread expiringThread = null;

	private MessagingAddress statCollectorAddress;

	// methods

	public McastImpl(McastConfiguration config, ID selfID /* possibly null */)
			throws Exception {
		this(Signature.getAllAcceptingApplicationID(), Signature.getAllAcceptingApplicationVersion(),
				config, selfID);
	}

	public McastImpl(short applicationID, short applicationVersion,
			McastConfiguration config, ID selfID /* possibly null */)
				throws Exception {
		// obtain messaging service
		byte[] messageSignature = Signature.getSignature(
				RoutingServiceFactory.getRoutingStyleID(config.getRoutingStyle()),
				RoutingAlgorithmFactory.getAlgorithmID(config.getRoutingAlgorithm()),
				applicationID, applicationVersion);

		this.msgProvider = MessagingFactory.getProvider(config.getMessagingTransport(), messageSignature);
		if (config.getSelfAddress() != null) {
			this.msgProvider.setSelfAddress(config.getSelfAddress());
		}

		MessagingConfiguration msgConfig = this.msgProvider.getDefaultConfiguration();
		msgConfig.setDoUPnPNATTraversal(config.getDoUPnPNATTraversal());

		this.receiver = this.msgProvider.getReceiver(
				msgConfig, config.getSelfPort(), config.getSelfPortRange());
		config.setSelfPort(this.receiver.getPort());	// correct config

		// obtain routing service
		RoutingAlgorithmProvider algoProvider = RoutingAlgorithmFactory.getProvider(config.getRoutingAlgorithm());
		this.algoConfig = algoProvider.getDefaultConfiguration();

		RoutingServiceProvider svcProvider = RoutingServiceFactory.getProvider(config.getRoutingStyle());
		RoutingService routingSvc = svcProvider.getService(
				svcProvider.getDefaultConfiguration(),
				this.msgProvider, this.receiver,
				algoProvider, this.algoConfig, selfID);

		// instantiate a RoutingAlgorithm in the routing service
		algoProvider.initializeAlgorithmInstance(this.algoConfig, routingSvc);

		init(config, routingSvc);
	}

	public McastImpl(McastConfiguration config, RoutingService routingSvc)
			throws Exception {
		init(config, routingSvc);
	}

	private void init(McastConfiguration config, RoutingService routingSvc)
			throws Exception {
		this.config = config;
		this.routingSvc = routingSvc;
		this.sender = this.routingSvc.getMessageSender();

		// initialize tables
		this.neighborTable = new NeighborTable(this, this.receiver, config.getNeighborExpiration());
		this.joinedGroupSet = new GroupSet();
		this.connectRefuseGroupSet = new ExpiringSet<ID>(config.getConnectRefuseDuration());
		this.connectProhibitedNeighborMap = new ExpiringMap<ID,MessagingAddress>(config.getConnectRefuseDuration());

		// prepare messages
		this.ackConnectMessage = McastMessageFactory.getAckConnectMessage(this.getSelfIDAddressPair());
		this.nackConnectMessage = McastMessageFactory.getNackConnectMessage(this.getSelfIDAddressPair());

		// initialize message handlers and callbacks
		prepareHandlers(this.routingSvc);
		prepareCallbacks(this.routingSvc);

		// initialize a Refresher and a NeighborExpirer
		if (config.getRefreshInterval() > 0) {
			this.refreshingThread = new Thread(new GroupRefresher());
			this.refreshingThread.setName("Refresher");
			this.refreshingThread.setDaemon(true);
			this.refreshingThread.start();
		}

		if (config.getNeighborExpireCheckInterval() > 0) {
			this.expiringThread = new Thread(new NeighborExpirer());
			this.expiringThread.setName("NeighborExpirer");
			this.expiringThread.setDaemon(true);
			this.expiringThread.start();
		}
	}

	public MessagingAddress joinOverlay(String hostAndPort, int defaultPort)
			throws UnknownHostException, RoutingException {
		MessagingAddress addr = this.msgProvider.getMessagingAddress(hostAndPort, defaultPort);
		this.initialize(addr);

		return addr;
	}

	public MessagingAddress joinOverlay(String hostAndPort)
			throws UnknownHostException, RoutingException {
		MessagingAddress addr = this.msgProvider.getMessagingAddress(hostAndPort);
		this.initialize(addr);

		return addr;
	}

	private void initialize(MessagingAddress addr)
			throws RoutingException {
		this.lastKey = this.getSelfIDAddressPair().getID();
		this.lastRoutingResult =
			this.routingSvc.join(addr);
	}

	public void clearRoutingTable() {
		this.routingSvc.leave();

		this.lastKey = null;
		this.lastRoutingResult = null;
	}

	public void clearMcastState() {
		this.neighborTable.clear();
		this.joinedGroupSet.clear();
		this.connectRefuseGroupSet.clear();
		this.connectProhibitedNeighborMap.clear();
	}

	//
	// methods to maintain groups
	//

	public void joinGroup(ID groupID)
			throws RoutingException {
		this.joinedGroupSet.add(groupID);

		/*
		if (this.neighborTable.hasParent(groupID)) {	// already connected
			return;
		}
		*/

		this.lastKey = groupID;
		this.lastRoutingResult =
			this.routingSvc.invokeCallbacksOnRoute(
					groupID,	/* target */
					1,		/* numNeighbors */
					null,	/* returnedValueContainer */
					null,	/* filter */
					0,		/* tag */
					null	/* args */);
	}

	public boolean leaveGroup(ID groupID) {
		// remove from the group set
		// and wait for expiration to trim branches
		boolean ret = this.joinedGroupSet.remove(groupID);

		synchronized (this.neighborTable) {
			if (!this.neighborTable.hasChild(groupID)) {	// there is no child
				this.disconnectParent(groupID);
			}

			if (ret) {
				invokeSpanningTreeChangedCallbacks(groupID);
			}
		}

		return ret;
	}

	public void leaveAllGroups() {
		ID[] joinedGroups = this.joinedGroupSet.toArray();
		if (joinedGroups != null) {
			for (ID groupID: joinedGroups) {
				this.leaveGroup(groupID);
			}
		}
	}

	//
	// methods for multicast
	//

	List<SpanningTreeChangedCallback> spanningTreeChangedCallbacks = new ArrayList<SpanningTreeChangedCallback>(1);

	public void addSpanningTreeChangedCallback(SpanningTreeChangedCallback callback) {
		this.spanningTreeChangedCallbacks.add(callback);
	}

	List<McastCallback> multicastCallbacks = new ArrayList<McastCallback>(1);

	public void addMulticastCallback(McastCallback callback) {
		synchronized (this.multicastCallbacks) {
			this.multicastCallbacks.add(callback);
		}
	}

	public void multicast(ID groupID, Serializable payload)
			throws RoutingException {
		if (!this.joinedGroupSet.contains(groupID)) {	// has not joined
			this.joinGroup(groupID);
		}

		Message msg = McastMessageFactory.getMulticastMessage(this.getSelfIDAddressPair(),
				groupID, config.getMulticastTTL(), payload);

		this.floodMessage(groupID, msg, null);

		invokeMulticastCallbacks(groupID, payload);
	}

	public synchronized void stop() {
		logger.log(Level.INFO, "Mcast#stop() called.");

		// stop daemons
		if (this.refreshingThread != null) {
			this.refreshingThread.interrupt();
			this.refreshingThread = null;
		}

		if (this.expiringThread != null) {
			this.expiringThread.interrupt();
			this.expiringThread = null;
		}

		// stop routing service
		if (this.routingSvc != null) {
			this.routingSvc.stop();
			this.routingSvc = null;
		}
	}

	public synchronized void suspend() {
		// suspend routing service
		this.routingSvc.suspend();

		// suspend daemons
		this.suspended = true;
	}

	public synchronized void resume() {
		// resume routing service
		this.routingSvc.resume();

		// resume daemons
		this.suspended = false;
		this.notifyAll();
	}

	//
	// methods specific to Mcast
	//

	public ID[] getJoinedGroups() {
		return this.joinedGroupSet.toArray();
	}

	public ID[] getGroupsWithSpanningTree() {
		ID[] ret = null;	

		Set<ID> groups = neighborTable.getGroupsWithSpanningTree();
		int n = groups.size();
		if (n > 0) {
			ret = new ID[n];
			groups.toArray(ret);
		}

		return ret;
	}

	public IDAddressPair getParent(ID groupID) {
		return neighborTable.getParent(groupID);
	}

	public IDAddressPair[] getChildren(ID groupID) {
		return neighborTable.getChildren(groupID);
	}

	//
	// methods are common to DHT and Mcast
	//

	public RoutingService getRoutingService() {
		return this.routingSvc;
	}

	public McastConfiguration getConfiguration() {
		return this.config;
	}

	public RoutingAlgorithmConfiguration getRoutingAlgorithmConfiguration() {
		return this.algoConfig;
	}

	public IDAddressPair getSelfIDAddressPair() {
		return this.routingSvc.getSelfIDAddressPair();
	}

	public MessagingAddress getStatCollectorAddress() {
		return this.statCollectorAddress;
	}

	public void setStatCollectorAddress(String host, int port) throws UnknownHostException {
		MessagingAddress addr = this.routingSvc.getMessagingProvider().getMessagingAddress(host, port);

		this.statCollectorAddress = addr;
		this.routingSvc.setStatCollectorAddress(addr);
	}

	private ID lastKey = null;
	public String getLastKeyString() {
		if (lastKey == null) return "null";

		return lastKey.toString();
	}

	private RoutingResult lastRoutingResult = null;
	public RoutingResult getLastRoutingResult() { return this.lastRoutingResult; }

	public String getRoutingTableString() {
		return this.routingSvc.getRoutingAlgorithm().getRoutingTableString();
	}

	private void prepareHandlers(RoutingService routingSvc) {
		MessageHandler handler;

		// CONNECT
		handler = new MessageHandler() {
			public Message process(Message msg) {
				Serializable[] contents = msg.getContents();
				final IDAddressPair selfIDAddress = McastImpl.this.getSelfIDAddressPair();
				final ID groupID = (ID)contents[0];
				final IDAddressPair from = msg.getSource();

				Message repMsg;
				if (connectRefuseGroupSet.contains(groupID)) {
					// refuse
					logger.log(Level.INFO, "Refuse to be connected on " + selfIDAddress.getAddress() + ". group ID: " + groupID);
					repMsg = McastImpl.this.nackConnectMessage;
				}
				else {
					// accept
					Runnable r = new Runnable() {
						public void run() {
							boolean parentChanged = false;

							synchronized (neighborTable) {
								// disconnect parent if parent changes
								IDAddressPair oldParent = neighborTable.getParent(groupID);
								if (!from.equals(oldParent)) {
									if (oldParent != null) {
										disconnectParent(groupID, oldParent);
									}
								}

								// register new parent
								parentChanged = neighborTable.registerParent(groupID, from);
							}

							if (parentChanged) {
								invokeSpanningTreeChangedCallbacks(groupID);

								receiver.getMessagingReporter().notifyStatCollectorOfConnectNodes(
										selfIDAddress,
										selfIDAddress.getID(), from.getID(),
										groupID.hashCode());
							}
						}
					};
					Thread t = new Thread(r);
					t.setName("CONNECT handler");
					t.setDaemon(true);
					t.start();

					repMsg = McastImpl.this.ackConnectMessage;
				}

				return repMsg;
			}
		};
		routingSvc.addMessageHandler(Tag.CONNECT.getNumber(), handler);

		// DISCONNECT
		handler = new MessageHandler() {
			public Message process(final Message msg) {
				Serializable[] contents = msg.getContents();
				final ID groupID = (ID)contents[0];

				synchronized (neighborTable) {
					boolean removed =
						neighborTable.removeChild(groupID, msg.getSource());

					if (removed) {
						invokeSpanningTreeChangedCallbacks(groupID);
					}
				}

				return null;
			}
		};
		routingSvc.addMessageHandler(Tag.DISCONNECT.getNumber(), handler);

		// DISCONNECT_AND_REFUSE
		handler = new MessageHandler() {
			public Message process(Message msg) {
				Serializable[] contents = msg.getContents();
				ID groupID = (ID)contents[0];

				IDAddressPair source = msg.getSource();

				connectProhibitedNeighborMap.put(groupID, source.getAddress());

				synchronized (neighborTable) {
					boolean removed =
						neighborTable.removeChild(groupID, source);

					if (removed) {
						invokeSpanningTreeChangedCallbacks(groupID);
					}
				}

				return null;
			}
		};
		routingSvc.addMessageHandler(Tag.DISCONNECT_AND_REFUSE.getNumber(), handler);

		// MULTICAST
		handler = new MessageHandler() {
			public Message process(Message msg) {
				Serializable[] contents = msg.getContents();
				ID groupID = (ID)contents[0];
				int ttl = (Integer)contents[1];
				Serializable payload = contents[2];

				logger.log(Level.INFO, "MULTICAST msg received. groupID: " + groupID
						+ ", ttl: " + ttl + ", from: " + msg.getSource().getAddress());

				// forward
				if (ttl > 0) {
					IDAddressPair source = msg.getSource();

					Message newMsg = McastMessageFactory.getMulticastMessage(
							McastImpl.this.getSelfIDAddressPair(),
							groupID, ttl - 1, payload);

					floodMessage(groupID, newMsg, source.getAddress());
				}

				// invoke callbacks
				invokeMulticastCallbacks(groupID, payload);

				return null;
			}
		};
		routingSvc.addMessageHandler(Tag.MULTICAST.getNumber(), handler);
	}

	private void prepareCallbacks(RoutingService routingSvc) {
		routingSvc.addCallbackOnRoute(new CallbackOnRoute() {
			public Serializable process(final ID groupID, int tag, Serializable[] args, CallbackResultFilter ignored, final IDAddressPair lastHop, final boolean onRootNode) {
				if (lastHop != null) {
					Runnable r = new Runnable() {
						public void run() {
							// disconnect parent if connected
							// to avoid loop
							// because being a parent is prior to being a child
							if (onRootNode) { 
								connectRefuseGroupSet.add(groupID);

								synchronized (neighborTable) {
									disconnectAndRefuseParent(groupID);
								}
							}
								// this technique solely could not prevent loop

							// connect with last hop
							// unless last hop is parent 
//							if (!lastHop.equals(neighborTable.getParent(groupID))) {
								connectWithChild(groupID, lastHop);
//							}
						}
					};

					Thread t = new Thread(r);
					t.setName("Connector");
					t.setDaemon(true);
					t.start();
				}

				return null;
			}
		});

		routingSvc.addCallbackOnNodeFailure(new CallbackOnNodeFailure() {
			public void fail(IDAddressPair failedNode) {
				Set<ID> changedGroups = new HashSet<ID>();

				synchronized (neighborTable) {
					changedGroups.addAll(neighborTable.removeChild(failedNode));
					changedGroups.addAll(neighborTable.removeParent(failedNode));

					for (ID changedGroup: changedGroups) {
						invokeSpanningTreeChangedCallbacks(changedGroup);
					}
				}

				// TODO: maintainance ???
			}
		});
	}

	//
	// Utility methods for multicast
	//

	private void floodMessage(ID groupID, Message msg, MessagingAddress from /* could be null */) {
		logger.log(Level.INFO, "floodMessage() called on "
				+ this.getSelfIDAddressPair().getAddress() + ". from: " + from);

		// forward to parent
		IDAddressPair parentIDAddr = neighborTable.getParent(groupID);
		if (parentIDAddr != null) {
			MessagingAddress parent = parentIDAddr.getAddress();
			if (!parent.equals(from)) {
				try {
					this.sender.send(parent, msg);
				}
				catch (IOException e) {
					logger.log(Level.WARNING, "Faild to flood to the parent.", e);
				}
			}
		}

		// forward to children
		IDAddressPair[] children = neighborTable.getChildren(groupID);
		if (children != null) {
			for (IDAddressPair child: children) {
				MessagingAddress childAddress = child.getAddress();
				if (!childAddress.equals(from)) {
					try {
						this.sender.send(childAddress, msg);
					}
					catch (IOException e) {
						logger.log(Level.WARNING, "Failed to flood to a child.", e);
					}
				}
			}
		}
	}

	private void invokeMulticastCallbacks(ID groupID, Serializable payload) {
		// invoke callbacks
		if (this.joinedGroupSet.contains(groupID)) {
			synchronized (this.multicastCallbacks) {
				for (McastCallback cb: this.multicastCallbacks) {
					cb.received(groupID, payload);
				}
			}
		}
	}

	//
	// Utility methods to maintain parent-child relationships
	//

	private void connectWithChild(ID groupID, IDAddressPair child) {
		MessagingAddress childAddress = child.getAddress();

		if (childAddress.equals(connectProhibitedNeighborMap.get(groupID))) {
			// prohibited to connect with the child
			return;
		}

		// send a CONNECT message
		Message connectMsg = McastMessageFactory.getConnectMessage(
				this.getSelfIDAddressPair(), groupID);

		try {
			Message ackMsg = sender.sendAndReceive(childAddress, connectMsg);

			if (ackMsg.getTag() == Tag.ACK_CONNECT.getNumber()) {
				logger.log(Level.INFO, "connectWithChild succeeded: " + child);

				synchronized (this.neighborTable) {
					boolean added =
						this.neighborTable.registerChild(groupID, child);

					if (added) {
						this.invokeSpanningTreeChangedCallbacks(groupID);
					}
				}
			}
			else if (ackMsg.getTag() == Tag.NACK_CONNECT.getNumber()) {
				connectProhibitedNeighborMap.put(groupID, childAddress);
			}
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Failed to connect to " + child);
		}
	}

	protected void disconnectParent(ID groupID) {
		IDAddressPair parent = this.neighborTable.getParent(groupID);
		if (parent == null)
			return;

		disconnectParent(groupID, parent);
	}

	protected void disconnectParent(ID groupID, IDAddressPair parent) {
		IDAddressPair selfIDAddress = this.getSelfIDAddressPair();

		// anyway, remove the parent from the table
		synchronized (this.neighborTable) {
			this.neighborTable.removeParent(groupID, parent);
		}

		// send a DISCONNECT message
		Message msg = McastMessageFactory.getDisconnectMessage(
				selfIDAddress, groupID);

		try {
			sender.send(parent.getAddress(), msg);
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Failed to disconnect from the parent: " + parent);
		}

		this.receiver.getMessagingReporter().notifyStatCollectorOfDisconnectNodes(
				selfIDAddress,
				selfIDAddress.getID(), parent.getID(),
				groupID.hashCode());
	}

	private void disconnectAndRefuseParent(ID groupID) {
		IDAddressPair selfIDAddress = this.getSelfIDAddressPair();

		IDAddressPair parent = this.neighborTable.getParent(groupID);
		if (parent == null)
			return;

		// anyway, remove the parent from the table
		synchronized (this.neighborTable) {
			this.neighborTable.removeParent(groupID, parent);

			this.invokeSpanningTreeChangedCallbacks(groupID);
		}

		// send a DISCONNECT_AND_REFUSE message
		Message msg = McastMessageFactory.getDisconnectAndRefuseMessage(
				selfIDAddress, groupID);

		try {
			sender.send(parent.getAddress(), msg);
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Failed to disconnect_and_refuse a parent: " + parent);
		}

		this.receiver.getMessagingReporter().notifyStatCollectorOfDisconnectNodes(
				selfIDAddress,
				selfIDAddress.getID(), parent.getID(),
				groupID.hashCode());
	}

	private void invokeSpanningTreeChangedCallbacks(ID groupID) {
		IDAddressPair parent = this.neighborTable.getParent(groupID);
		IDAddressPair[] children = this.neighborTable.getChildren(groupID);

		synchronized (this.spanningTreeChangedCallbacks) {
			for (SpanningTreeChangedCallback cb: this.spanningTreeChangedCallbacks) {
				cb.topologyChanged(groupID, parent, children);
			}
		}
	}

	//
	// Utilitiy methods to maintain groups which this node has joined
	//

	//
	// Daemons
	//

	private class GroupRefresher implements Runnable {
		public void run() {
			try {
				while (true) {
					// sleep
					Thread.sleep(config.getRefreshInterval());
					//ClockDelayAwareSystem.sleep(config.getRefreshInterval());

					synchronized (McastImpl.this) {
						while (suspended) {
							McastImpl.this.wait();
						}
					}

					// refresh
					ID[] groups = getJoinedGroups();
					if (groups != null) {
						for (ID group: groups) {
							try {
								joinGroup(group);
							}
							catch (RoutingException e) {
								logger.log(Level.WARNING, "Routing failed when joining " + group, e);
							}
						}
					}
				}
			}
			catch (InterruptedException e) {
				logger.log(Level.WARNING, "GroupRefresher interrupted and die.", e);
			}
		}
	}

	private class NeighborExpirer implements Runnable {
		public void run() {
			try {
				while (true) {
					// sleep
					ClockJumpAwareSystem.sleep(config.getNeighborExpireCheckInterval());

					while (suspended) {
						synchronized (McastImpl.this) {
							McastImpl.this.wait();
						}
					}

					// expire
					synchronized (neighborTable) {
						Set<ID> changedGroups = neighborTable.expire();

						for (ID groupID: changedGroups) {
							invokeSpanningTreeChangedCallbacks(groupID);
						}
					}
				}
			}
			catch (InterruptedException e) {
				logger.log(Level.WARNING, "NeighborExpirer interrupted and die.", e);
			}			
		}
	}
}
