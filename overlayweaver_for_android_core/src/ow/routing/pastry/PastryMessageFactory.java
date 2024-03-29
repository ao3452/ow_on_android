/*
 * Copyright 2006 National Institute of Advanced Industrial Science
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

import ow.id.IDAddressPair;
import ow.messaging.Message;
import ow.messaging.Tag;
import ow.routing.plaxton.RoutingTableRow;

public class PastryMessageFactory {
	public static Message getUpdateRoutingTableMessage(IDAddressPair src,
			IDAddressPair[] nodes, IDAddressPair[] smallerLeafSet, IDAddressPair[] largerLeafSet) {
		int tag = Tag.UPDATE_ROUTING_TABLE.getNumber();
		return new Message(src, tag, nodes, smallerLeafSet, largerLeafSet);
	}

	public static Message getReqLeafSetMessage(IDAddressPair src) {
		int tag = Tag.REQ_LEAF_SET.getNumber();
		return new Message(src, tag);
	}

	public static Message getRepLeafSetMessage(IDAddressPair src,
			IDAddressPair[] smallerLeafSet, IDAddressPair[] largerLeafSet) {
		int tag = Tag.REP_LEAF_SET.getNumber();
		return new Message(src, tag, smallerLeafSet, largerLeafSet);
	}

	public static Message getReqRoutingTableRowMessage(IDAddressPair src,
			int rowIndex) {
		int tag = Tag.REQ_ROUTING_TABLE_ROW.getNumber();
		return new Message(src, tag, rowIndex);
	}

	public static Message getRepRoutingTableRowMessage(IDAddressPair src,
			RoutingTableRow row) {
		int tag = Tag.REP_ROUTING_TABLE_ROW.getNumber();
		return new Message(src, tag, row);
	}
}
