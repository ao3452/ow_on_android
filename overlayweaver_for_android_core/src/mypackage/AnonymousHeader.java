package mypackage;

import java.io.Serializable;

import javax.crypto.SecretKey;

import ow.id.ID;

public class AnonymousHeader implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ID nextID;
	private byte[] nextHeader;
	private SecretKey sharedKey;
	
	// ���󥹥ȥ饯��
	public AnonymousHeader(){}
	public AnonymousHeader(ID nextID, byte[] nextHeader, SecretKey secKey){
		this.nextID = nextID;
		this.nextHeader = nextHeader;
		this.sharedKey = secKey;
	}
	
	public void setNextID(ID nextID){
		this.nextID = nextID;
	}
	
	public ID getNextID(){
		return this.nextID;
	}
	
	public void setNextHeader(byte[] nextHeader){
		this.nextHeader = nextHeader;
	}
	
	public byte[] getNextHeader(){
		return this.nextHeader;
	}
	
	public void setSharedKey(SecretKey sharedKey){
		this.sharedKey = sharedKey;
	}
	
	public SecretKey getSharedKey(){
		return this.sharedKey;
	}
	
	/**
	 * ���Υ��饹��ʣ�����֤�
	 * ����Ū���ͤ�Ʊ����������ȤΥ��֥������ȤȤϰۤʤ륪�֥������ȤǤ���
	 * original.equal(replica)��true����
	 * original==replica��false�ʤΤϤ���
	 * @param original ���ԡ�������
	 * @return
	 */
	public static AnonymousHeader duplicate(AnonymousHeader original){
		if(original == null){
			return null;
		}
		AnonymousHeader replica = new AnonymousHeader();
		replica.setNextHeader(original.getNextHeader());
		replica.setNextID(original.getNextID());
		replica.setSharedKey(original.getSharedKey());
		return replica;
	}
	
	/**
	 * ���Υ��饹������ͤ򤹤٤�null�ˤ���
	 */
	public void clear(){
		this.nextID = null;
		this.nextHeader = null;
		this.sharedKey = null;
	}
}
