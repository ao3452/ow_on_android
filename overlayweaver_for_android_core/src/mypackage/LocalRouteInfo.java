package mypackage;

import javax.crypto.SecretKey;

import ow.id.ID;

/**
 * �����Ԥ���ƿ̾ϩ�ι��ۡס���ƿ̾ϩ�����פʤɤ����Ѥ��뤿��ξ���
 * ����Ū�ˤ�����ϩ�ζɽ�Ū�ʾ������¸����
 * �����ԤϤ��Υ��饹������ˤǤ⤷���ݻ����Ƥ�����
 * @author nozomu
 *
 */

public class LocalRouteInfo {
	private ID nodeID;
	private SecretKey sharedKey;
	
	/*
	 * ���Υ��饹���ݻ�����Ρ��ɤ�
	 * ����ѥΡ��ɡ���Хå����åס�	= 0
	 * ����ѥΡ��ɡʥХå����åס�	= 1
	 * �������Ρ���					= 2
	 * �Τɤ줫��ɽ��
	 */
	private int nodeType;
	
	/*
	 * ���󥹥ȥ饯����private�ˤ���٤����ʤ�����
	 */
	public LocalRouteInfo(ID id, SecretKey skey, int type){
		this.nodeID = id;
		this.sharedKey = skey;
		this.nodeType = type;
	}
	
	/**
	 * ƿ̾ϩ����������ѥΡ��ɤξ�����ݻ����륤�󥹥��󥹤���������
	 * @param id ��ѥΡ��ɤ�ID
	 * @param skey ��ѥΡ��ɤȶ�ͭ���붦�̸�
	 * @return ƿ̾ϩ�ζɽ�Ū�ʾ�����ݻ����륤�󥹥���
	 */
	static LocalRouteInfo generateInfoAsRelay(ID id, SecretKey skey){
		LocalRouteInfo lrci = new LocalRouteInfo(id, skey, 0);
		return lrci;
	}
	
	/**
	 * ƿ̾ϩ��������Хå����åץΡ��ɤξ�����ݻ����륤�󥹥��󥹤���������
	 * @param id ���ΥΡ��ɡʥХå����åץΡ��ɡˤ�ID
	 * @param skey ���ΥΡ��ɡʥХå����åץΡ��ɡˤȶ�ͭ���붦�̸�
	 * @return ƿ̾ϩ�ζɽ�Ū�ʾ�����ݻ����륤�󥹥���
	 */
	static LocalRouteInfo generateInfoAsBackup(ID id, SecretKey skey){
		LocalRouteInfo lrci = new LocalRouteInfo(id, skey ,1);
		return lrci;
	}
	
	/**
	 * ����ϩ������������Ρ��ɤξ�����ݻ����륤�󥹥��󥹤���������
	 * @param id ���ΥΡ��ɡʼ����Ρ��ɡˤ�ID
	 * @param skey ���ΥΡ��ɡʼ����Ρ��ɡˤȶ�ͭ���붦�̸�
	 * @return ƿ̾ϩ�ζɽ�Ū�ʾ�����ݻ����륤�󥹥���
	 */
	static LocalRouteInfo generateInfoAsReceiver(ID id, SecretKey skey){
		LocalRouteInfo lrci = new LocalRouteInfo(id, skey ,2);
		return lrci;
	}
	
	/**
	 * ���Υ��饹���ݻ����������ѥΡ��ɡ���Хå����åץΡ��ɡˤǤ��뤫�ɤ���Ƚ��
	 * @return ��ѥΡ��ɡ���Хå����åץΡ��ɡˤʤ�true������ʳ��ʤ�false
	 */
	public boolean isRelay(){
		if(this.nodeType == 0)
			return true;
		return false;
	}
	
	/**
	 * ���Υ��饹���ݻ�������󤬥Хå����åץΡ��ɤǤ��뤫�ɤ���Ƚ��
	 * @return �Хå����åץΡ��ɤʤ�true������ʳ��ʤ�false
	 */
	public boolean isBackup(){
		if(this.nodeType == 1)
			return true;
		return false;
	}
	
	/**
	 * ���Υ��饹���ݻ�������󤬼����Ρ��ɤǤ��뤫�ɤ���Ƚ��
	 * @return �����Ρ��ɤʤ�true������ʳ��ʤ�false
	 */
	public boolean isReceiver(){
		if(this.nodeType == 2)
			return true;
		return false;
	}
	
	/**
	 * ���Υ��饹���ݻ�����Ρ��ɤΥ����פ��ֵ�
	 * @return
	 * 	0:��ѥΡ��ɡ���Хå����åץΡ��ɡ�
	 * 	1:�Хå����åץΡ���
	 *	2:�����Ρ���
	 */
	public int getNodeType(){
		return this.nodeType;
	}
	
	/**
	 * ���Υ��饹���ݻ�����Ρ���ID���ֵ�
	 * @return �Ρ��ɤ�ID
	 */
	public ID getNodeID(){
		return this.nodeID;
	}
	
	/**
	 * ���Υ��饹���ݻ����붦ͭ�����ֵ�
	 * @return ���ΥΡ��ɤȤζ�ͭ��
	 */
	public SecretKey getSharedKey(){
		return this.sharedKey;
	}
	
	/**
	 * ���Υ��饹�ξ����ɽ������ʸ������֤�
	 * �������ɤ�;�Ϥ���
	 */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(this.nodeID.toHexString());
		sb.append(this.sharedKey.toString());
		sb.append(this.getNodeType());
		return sb.toString();
	}
	
	/**
	 * ���Υ��饹�����ID��ɽ��ʸ������֤�
	 * @return
	 */
	public String toStringID(){
		return this.nodeID.toString();
	}
}
