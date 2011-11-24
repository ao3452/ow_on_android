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

package ow.routing.pastry;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import ow.id.ID;
import ow.id.IDAddressPair;
import ow.id.comparator.AlgoBasedTowardTargetIDAddrComparator;
import ow.id.comparator.AlgoBasedTowardTargetIDComparator;
import ow.messaging.Message;
import ow.messaging.MessageHandler;
import ow.messaging.Tag;
import ow.routing.RoutingAlgorithmConfiguration;
import ow.routing.RoutingContext;
import ow.routing.RoutingService;
import ow.routing.plaxton.Plaxton;
import ow.routing.plaxton.RoutingTableRow;
import ow.util.ClockJumpAwareSystem;
import ow.util.Timer;

public final class Pastry extends Plaxton {
	// messages
	private final Message reqLeafSetMessage;

	private PastryConfiguration config;

	private LeafSet leafSet = null;

	private RoutingTableMaintainer routingTableMaintainer;
	private Thread routingTableMaintainerThread = null;

	protected Pastry(RoutingAlgorithmConfiguration conf, RoutingService routingSvc)
			throws InvalidAlgorithmParameterException {
		super(conf, routingSvc);

		this.config = (PastryConfiguration)conf;

		if (this.config.getUseLeafSet()) {
			if (routingSvc != null) {
				this.leafSet = new LeafSet(this, this.config.getIDSizeInByte() * 8,
						routingSvc.getSelfIDAddressPair(), this.config.getLeafSetOneSideSize());
			}
			else {
				logger.log(Level.SEVERE, "routingSvc is null. test?");
			}
		}

		this.reqLeafSetMessage = PastryMessageFactory.getReqLeafSetMessage(selfIDAddress);

		// initialize message handlers
		this.prepareHandlers();

		// does not invoke routing table maintainer
		//startRoutingTableMaintainer();
	}

	private synchronized void startRoutingTableMaintainer() {
		if (!this.config.getDoPeriodicRoutingTableMaintenance()) return;

		if (this.routingTableMaintainer != null) return;	// to avoid multiple invocations

		this.routingTableMaintainer = new RoutingTableMaintainer();

		if (config.getUseTimerInsteadOfThread()) {
			Timer.getSingletonTimer().schedule(this.routingTableMaintainer,
					System.currentTimeMillis(), true /*isDaemon*/);
		}
		else if (this.routingTableMaintainerThread == null){
			this.routingTableMaintainerThread = new Thread(this.routingTableMaintainer);
			this.routingTableMaintainerThread.setName("RoutingTableMaintainer on " + selfIDAddress.getAddress());
			this.routingTableMaintainerThread.setDaemon(true);
			this.routingTableMaintainerThread.start();
		}
	}

	private synchronized void stopRoutingTableMaintainer() {
		if (this.routingTableMaintainerThread != null) {
			this.routingTableMaintainerThread.interrupt();
			this.routingTableMaintainerThread = null;
		}
	}

	public void reset() {
		super.reset();
		this.leafSet.clear();
	}

	public synchronized void stop() {
		super.stop();
		this.stopRoutingTableMaintainer();
	}

	public synchronized void suspend() {
		super.suspend();
		this.stopRoutingTableMaintainer();
	}

	public synchronized void resume() {
		super.resume();
		this.startRoutingTableMaintainer();
	}

	private final BigInteger ID_SPACE_SIZE = BigInteger.ONE.shiftLeft(idSizeInBit);
	private final BigInteger HALF_ID_SPACE_SIZE = BigInteger.ONE.shiftLeft(idSizeInBit - 1);

	public BigInteger distance(ID to, ID from) {
		BigInteger toInt = to.toBigInteger();
		BigInteger fromInt = from.toBigInteger();

		BigInteger distance = toInt.subtract(fromInt);
		if (distance.compareTo(BigInteger.ZERO) < 0) {
			distance = distance.add(ID_SPACE_SIZE);
		}

		if (distance.compareTo(HALF_ID_SPACE_SIZE) < 0) {	// if d < 2^(ID_SIZE - 1)
			// d = d * 2
			distance = distance.shiftLeft(1);
		}
		else {
			// d = ((2 ^ ID_SIZE) - d) * 2 - 1
			distance = ID_SPACE_SIZE.subtract(distance);
			distance = distance.shiftLeft(1);
			distance = distance.subtract(BigInteger.ONE);
		}

		return distance;
	}

	public IDAddressPair[] closestTo(ID target, int maxNum, RoutingContext cxt) {
		SortedSet<IDAddressPair> nodesByLeafSet = null;
		IDAddressPair[] nodesByRoutingTable = null;
		boolean leafSetPreferred = false;
		int num = 0;

		// leaf set
		if (this.leafSet != null) {
			nodesByLeafSet = this.leafSet.closestNodes(target, maxNum);
				// includes this node itself
			num = nodesByLeafSet.size();

			// check if leaf set covers the target
			IDAddressPair targetIDAddress = IDAddressPair.getIDAddressPair(target, null);
			if (target.equals(selfIDAddress.getID())
					|| this.leafSet.coversWithSmallerSet(targetIDAddress)
					|| this.leafSet.coversWithLargerSet(targetIDAddress)) {
				leafSetPreferred = true;
			}
		}

		// routing table (Plaxton et al.)
		if (!leafSetPreferred || num < maxNum) {
			nodesByRoutingTable = super.closestTo(target, maxNum - num, cxt);
				// includes this node itself,

			// decide which is preferred, leaf set or routing table
			if (!leafSetPreferred
					&& nodesByLeafSet != null && nodesByLeafSet.size() > 0
					/*&& nodesByRoutingTable != null*/ && nodesByRoutingTable.length > 0) {
				ID headOfLeafSet = nodesByLeafSet.first().getID();
				ID headOfRoutingTable = nodesByRoutingTable[0].getID();

				Comparator<ID> toTargetComparator = new AlgoBasedTowardTargetIDComparator(this, target);
				if (toTargetComparator.compare(headOfLeafSet, headOfRoutingTable) <= 0) {
					leafSetPreferred = true;
				}
			}
		}

		// merge
		List<IDAddressPair> result = new ArrayList<IDAddressPair>();
		if (leafSetPreferred) {
			if (nodesByLeafSet != null)
				result.addAll(nodesByLeafSet);
			if (nodesByRoutingTable != null)
				for (IDAddressPair p: nodesByRoutingTable) result.add(p);
		}
		else {
			if (nodesByRoutingTable != null)
				for (IDAddressPair p: nodesByRoutingTable) result.add(p);
			if (result.size() < maxNum)
				if (nodesByLeafSet != null)
					result.addAll(nodesByLeafSet);
		}

		num = Math.min(result.size(), maxNum);

		IDAddressPair[] ret = new IDAddressPair[num];
		for (int i = 0; i < num; i++) {
			ret[i] = result.get(i);
		}
		return ret;
	}

// (almost) original algorithm
//	public IDAddressPair[] closestTo(ID target, int maxNum, RoutingContext cxt) {
//		SortedSet<IDAddressPair> setByLeafSet = null;
//		List<IDAddressPair> listByLeafSet = null;
//		IDAddressPair[] arrayByRoutingTable = null;
//
//		// leaf set
//		if (this.leafSet != null) {
//			IDAddressPair targetIDAddress = IDAddressPair.getIDAddressPair(target, null);
//
//			if (target.equals(selfIDAddress.getID())
//					|| this.leafSet.coversWithSmallerSet(targetIDAddress)
//					|| this.leafSet.coversWithLargerSet(targetIDAddress)) {
//				// leaf set covers the target
//				setByLeafSet = this.leafSet.closestNodes(target, maxNum);
//					// includes this node itself
//			}
//		}
//
//		int num = 0;
//
//		if (setByLeafSet != null) {
//			listByLeafSet = new ArrayList<IDAddressPair>();
//			listByLeafSet.addAll(setByLeafSet);
//
//			num = listByLeafSet.size();
//		}
//
//		// routing table (Plaxton et al.)
//		if (num < maxNum) {
//			arrayByRoutingTable = super.closestTo(target, maxNum - num, cxt);
//				// includes this node itself,
//
//				// It's possible for the obtained list not to be in order of ID distance.
//				// This can cause a loop in routing.
//				// Plaxton table traversal necessarily yields this incorrectly-ordered list. 
//				// It is very easy to sort the list of nodes instead of traversal
//				// but Plaxton table loses its meaning if do it. Isn't it?
//		}
//
//		if (listByLeafSet == null) return arrayByRoutingTable;
//
//		// merge
//		if (arrayByRoutingTable != null) {
//			for (IDAddressPair p: arrayByRoutingTable) {
//				if (!setByLeafSet.contains(p)) {
//					listByLeafSet.add(p);
//				}
//			}
//
//			num = listByLeafSet.size();
//		}
//
//		IDAddressPair[] ret = new IDAddressPair[num];
//		for (int i = 0; i < num; i++) {
//			ret[i] = listByLeafSet.get(i);
//		}
//		return ret;
//	}

	protected List<IDAddressPair> traverseDownward(int rowIndex, int startingCol, ID target, int maxNum) {
		Comparator<IDAddressPair> comparator =
			new AlgoBasedTowardTargetIDAddrComparator(this, target);
		List<IDAddressPair> results = new ArrayList<IDAddressPair>();

		traverseDownward(results, comparator, rowIndex, startingCol, target, maxNum);
			// results include this node itself

		return results;
	}

	private void traverseDownward(List<IDAddressPair> results,
			Comparator<IDAddressPair> comparator,
			int rowIndex, int startingCol, ID target, int maxNum) {
		RoutingTableRow row = routingTable.getRow(rowIndex);
		int rowSize = row.size();

		if (pickCloseEntry(results, comparator, maxNum,
				target, row, rowIndex, startingCol, 0)) {
			return;
		}

		for (int i = 1; i < rowSize; i++) {
			int colIndex = startingCol + i;
			if (colIndex < rowSize) {
//System.out.println("[" + Integer.toHexString(rowIndex) + ":" + Integer.toHexString(colIndex) + "]");
				if (pickCloseEntry(results, comparator,
						maxNum, target, row, rowIndex, colIndex, +1)) {
					break;
				}
			}

			colIndex = startingCol - i;
			if (colIndex >= 0) {
//System.out.println("[" + Integer.toHexString(rowIndex) + ":" + Integer.toHexString(colIndex) + "]");
				if (pickCloseEntry(results, comparator,
						maxNum, target, row, rowIndex, colIndex, -1)) {
					break;
				}
			}
		}
	}

	// a specialized version of traverseDownward(): startingCol is 0.
	private void traverseDownwardFromLeftToRight(List<IDAddressPair> results,
			int rowIndex, ID target, int maxNum) {
		RoutingTableRow row = routingTable.getRow(rowIndex);
		int rowSize = row.size();

		for (int i = 0; i < rowSize; i++) {
			if (pickCloseEntry(results, null, maxNum, target, row, rowIndex, i, +1)) break;
		}
	}

	// a specialized version of traverseDownward(): startingCol is row.size() - 1.
	private void traverseDownwardFromRightToLeft(List<IDAddressPair> results,
			int rowIndex, ID target, int maxNum) {
		RoutingTableRow row = routingTable.getRow(rowIndex);
		int rowSize = row.size();

		for (int i = rowSize - 1; i >= 0; i--) {
			if (pickCloseEntry(results, null, maxNum, target, row, rowIndex, i, -1)) break;
		}
	}

	private boolean pickCloseEntry(List<IDAddressPair> results, Comparator<IDAddressPair> comparator,
			int maxNum, ID target, RoutingTableRow row,
			int rowIndex, int colIndex, int exploringSide) {
		IDAddressPair entry = row.get(colIndex);
		if (entry == null) return false;

		if (entry.equals(selfIDAddress) && rowIndex + 1 < idSizeInDigit) {
			// recursive call
			if (exploringSide > 0) {
				traverseDownwardFromLeftToRight(results,
						rowIndex + 1, target, maxNum - results.size());
			}
			else if (exploringSide < 0) {
				traverseDownwardFromRightToLeft(results,
						rowIndex + 1, target, maxNum - results.size());
			}
			else {
				traverseDownward(results, comparator,
						rowIndex + 1, colIndex, target, maxNum - results.size());
			}
		}

		results.add(entry);	// can be selfIDAddress on the lowest row

		if (results.size() >= maxNum)
			return true;
		else
			return false;
	}

	protected List<IDAddressPair> traverseUpward(int rowIndex, int maxNum) {
		// Plaxton#traverseUpward() is provided here
		// mainly for getting enough number of neighbors in Tapestry.
		// In Pastry, this method will not be called usually
		// because a leaf set provides enough number of neighbors.

		List<IDAddressPair> results = new ArrayList<IDAddressPair>();
		ID selfID = selfIDAddress.getID();

		outer:
		for (int i = rowIndex - 1; i >= 0; i--) {
			RoutingTableRow row = routingTable.getRow(i);
			int rowSize = row.size();
			int digit = getDigit(selfID, i);

			int colIndex;
			IDAddressPair entry;
			for (int j = 0; j < rowSize; j++) {
				if (j != 0) {
					colIndex = digit + j;
					if (colIndex < rowSize) {
						if ((entry = row.get(colIndex)) != null) {
							results.add(entry);
							if (results.size() >= maxNum) break outer;
						}
					}
				}

				colIndex = (digit - 1 - j) % rowSize;
				if (colIndex >= 0) {
					if ((entry = row.get(colIndex)) != null) {
						results.add(entry);
						if (results.size() >= maxNum) break outer;
					}
				}
			}
		}

		return results;
	}

	public void join(IDAddressPair joiningNode, IDAddressPair lastHop, boolean isFinalHop) {
//System.out.println("join:");
//System.out.println("  lastHop:     " + (lastHop == null ? "(null)" : lastHop));
//System.out.println("  self:        " + selfIDAddress);
//System.out.println("  joiningNode: " + joiningNode);
		super.join(joiningNode, lastHop, isFinalHop);

		if (lastHop == null) {
			// this node is the joining node
			// and do not have to send a UPDATE_ROUTING_TABLE message
			return;
		}

		int startRow, endRow;

		if (lastHop.equals(joiningNode)) {
			// on the hop next to the joining node
			startRow = 0;
		}
		else {
			int lastMatchBits = ID.matchLengthFromMSB(lastHop.getID(), joiningNode.getID());
			int lastMatchDigits = lastMatchBits / digitSize;

			startRow = lastMatchDigits + 1;
//System.out.println("lastMatchDigits: " + lastMatchDigits);
		}

		int selfMatchBits = ID.matchLengthFromMSB(selfIDAddress.getID(), joiningNode.getID());
		int selfMatchDigits = selfMatchBits / digitSize;

		endRow = selfMatchDigits;
		if (isFinalHop || endRow >= idSizeInDigit) {
			// on the last hop (or overflow)
			endRow = idSizeInDigit - 1;
		}

//System.out.println("  selfMatchDigits: " + selfMatchDigits);
//System.out.println("  startRow: " + startRow);
//System.out.println("  endRow:   " + endRow);
		if (startRow < idSizeInDigit && startRow <= endRow) {
			int rowSetWidth = endRow - startRow + 1;
//System.out.println("rowSetWidth:     " + rowSetWidth);

			Set<IDAddressPair> nodeSet = routingTable.getNodes(startRow, rowSetWidth);
			nodeSet.remove(joiningNode);	// is not required
			IDAddressPair[] nodes = new IDAddressPair[nodeSet.size()];
			nodeSet.toArray(nodes);

			IDAddressPair[] smallerLeafSet = null;
			IDAddressPair[] largerLeafSet = null;
			if (this.leafSet != null && isFinalHop) {
				smallerLeafSet = this.leafSet.getArrayOfSmallerSet(joiningNode);
				largerLeafSet = this.leafSet.getArrayOfLargerSet(joiningNode);
			}

			Message reqMsg = PastryMessageFactory.getUpdateRoutingTableMessage(selfIDAddress, nodes, smallerLeafSet, largerLeafSet);

			try {
				sender.send(joiningNode.getAddress(), reqMsg);
			}
			catch (IOException e) {
				logger.log(Level.WARNING, "Failed to send a UPDATE_ROUNTING_TABLE message: " + joiningNode.getAddress(), e);

				fail(joiningNode);
			}
		}
	}

	public void touch(IDAddressPair from) {
		// processes not described in the paper
		super.touch(from);

		if (this.leafSet != null)
			this.leafSet.add(from);
	}

	public void forget(IDAddressPair failedNode) {
		super.forget(failedNode);

		if (this.leafSet != null) {
			this.leafSet.remove(failedNode);
			this.checkAndFillLeafSet();
		}
	}

	public String getRoutingTableString() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.routingTable.toString());

		if (this.leafSet != null) {
			sb.append("\n");
			sb.append("leaf set: ");
			sb.append(this.leafSet.toString());
		}

		return sb.toString();
	}

	public String getRoutingTableHTMLString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<h4>Plaxton Routing Table</h5>\n");
		sb.append(this.routingTable.toHTMLString());

		if (this.leafSet != null) {
			sb.append("<h4>Leaf Set</h4>");
			sb.append(this.leafSet.toHTMLString());
		}

		return sb.toString();
	}

	private void checkAndFillLeafSet() {
		if (this.leafSet.coversEntireRing()) {
			// current leaf set covers entire ring
			return;
		}

		int oneSideSize = this.leafSet.getOneSideSize();
		if (this.leafSet.getNumberOfSmallerNodes() < oneSideSize) {
			// smaller half of leaf set is not full
			IDAddressPair smallest = this.leafSet.getSmallestNode();
			if (smallest != null) {
				this.requestLeafSet(smallest);
			}
		}

		if (this.leafSet.getNumberOfLargerNodes() < oneSideSize) {
			// larger half of leaf set is not full
			IDAddressPair largest = this.leafSet.getLargestNode();
			if (largest != null) {
				this.requestLeafSet(largest);
			}
		}
	}

	private void requestLeafSet(IDAddressPair target) {
		Message reqMsg = this.reqLeafSetMessage;
		Message repMsg;
		try {
			repMsg = sender.sendAndReceive(target.getAddress(), reqMsg);
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Failed to send or receive REQ/REP_LEAF_SET message.", e);
			fail(target);
			return;
		}

		Serializable[] contents = repMsg.getContents();
		IDAddressPair[] smallerLeafSet = (IDAddressPair[])contents[0];
		IDAddressPair[] largerLeafSet = (IDAddressPair[])contents[1];

		this.leafSet.merge(smallerLeafSet);
		this.leafSet.merge(largerLeafSet);

		// merge nodes into routing table
		// processes not described in the paper
		for (IDAddressPair p: smallerLeafSet) this.routingTable.merge(p);
		for (IDAddressPair p: largerLeafSet) this.routingTable.merge(p);
	}

	protected void prepareHandlers() {
		super.prepareHandlers();

		MessageHandler handler;

		// UPDATE_ROUTING_TABLE
		handler = new MessageHandler() {
			public Message process(final Message msg) {
				Serializable[] contents = msg.getContents();
				IDAddressPair[] nodes = (IDAddressPair[])contents[0];
				IDAddressPair[] smallerLeafSet = (IDAddressPair[])contents[1];
				IDAddressPair[] largerLeafSet = (IDAddressPair[])contents[2];

				Set<IDAddressPair> nodesINotice = new HashSet<IDAddressPair>();

				logger.log(Level.INFO, "UPDATE_ROUTING_TABLE received: # of nodes: " + nodes.length + " on " + selfIDAddress.getAddress());

				// merge row set
				for (IDAddressPair p: nodes) {
					routingTable.merge(p);
				}

				nodesINotice.addAll(routingTable.getNodes(0, Integer.MAX_VALUE));

				if (leafSet != null) {
					// merge leaf set
					if (smallerLeafSet != null)
						leafSet.merge(smallerLeafSet);

					if (largerLeafSet != null)
						leafSet.merge(largerLeafSet);

					// fill leaf set
					checkAndFillLeafSet();

					for (IDAddressPair p: leafSet.getArrayOfLargerSet()) {
						if (p != null) nodesINotice.add(p);
					}

					for (IDAddressPair p: leafSet.getArrayOfSmallerSet()) {
						if (p != null) nodesINotice.add(p);
					}
				}

				nodesINotice.remove(selfIDAddress);
				nodesINotice.remove(msg.getSource());

				// send JOINED messages to nodes in the received table

				// Note: Pastry paper requires sending "resulting state to each of the nodes
				// found in its neighborhood set, leaf set, and routing table",
				// but the state can be large and is not suitable to send over network.
				// Instead, notify each node of appearance of this node by sending a PING message.
				for (IDAddressPair node: nodesINotice) {
					logger.log(Level.INFO, selfIDAddress.getAddress() + " sends a PING msg to " + node.getAddress());

					try {
						runtime.ping(sender, node);
					}
					catch (IOException e) {
						logger.log(Level.WARNING, "An IOException thrown while sending JOINED message.", e);
					}
				}

				return null;
			}
		};
		runtime.addMessageHandler(Tag.UPDATE_ROUTING_TABLE.getNumber(), handler);

		// REQ_LEAF_SET
		handler = new MessageHandler() {
			public Message process(Message msg) {
				IDAddressPair[] smallerLeafSet = leafSet.getArrayOfSmallerSet();
				IDAddressPair[] largerLeafSet = leafSet.getArrayOfLargerSet();

				Message repMsg = PastryMessageFactory.getRepLeafSetMessage(selfIDAddress,
						smallerLeafSet, largerLeafSet);

				return repMsg;
			}
		};
		runtime.addMessageHandler(Tag.REQ_LEAF_SET.getNumber(), handler);

		// REQ_ROUTING_TABLE_ROW
		handler = new MessageHandler() {
			public Message process(Message msg) {
				Serializable[] contents = msg.getContents();
				int rowIndex = (Integer)contents[0];

				RoutingTableRow row = routingTable.getRow(rowIndex);
				if (row.isEmpty()) row = null;

				Message repMsg = PastryMessageFactory.getRepRoutingTableRowMessage(selfIDAddress,
						row);

				return repMsg;
			}
		};
		runtime.addMessageHandler(Tag.REQ_ROUTING_TABLE_ROW.getNumber(), handler);
	}

	private final class RoutingTableMaintainer implements Runnable {
		public void run() {
			try {
				// initial sleep
				if (!config.getUseTimerInsteadOfThread()) {
					ClockJumpAwareSystem.sleep(config.getRoutingTableMaintenanceInterval());
				}

				while (true) {
					for (int rowIndex = 0; rowIndex < idSizeInDigit; rowIndex++) {
						// check if stopped or suspended
						synchronized (Pastry.this) {
							if (stopped || suspended) {
								Pastry.this.routingTableMaintainer = null;
								Pastry.this.routingTableMaintainerThread = null;
								break;
							}
						}

						// find a target
						RoutingTableRow row;
						IDAddressPair target = null;

						find_target:
						for (int i = rowIndex; i < idSizeInDigit; i++) {
							row = routingTable.getRow(rowIndex);
							if (row.isEmpty()) continue;

							int colCandidate = random.nextInt(1 << digitSize);
							target = null;

							for (int j = 0; j < (1 << digitSize); j++) {
								int index = (colCandidate + j) % (1 << digitSize);
								target = row.get(index);

								if (selfIDAddress.equals(target)) target = null;		// ignore self

								if (target != null) break find_target;
							}
						}

						if (target == null) {
							// no node found and try next row
							continue;
						}

						// send a request
						Message msg = PastryMessageFactory.getReqRoutingTableRowMessage(selfIDAddress,
								rowIndex);
						Message repMsg = null;
						try {
							repMsg = sender.sendAndReceive(target.getAddress(), msg);
						}
						catch (IOException e) {
							logger.log(Level.WARNING, "Failed to send or receive a REQ/REP_ROUTING_TABLE_ROW message.", e);
							fail(target);
							continue;
						}

						Serializable[] contents = repMsg.getContents();
						row = (RoutingTableRow)contents[0];

						logger.log(Level.INFO, selfIDAddress.getAddress()
								+ " received REP_ROUTING_TABLE_ROW: " + (row == null ? "(null)" : row));

						if (row != null) {
							routingTable.merge(row);

							// merge nodes in the row into leaf set
							// processes not described in the paper
							for (IDAddressPair node: row.getAllNodes()) {
								Pastry.this.leafSet.add(node);
							}
						}
					}	// for (int rowIndex = 0; ...

					// sleep
					this.sleep();

					if (config.getUseTimerInsteadOfThread()) return;
				}	// while (true)
			}
			catch (InterruptedException e) {
				logger.log(Level.WARNING, "RoutingTableMainainer interrupted and die.", e);
			}
		}

		private void sleep() throws InterruptedException {
			// sleep for 15 - 45 sec (in default)
			long interval = config.getRoutingTableMaintenanceInterval();
			double playRatio = config.getRoutingTableMaintenanceIntervalPlayRatio();
			double pauseRatio = 1.0 - playRatio + (playRatio * 2.0 * random.nextDouble());

			long sleepPeriod = (long)(interval * pauseRatio);
			if (config.getUseTimerInsteadOfThread()) {
				Timer.getSingletonTimer().schedule(this, System.currentTimeMillis() + sleepPeriod, true /*isDaemon*/);
			}
			else {
				ClockJumpAwareSystem.sleep(sleepPeriod);
			}
		}
	}

	public IDAddressPair[] getSuccessorlist() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
}
