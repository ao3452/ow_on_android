package mypackage;

import javax.crypto.SecretKey;

import ow.messaging.MessagingAddress;

/**
 * ƿ̾�̿������Ρ��ɤ����å������������ä��塢���ΥΡ��ɤ����뤿��ν������������Ƥ��ߤ��륯�饹
 * ��å������������ä��塢�ƥΡ��ɤϥإå��򥭡���
 * �����ΥΡ��ɤξ���	: MessagingAddress dest
 * �����̸�				: SecretKey key
 * �����Ρ��ɤ��������	: byte[] sendHeader
 * ���Ź沽�����椫		: int decOrEnc
 * ���������ƿ̾ϩ�ֹ� : int number  // 1/3 ע�����ɲ�
 * �嵭��4�Ĥ��Ȥ߹�碌��Х�塼�Ȥ�����¸����
 * ���Υ��饹�ǤϥХ�塼�Ȥʤ륻�åȤ򥻥åȤȤ����ݻ����롣
 * 
 * ��ä�Ŭ�ڤʥ��饹̵̾�����ʡ�
 * 
 * @author nozomu
 *
 */

public class RelayProcessSet {
	
	/*
	 * byte[] sendHeader�������������Ǻ��Ǥ��롣
	 * �Ĥ��ΥΡ��ɤ��Ф��ơ����椷���إå����Ϥ�����hashCode��¹Ԥ��������ϥå����ͤ��Ϥ�����
	 * �����Ȥ���ͤ���ȥϥå����ͤ��Ϥ��������ɤ��褦�ʵ������롣
	 * 
	 * 2011.10.03
	 * �¸����Ʒ�̤��ɤ����ˤ��褦��
	 */
	
	private MessagingAddress dest;
	private SecretKey key;
	private byte[] primalKey;
	private int decOrEnc;
	private int number;
	
	public RelayProcessSet(MessagingAddress dest, SecretKey key, byte[] sendHeader, int decOrEnc , int number){
		this.dest = dest;
		this.key = key;
		this.primalKey = sendHeader;
		this.decOrEnc = decOrEnc;
		this.number = number;
	}
	
	public MessagingAddress getDestMessagingAddress(){
		return this.dest;
	}
	
	public SecretKey getSecretKey(){
		return this.key;
	}
	
	public byte[] getPrimalKey(){
		return this.primalKey;
	}
	
	public int getDecOrEnc(){
		return this.decOrEnc;
	}
	
	public int getNumber(){
		return this.number;
	}
}
