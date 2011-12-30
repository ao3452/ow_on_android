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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import mypackage.C;

import ow.SessionID.SessionID;
import ow.dht.ByteArray;
import ow.dht.DHT;
import ow.id.ID;
import ow.id.IDAddressPair;
import ow.messaging.Message;
import ow.messaging.Tag;

/**
 * An utility class to create messages for DHT.
 */
public class DHTMessageFactory {
	public static Message getGetMessage(IDAddressPair src, ID[] keys) {
		int tag = Tag.GET.getNumber();
		return new Message(src, tag, (Serializable)keys);
	}

	public static Message getPutMessage(IDAddressPair src,
			DHT.PutRequest[] requests,
			long ttl, ByteArray hashedSecret, int numReplica) {
		int tag = Tag.PUT.getNumber();
		return new Message(src, tag, (Serializable)requests, ttl, hashedSecret, numReplica);
	}

	public static Message getRemoveMessage(IDAddressPair src,
			DHT.RemoveRequest[] requests,
			ByteArray hashedSecret, int numReplica) {
		int tag = Tag.REMOVE.getNumber();
		return new Message(src, tag, (Serializable)requests, hashedSecret, numReplica);
	}

	public static Message getDHTReplyMessage(IDAddressPair src, Set[] existedValues) {
		int tag = Tag.DHT_REPLY.getNumber();
		return new Message(src, tag, (Serializable)existedValues);
	}

	public static Message getReqTransferMessage(IDAddressPair src) {
		int tag = Tag.REQ_TRANSFER.getNumber();
		return new Message(src, tag);
	}

	public static Message getPutValueInfoMessage(IDAddressPair src, Map/*<ID,Set<ValueInfo<V>>>*/ keyValuesMap) {
		int tag = Tag.PUT_VALUEINFO.getNumber();
		return new Message(src, tag, (Serializable)keyValuesMap);
	}
	
	/*
	 * �ʹߡ�¿����Ѥˤ��ƿ̾�̿������Ѥ�����å����������Τ���δؿ�
	 * ƿ̾�̿��Ǥ�Message.contents[]����Ȥϰʲ��ι����ǹͤ��Ƥ����2011.10.01���ߡ�
	 * contents[0] : header
	 * contents[1] : body
	 * contents[2] : hash
	 * contents[3] : type
	 */
	
	/**
	 * ƿ̾ϩ���ۥ�å��������������ؿ�
	 * @param src	�����Ԥξ�����������
	 * @param header	ƿ̾ϩ���۾���Υإå�
	 * @return	overlay weaver����᤹���å�����
	 */
	public static Message getConstructMessage(IDAddressPair src, byte[] header){
		int tag = Tag.RELAY.getNumber();
		return new Message(src, tag, (Serializable)header, null, null, C.TYPE_CONSTRUCTION);
	}
	
	/**
	 * ƿ̾ϩ������å��������������ؿ�
	 * @param src	�����Ԥξ���
	 * @param header	ƿ̾ϩ����������ݻ�����إå�
	 * @return
	 */
	public static Message getRepairMessage(IDAddressPair src, byte[] header){
		int tag = Tag.RELAY.getNumber();
		return new Message(src, tag, (Serializable)header, null, null, C.TYPE_REPAIR);
	}
	
	/**
	 * ƿ̾�̿��ѥ�å��������������ؿ�
	 * @param src
	 * @param body
	 * @param hashValue
	 * @return
	 */
	public static Message getCommunicateMessage(IDAddressPair src, byte[] body, byte[] primeKey){
		int tag = Tag.RELAY.getNumber();
		return new Message(src, tag, null, (Serializable) body, primeKey, C.TYPE_COMMUNICATION);
	}
	
	/**
	 * �Ź�����ʤ��ѥ�å��������������ؿ�
	 * @param src
	 * @param body
	 * @param hashValue
	 * @return
	 */
	public static Message getCommunicateRelayMessage(IDAddressPair src, byte[] body, byte[] primeKey){
		int tag = Tag.RELAY.getNumber();
		return new Message(src, tag, null, (Serializable) body, primeKey, C.TYPE_COMMUNICATION_RELAY);
	}
	
	/**
	 * ��ѵ����ѥ�å��������������ؿ�
	 * @param src
	 * @param body
	 * @param hashValue
	 * @return
	 */
	public static Message getCommunicateRejectMessage(IDAddressPair src, byte[] body, byte[] primeKey){
		int tag = Tag.RELAY.getNumber();
		return new Message(src, tag, null, (Serializable) body, primeKey, C.TYPE_COMMUNICATION_REJECT);
	}
	
	/**
	 * λ�������ѥ�å��������������ؿ�
	 * @param src
	 * @param body
	 * @param hashValue
	 * @return
	 */
	public static Message getChangeApproveMessage(IDAddressPair src, byte[] body, byte[] primeKey){
		int tag = Tag.RELAY.getNumber();
		return new Message(src, tag, null, (Serializable) body, primeKey, C.TYPE_CHANGE_APPROVE);
	}
}
