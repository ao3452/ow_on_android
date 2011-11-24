package mypackage;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import edu.cityu.util.Point;

import ow.id.ID;

public class AnonymousRouteInfo {
	/*
	 * �����Ԥξ�����ݻ������ѿ�
	 */
	private LocalRouteInfo receiverInfo = null;

	/*
	 * ��ѥΡ��ɤξ�����ݻ������ѿ�����
	 */
	private ArrayList<LocalRouteInfo> relayInfo = null;

	/*
	 * ¿�ŰŹ���Ѥ��붦�̸����ݻ������ꥹ�� ArrayList�ΰ�̣�Ϥʤ��ʤʤ�Ȥʤ�������äȤ����������뤫���Τ�ʤ���
	 */
	private ArrayList<SecretKey> keysForAnonymity;

	/*
	 * ���ܤ���������Ρ��ɡ��ܼ�Ū��������Ǥ�̵������ѥΡ��ɡˤ�����祭���ΥХ�����
	 */
	private byte[] primaryKey = null;

	// �Ź沽���������פ�ʤ����⤷����
	public static final String DES_ALGORITHM = "DES";
	public static final String DESEDE_ALGORITHM = "DESede";
	public static final String AES_ALGORITHM = "AES";
	public static final String DEFAULT_ALGORITHM = AES_ALGORITHM; // �ǥե����

	/**
	 * �ʤˤ⤷�ʤ����󥹥ȥ饯�� ����λȤ������Ǥʤ����տ�Ū�ʥإå��κ����Τ�������Ѥ���ʡ�ƥ��ȡ��ǥХå��ѡ�
	 */
	public AnonymousRouteInfo() {
	}

	/**
	 * ƿ̾ϩ������������ ����Ū�ˤϼ�������Ū�˥Ρ��ɤȡ��ǿ������͡���ѥΡ��ɤο�������� �����ͤ˱�������ϩ�ξ�����ߤ���
	 * ��ѥΡ��ɤϺǿ����ʲ��η��˽�°����Ρ��ɤ��椫�����������򤹤�
	 * 
	 * @param targetID
	 *            ��Ū�Ȥ��������Ρ��ɤ�ID
	 * @oaran latestIDLevel ���Ȥ��ΤäƤ���ǿ����Υ�٥�
	 * @param relayCount
	 *            ���ꤹ����ѥΡ��ɤθĿ�
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	public AnonymousRouteInfo(final ID targetID, final int latestIDLevel, final int relayCount) {
		try {
			this.keysForAnonymity = new ArrayList<SecretKey>();
			this.relayInfo = new ArrayList<LocalRouteInfo>();

			KeyGenerator kg;
			kg = KeyGenerator.getInstance(DEFAULT_ALGORITHM);
			kg.init(128);
			SecretKey skey;

			/* ��Ū�Ρ��ɤξ������¸ */
			skey = kg.generateKey();
			this.receiverInfo = LocalRouteInfo.generateInfoAsReceiver(targetID, skey);
			this.keysForAnonymity.add(0, skey);

			/* ��ѥΡ��ɤξ������¸ */
			long seed = System.currentTimeMillis();
			ID nextID = targetID;

			for (int i = 0; i < relayCount; i++) {
				skey = kg.generateKey();

				// System.currentTimeMillis����seed���ͤ��Ѳ����ʤ��ΤǤ�������
				seed += 10;
				ID relayID = ID.getLatestLevelID(latestIDLevel, seed);

				// Ʊ���Ρ��ɤ�Ϣ³���뤳�Ȥ����
				if (relayID.equals(nextID)) {
					i--;
					continue;
				}
				nextID = relayID;

				this.relayInfo.add(LocalRouteInfo.generateInfoAsRelay(relayID, skey));
				this.keysForAnonymity.add(i, skey);
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * �äῧ�����������롣 ����Ū�ˤϼ�������Ū�˥Ρ��ɤȡ���ѥΡ��ɤ�ID���������¸���롣 ��˥ǥХå��Ѥδؿ��Ǥ���
	 * 
	 * @param targetID
	 *            ��Ū�Ȥ��������Ρ��ɤ�ID
	 * @param relayID
	 *            ��ѥΡ��ɤ�ID
	 */
	public AnonymousRouteInfo(final ID targetID, final ID[] relayID) {
		try {
			this.keysForAnonymity = new ArrayList<SecretKey>();
			this.relayInfo = new ArrayList<LocalRouteInfo>();
			
			KeyGenerator kg;
			kg = KeyGenerator.getInstance(DEFAULT_ALGORITHM);
			kg.init(128);
			SecretKey skey;

			/* ��Ū�Ρ��ɤξ������¸ */
			skey = kg.generateKey();
			this.receiverInfo = LocalRouteInfo.generateInfoAsReceiver(targetID, skey);
			this.keysForAnonymity.add(0, skey);

			/* ��ѥΡ��ɤξ������¸ */
			for (int i = 0; i < relayID.length; i++) {
				skey = kg.generateKey();
				ID id = relayID[i];
				this.relayInfo.add(LocalRouteInfo.generateInfoAsRelay(id, skey));
				this.keysForAnonymity.add(i, skey);
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * ���Υ��饹���ݻ���������ɽ������ʸ����򤫤��� ����Ū�ˤ� �������Ԥ˶ᤤ��ѥΡ��ɤ�ID�����̸������Хå����åץΡ��ɤ��ݤ�
	 * ����ѥΡ��ɤ�ID�����̸�����ѥΡ��ɤ��Ȥ�����ĥ ����ɽ������ʤϤ���
	 * 
	 * �ޤ���ǧ�ѤǤ��衣
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("relay node info \n");
		for (LocalRouteInfo lrci : this.relayInfo) {
			sb.append(lrci.toString());
			sb.append("\n");
		}
		sb.append("receiver node info \n");
		sb.append(this.receiverInfo.toString());
		return sb.toString();
	}

	/**
	 * ���Υ��饹���ݻ���������ID��ʬ�Τߤ�ɽ��
	 * 
	 * @return
	 */
	public String toPrintRoute() {
		StringBuffer sb = new StringBuffer();
		sb.append("relay node ID \n");
		for (LocalRouteInfo lrci : this.relayInfo) {
			sb.append(lrci.toStringID());
			sb.append("\n");
		}
		sb.append("receiver node ID \n");
		sb.append(this.receiverInfo.toStringID());
		return sb.toString();
	}

	/**
	 * ƿ̾ϩ����������ɽ������ʸ�����ɽ�� �����Ԥ˶ᤤ��ѥΡ��ɤ���Ϥ��ޤꡢ�����Ρ��ɤޤǤη�ϩ��ɽ������ �ʤ��ޤν�ϡ�
	 */
	public void printRouteInfo() {
		//System.out.println("ƿ̾ϩ����ɽ��");
		//System.out.println(this.toString() + "\n"); // �Ǹ�˲��Ԥ򤤤줿���ä�
	}

	/**
	 * ���Υ��饹���ݻ����Ƥ�����󤫤�ƿ̾ϩ���۾���ʽ����ǤϤʤ��ˤ��������
	 * 
	 * @return
	 */
	public AnonymousMessage getConstructMessage(Point masterKey) {
		try {
			// �����Ԥ���������������
			SecretKey secKey = this.receiverInfo.getSharedKey();
			ID nextID = null;
			byte[] nextHeader = null;
			AnonymousHeader header = new AnonymousHeader(nextID, nextHeader, secKey);

			// �����Ԥ�����Ǥ���褦�������Ԥ�ID�ǰŹ沽
			byte[] byteHeader = MyUtility.object2Bytes(header);
			ID receiverID = this.receiverInfo.getNodeID();
			nextHeader = CipherTools.encryptByIBEPadding(byteHeader, receiverID.toString(), masterKey);

			// ��ѼԤ���������������
			nextID = receiverID;
			for (int index = this.relayInfo.size() - 1; index >= 0; index--) {
				LocalRouteInfo localInfo = relayInfo.get(index);
				secKey = localInfo.getSharedKey();

				// ��ѼԤ��������إå���������
				header = new AnonymousHeader(nextID, nextHeader, secKey);
				byteHeader = MyUtility.object2Bytes(header);

				// ��ѼԤ�����Ǥ�����ǰŹ沽
				nextID = localInfo.getNodeID();
				nextHeader = CipherTools.encryptByIBEPadding(byteHeader, nextID.toString(), masterKey);
			}
			AnonymousMessage ret = new AnonymousMessage();
			ret.setHeader(nextHeader);

			// �祭������¸
			this.primaryKey = nextHeader;

			return ret;
		}
		// �桼�������ǥ��顼�������뤳�Ȥ�̵�����餳���ǥ��顼��ߤ��٤���
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	// �̾�ϥ��������ѥΡ��ɤ��������뤬�ƥ��ȤǤϰ����ˤ����Τ�
	// ���ꤷ����ѤΥΡ��ɤ����ꤹ��
	// setReceiverInfo��setRelayNodeInfo�Ϥ��Τ���Υ��饹
	/**
	 * �ƥ��ȤΤ��������¸�� ¿�ŰŹ沽���줿�إå��������������ꤹ�� ¾�Υ��饹�ǥإå�����ɬ�פ�����ʤ��Υ��饹�Ǻ���褦���ѹ����٤����⡩��
	 * 
	 * @param receiverInfo
	 */
	public void setReceiverInfo(LocalRouteInfo receiverInfo) {
		this.receiverInfo = receiverInfo;
	}

	/**
	 * �ƥ��ȤΤ��������¸��
	 * 
	 * @param relayInfo
	 */
	public void setRelayNodeInfo(ArrayList<LocalRouteInfo> relayInfo) {
		this.relayInfo = relayInfo;
	}

	/**
	 * ¿����Ѥˤ�����"ľ��"���������ID��������뤿��δؿ�
	 * 
	 * @return
	 */
	public ID getLocalTargetID() {
		ID localTargetID = this.relayInfo.get(0).getNodeID();
		return localTargetID;
	}

	/**
	 * ¿����Ѥˤ�����"�º�"���������ID��������뤿��δؿ�
	 * 
	 * @return
	 */
	public ID getGlobalTargetID() {
		ID globalTargetID = this.receiverInfo.getNodeID();
		return globalTargetID;
	}

	/**
	 * ƿ̾ϩ�����Ѥ���륭���ν����������뤿��δؿ� index=0���������Ԥ˶ᤤ�Ρ��ɤȤζ�ͭ���Ǥ���
	 * index=size-1�������ԤȤζ�ͭ���Ȥʤ�
	 * 
	 * @return
	 */
	public ArrayList<SecretKey> getKeyList() {
		return this.keysForAnonymity;
	}

	/**
	 * ���Ρ��ɤ���������祭����������뤿��δؿ�
	 * 
	 * @return
	 */
	public byte[] getPrimaryKey() {
		return this.primaryKey;
	}
}
