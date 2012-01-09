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

package ow.dht;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Set;

import ow.id.ID;
import ow.id.IDAddressPair;
import ow.messaging.MessagingAddress;
import ow.routing.RoutingAlgorithmConfiguration;
import ow.routing.RoutingException;
import ow.routing.RoutingResult;
import ow.routing.RoutingService;

/**
 * A DHT interface.
 * 
 * @param <V>
 *            type of a value put on the DHT.
 */
public interface DHT<V extends Serializable> {
	//
	// Overlay and DHT management
	//

	/**
	 * Joins an a routing network by contacting the specified host. An
	 * application program has to call this method explicitly.
	 * 
	 * @return MessagingAddress of the contact.
	 */
	MessagingAddress joinOverlay(String hostAndPort, int defaultPort) throws UnknownHostException, RoutingException;

	/**
	 * Joins a routing network by contacting the specified host. An application
	 * program has to call this method explicitly.
	 * 
	 * @return MessagingAddress of the contact.
	 */
	MessagingAddress joinOverlay(String hostAndPort) throws UnknownHostException, RoutingException;

	/**
	 * Leaves the routing network by clearing the routing table.
	 */
	void clearRoutingTable();

	/**
	 * Clears all DHT-related states, e.g. key-value pairs.
	 */
	void clearDHTState();

	//
	// DHT-related operations
	//

	/**
	 * Puts a pair of the specified key and value. Note that multiple values
	 * associated to the same key can be stored, but same (equals()) values are
	 * unified. Sets a secret with ({@link #setHashedSecretForPut(ByteArray)
	 * setHashedSecretForPut(ByteArray)}) method if you want to remove the
	 * key-value pair later.
	 * 
	 * @return values which have existed. null if not stored.
	 */
	Set<ValueInfo<V>> put(ID key, V value) throws Exception;

	/*
	 * Puts pairs of the specified key and values.
	 */
	Set<ValueInfo<V>> put(ID key, V[] values) throws Exception;

	/**
	 * Puts multiple key-value pairs collectively.
	 * 
	 * @param requests
	 *            put requests.
	 * @return values which have existed. an element of the array is null if not
	 *         stored.
	 */
	Set<ValueInfo<V>>[] put(PutRequest<V>[] requests) throws Exception;

	/**
	 * Returns a set of values associated to the specified key.
	 * 
	 * @return null if no value was found.
	 */
	Set<ValueInfo<V>> get(ID key) throws RoutingException;

	/**
	 * Performs multiple get operations collectively.
	 */
	Set<ValueInfo<V>>[] get(ID[] keys);

	/**
	 * Removes a pair of the specified key and value.
	 * 
	 * @param hashedSecret
	 *            a secret hashed with SHA1.
	 * @return the associated value if found. null if not found.
	 */
	Set<ValueInfo<V>> remove(ID key, V[] value, ByteArray hashedSecret) throws RoutingException;

	/**
	 * Removes a pair of the specified key and hashed value.
	 * 
	 * @param valueHash
	 *            hash of the value which is removed.
	 *            SHA1(value.toString().getBytes("UTF-8")).
	 * @param hashedSecret
	 *            a secret hashed with SHA1.
	 * @return the associated value if found. null if not found.
	 */
	Set<ValueInfo<V>> remove(ID key, ID[] valueHash, ByteArray hashedSecret) throws RoutingException;

	/**
	 * Removes all values associated to the specified key.
	 * 
	 * @param hashedSecret
	 *            a secret hashed with SHA1.
	 * @return all found values. null if not found.
	 */
	Set<ValueInfo<V>> remove(ID key, ByteArray hashedSecret) throws RoutingException;

	/**
	 * Performs multiple remove operations collectively.
	 */
	Set<ValueInfo<V>>[] remove(RemoveRequest<V>[] requests, ByteArray hashedSecret);

	/**
	 * Sets a secret for following put operations. The stored pair can be
	 * removed later with the specified secret.
	 */
	ByteArray setHashedSecretForPut(ByteArray hashedSecret);

	/**
	 * Sets TTL (millisecond) for following put operations.
	 */
	long setTTLForPut(long ttl);

	//
	// Node management
	//

	void stop();

	void suspend();

	void resume();

	//
	// Utilities
	//

	// specific to DHT
	Set<ID> getLocalKeys();

	Set<ValueInfo<V>> getLocalValues(ID key);

	Set<ID> getGlobalKeys();

	Set<ValueInfo<V>> getGlobalValues(ID key);

	// common to DHT and Mcast
	RoutingService getRoutingService();

	DHTConfiguration getConfiguration();

	RoutingAlgorithmConfiguration getRoutingAlgorithmConfiguration();

	IDAddressPair getSelfIDAddressPair();

	void setStatCollectorAddress(String host, int port) throws UnknownHostException;

	// for debug and tools
	String getLastKeyString();

	RoutingResult getLastRoutingResult();

	String getRoutingTableString();

	//
	// Container classes
	//

	public static class PutRequest<V> implements Serializable {
		private final ID key;
		private final V[] values;

		public PutRequest(ID key, V[] values) {
			this.key = key;
			this.values = values;
		}

		public ID getKey() {
			return this.key;
		}

		public V[] getValues() {
			return this.values;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{ key: ").append(this.key.toString());
			if (this.values != null) {
				sb.append(", value:");
				for (V v : this.values)
					sb.append(" ").append(v);
			}
			sb.append(" }");
			return sb.toString();
		}
	}

	public final static class RemoveRequest<V> extends PutRequest<V> {
		private final ID[] valueHash; // optional

		public RemoveRequest(ID key, V[] values) {
			super(key, values);
			this.valueHash = null;
		}

		public RemoveRequest(ID key, ID[] valueHash) {
			super(key, null);
			this.valueHash = valueHash;
		}

		public RemoveRequest(ID key) {
			super(key, null);
			this.valueHash = null;
		}

		public ID[] getValueHash() {
			return this.valueHash;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{ key: ").append(super.key.toString());
			if (super.values != null) {
				sb.append(", value:");
				for (V v : super.values)
					sb.append(" ").append(v);
			}
			else if (this.valueHash != null) {
				sb.append(", vlaueHash:");
				for (ID vh : this.valueHash)
					sb.append(" ").append(vh.toString());
			}
			sb.append(" }");
			return sb.toString();
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	//
	// The following code is added by Hiroyuki Tanaka
	//
	// �������鲼��ƿ̾�̿��Τ�������δؿ�
	//
	// ////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This is a setup method of Bifrost. In this method, communicate PKG and
	 * get MPK and my private key. Additionally generate some instances for
	 * speed-up in advance.
	 * 
	 * @return if there are no problem, it returns true.
	 */
	public boolean SetUp();

	/**
	 * ƿ̾�̿��򤪤��ʤ�����δؿ��Ǥ���ʽ���DHT�ˤ�ɬ�פʤ��ˡ� ƿ̾�̿��ǤϺǽ��ƿ̾ϩ�ι��ۤ�ɬ�פǤ��롣 ���δؿ��ϡ�ƿ̾ϩ�ι��ۤ�Ԥ���
	 * 
	 * @param targetID
	 *            ƿ̾�̿�ϩ��Ǥ�������������
	 * @param relayAmount
	 *            ��ѥΡ��ɤθĿ�
	 * @return ƿ̾ϩ���ۤ�������������true�򡢼��Ԥ�������false���֤�
	 * @throws Exception
	 */
	public boolean construct(ID targetID, int relayAmount);
	
	/**
	 * ƿ̾�̿���Ԥ�����δؿ���
	 * ƿ̾�̿��ǤϺǽ��ƿ̾ϩ���ۤ���ɬ�פ����롣
	 * ���δؿ�������ϩ���ۤ��롣
	 * �̾�ϡ�ƿ̾ϩ�����ѥΡ��ɤϥ���������򤹤뤬���տޤ�����ѥΡ��ɤ�����Ǥ���褦�ˤ����Τ����δֿ��Ǥ��롣
	 * @param targetID	ƿ̾ϩ��Ǥ��������������ID
	 * @param relayID	��ѥΡ��ɤ�ID
	 * @return��ƿ̾ϩ���ۤ�������������true�򡢼��Ԥ�������false���֤�
	 */
	public boolean constructTest(ID targetID, ID[] relayID);

	/**
	 * ƿ̾�̿��򤪤��ʤ��ؿ��Ǥ��� ���δؿ���¹Ԥ������ˡ�construct��¹Ԥ���ƿ̾ϩ���ۤ���ɬ�פ�����
	 * 
	 * @param targetID
	 *            ƿ̾�̿�ϩ��Ǥμºݤ�������
	 * @param mail
	 *            �ºݤ���������Ϥ�������
	 * @return ƿ̾�̿��������������ɤ��� �ʤȤ��äƤ����Τ�ʬ�����衢���ΥΡ��ɤ����줿���ɤ������饤���衢�������ʤ��͡���
	 */
	public boolean communicate(ID targetID, Object mail);
	
	/**
	 * ƿ̾ϩ������ܤ���Ρ��ɤ�Υæ�����Ȥ��˸ƤФ��ؿ���
	 * �����Ԥ��Ф��ƥΡ��ɤ�Υæ�������Ȥ������롣
	 * @param departureID��Υæ�����Ρ��ɤ�ID
	 */
	public void notice(ID departureID);

	public void resetDHT(ID selfID);
	
	/**
	 * ��Ū��ؤ�ƿ̾ϩ�����Ǥ�¸�ߤ��뤫�ɤ�����Ƚ�Ǥ���ؿ�
	 * @param strID	��Ū��ΥΡ��ɤ�ID�򤢤�魯ʸ����
	 * @return��ƿ̾ϩ��¸�ߤ����true���ʤ����false
	 */
	public boolean containsRoute(ID targetID);

	public IDAddressPair[] getRoutingTable();

	public IDAddressPair[] getSuccessorlist();

}
