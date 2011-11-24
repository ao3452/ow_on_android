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

package ow.messaging;

//import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import ow.util.Color;

public final class Tag {
	//
	// implementation
	//

	private static volatile int lastNumber = 0;
		// this declaration has to be before the protocol declarations (PING, ACK, ...)

	private final int number;
		// in a message, only 1 byte is effective though this member "number" is 4 byte.
	private final String name;
	private boolean toBeReportedToStatCollector;
	private final Color color;

	protected Tag(int i, String name, boolean toBeReportedToStatCollector, Color color) {
		lastNumber = i;
		this.number = lastNumber++;
		this.name = name;
		this.toBeReportedToStatCollector = toBeReportedToStatCollector;
		this.color = color;

		// for Overlay Visualizer
		colorTable.put(this.number, color);

		tagTable.put(this.number, this);
		nameTable.put(this.number, name);
	}

	protected Tag(String name, boolean toBeReportedToStatCollector, Color color) {
		this.number = lastNumber++;
		this.name = name;
		this.toBeReportedToStatCollector = toBeReportedToStatCollector;
		this.color = color;

		// for Overlay Visualizer
		colorTable.put(this.number, color);

		tagTable.put(this.number, this);
		nameTable.put(this.number, name);
	}

	public int getNumber() { return this.number; }
	public String getName() { return this.name; }
	public Color getColor() { return this.color; }

	//
	// for Overlay Visualizer
	//

	private static final Map<Integer,Color> colorTable = new HashMap<Integer,Color>();

	public static Color getColor(int number) {
		Color ret = colorTable.get(number);
		if (ret == null) {
			ret = Color.BLACK;
		}

		return ret;
	}

	public static boolean toBeReportedToStatCollector(int tag) {
		Tag o = getTagByNumber(tag);

		if (o != null) {
			return o.toBeReportedToStatCollector;
		}

		return true;
	}

	//
	// for debug
	//

	private static final Map<Integer,Tag> tagTable = new HashMap<Integer,Tag>();
	private static final Map<Integer,String> nameTable = new HashMap<Integer,String>();

	public static Tag getTagByNumber(int number) {
		return tagTable.get(number);
	}

	public static String getNameByNumber(int number) {
		String ret = nameTable.get(number);
		if (ret == null) {
			ret = String.valueOf(number);
		}

		return ret;
	}

	//
	// kinds of a message
	//

	// for messaging layer (UDP hole punching)
	public static final Tag PUNCH_HOLE_REQ = new Tag(0, "PUNCH_HOLE_REQ", false, Color.GRAY);
	public static final Tag PUNCH_HOLE_REP = new Tag("PUNCH_HOLE_REP", false, Color.GRAY);

	// routing
	public static final Tag PING = new Tag("PING", true, Color.GRAY);
	public static final Tag ACK = new Tag("ACK", true, Color.GRAY);

	// iterative routing
	public static final Tag ITE_ROUTE_NONE = new Tag("ROUTE_NONE", true, Color.BLACK);
	public static final Tag ITE_ROUTE_INVOKE = new Tag("ROUTE_INVOKE", true, Color.BLACK);
	public static final Tag ITE_ROUTE_JOIN = new Tag("ROUTE_JOIN", true, Color.GRAY);
	public static final Tag ITE_ADJUST_LAST_HOP_REQ = new Tag("ADJUST_LAST_HOP_REQ", true, Color.BLACK);
	public static final Tag ITE_ADJUST_LAST_HOP_REP = new Tag("ADJUST_LAST_HOP_REP", true, Color.BLACK);
	public static final Tag ITE_TERMINATE_NONE = new Tag("TERMINATE_NONE", true, Color.BLACK);
	public static final Tag ITE_TERMINATE_INVOKE = new Tag("TERMINATE_INVOKE", true, Color.BLACK);
	public static final Tag ITE_TERMINATE_JOIN = new Tag("TERMINATE_JOIN", true, Color.GRAY);
	public static final Tag ITE_REPLY = new Tag("REPLY", true, Color.GRAY);

	// recursive routing
	public static final Tag REC_ROUTE_NONE = new Tag("ROUTE_NONE", true, Color.BLACK);
	public static final Tag REC_ROUTE_INVOKE = new Tag("ROUTE_INVOKE", true, Color.BLACK);
	public static final Tag REC_ROUTE_JOIN = new Tag("ROUTE_JOIN", true, Color.GRAY);
	public static final Tag REC_TERMINATE_NONE = new Tag("TERMINATE_NONE", true, Color.BLACK);
	public static final Tag REC_TERMINATE_INVOKE = new Tag("TERMINATE_INVOKE", true, Color.BLACK);
	public static final Tag REC_TERMINATE_JOIN = new Tag("TERMINATE_JOIN", true, Color.GRAY);
	public static final Tag REC_ACK = new Tag("ACK", true, Color.GRAY);
	public static final Tag REC_RESULT = new Tag("RESULT", true, Color.GRAY);

	// for Routing algorithms

	// Linear Walker
	public final static Tag REQ_CONNECT = new Tag("REQ_CONNECT", true, Color.GRAY);
	public final static Tag REP_CONNECT = new Tag("REP_CONNECT", true, Color.GRAY);
	public final static Tag REQ_SUCCESSOR = new Tag("REQ_SUCCESSOR", true, Color.GRAY);
	public final static Tag REP_SUCCESSOR = new Tag("REP_SUCCESSOR", true, Color.GRAY);

	// Chord
	public final static Tag UPDATE_FINGER_TABLE = new Tag("UPDATE_FINGER_TABLE", true, Color.GRAY);
	public final static Tag ACK_FINGER_TABLE = new Tag("ACK_FINGER_TABLE", true, Color.GRAY);

	// Koorde
	public final static Tag REQ_PREDECESSOR = new Tag("REQ_PREDECESSOR", true, Color.GRAY);
	public final static Tag REP_PREDECESSOR = new Tag("REP_PREDECESSOR", true, Color.GRAY);

	// Pastry and Tapestry
	public final static Tag UPDATE_ROUTING_TABLE = new Tag("UPDATE_ROUTING_TABLE", true, Color.GRAY);

	// Tapestry
	public final static Tag MULTICAST_JOINING_NODE = new Tag("MULTICAST_JOINING_NODE", true, Color.GRAY);
	public final static Tag MULTICAST_ACK = new Tag("MULTICAST_ACK", true, Color.GRAY);
	public final static Tag NOTIFY_JOINING_NODE = new Tag("NOTIFY_JOINING_NODE", true, Color.GRAY);

	// Pastry
	public final static Tag REQ_LEAF_SET = new Tag("REQ_LEAF_SET", true, Color.GRAY);
	public final static Tag REP_LEAF_SET = new Tag("REP_LEAF_SET", true, Color.GRAY);
	public final static Tag REQ_ROUTING_TABLE_ROW = new Tag("REQ_ROUTING_TABLE_ROW", true, Color.GRAY);
	public final static Tag REP_ROUTING_TABLE_ROW = new Tag("REP_ROUTING_TABLE_ROW", true, Color.GRAY);

	// for DHT
	public final static Tag GET = new Tag("GET", true, Color.GRAY);
	public final static Tag PUT = new Tag("PUT", true, Color.GRAY);
	public final static Tag REMOVE = new Tag("REMOVE", true, Color.GRAY);
	public final static Tag DHT_REPLY = new Tag("DHT_REPLY", true, Color.GRAY);
	public final static Tag PUT_VALUEINFO = new Tag("PUT_VALUEINFO", true, Color.GRAY);
	public final static Tag REQ_TRANSFER = new Tag("REQ_TRANSFER", true, Color.GRAY);
	public final static Tag RELAY = new Tag("CONSTRUCT", true, Color.GRAY);

	
		// REQ_TRANSFER and REP_TRANSFER are for key-value pair transfer

	// for memcached
	public final static Tag PUT_ON_CONDITION = new Tag("PUT_ON_CONDITION", true, Color.GRAY);

	// for Mcast
	public final static Tag CONNECT = new Tag("CONNECT", true, Color.GRAY);
	public final static Tag ACK_CONNECT = new Tag("ACK_CONNECT", true, Color.GRAY);
	public final static Tag NACK_CONNECT = new Tag("NACK_CONNECT", true, Color.GRAY);
	public final static Tag DISCONNECT = new Tag("DISCONNECT", true, Color.GRAY);
	public final static Tag DISCONNECT_AND_REFUSE = new Tag("DISCONNECT_AND_REFUSE", true, Color.GRAY);
	public final static Tag MULTICAST = new Tag("MULTICAST", true, Color.GRAY);

	// for tunneling
	public static final Tag ENCAPSULATED = new Tag("ENCAPSULATED", false, Color.GRAY);
		// for ow.messaging.distemulator

	// for Messaging Statistics Collector
	public static final Tag MESSAGE_SENT = new Tag("MESSAGE_SENT", false, Color.GRAY);

	// for Overlay Visualizer
	public static final Tag DELETE_NODE = new Tag("DELETE_NODE", false, Color.GRAY);
	public static final Tag EMPHASIZE_NODE = new Tag("EMPHASIZE_NODE", false, Color.GRAY);
	public static final Tag MARK_ID = new Tag("MARK_ID", false, Color.GRAY);
	public static final Tag CONNECT_NODES = new Tag("CONNECT_NODES", false, Color.GRAY);
	public static final Tag DISCONNECT_NODES = new Tag("DISCONNECT_NODES", false, Color.GRAY);

	public static final Tag STAT_PING = new Tag("STAT_PING", false, Color.GRAY);
	public static final Tag STAT_ACK = new Tag("STAT_ACK", false, Color.GRAY);

	public static final Tag REQ_NEIGHBORS = new Tag("REQ_NEIGHBORS", false, Color.GRAY);
	public static final Tag REP_NEIGHBORS = new Tag("REP_NEIGHBORS", false, Color.GRAY);
}
