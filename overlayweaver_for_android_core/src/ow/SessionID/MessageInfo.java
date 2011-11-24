package ow.SessionID;

import java.io.Serializable;

import javax.crypto.SecretKey;

import ow.id.ID;
import ow.messaging.InetMessagingAddress;
import ow.messaging.MessagingAddress;

public class MessageInfo implements Serializable {

	/**
	 * SessionID���б��դ��ƥơ��֥����¸�������(����ϰʲ�)
	 */
	private static final long serialVersionUID = -2781434384383204620L;
	private MessagingAddress Address;//���ΰ���Ǥ���IP���ɥ쥹
	private boolean headderflag;//�إå������椹��Ρ��ɤ��ɤ���
	private boolean receiverflag;//�����Ԥ��ɤ���
	private boolean senderflag;
	private SecretKey s_rkey ;//���椹�붦�̸�(��������)
	private SecretKey s_hkey ;//���椹�붦�̸�(�إå���)
	private ID nextID;//���ΰ���Ǥ���ID(IP���ɥ쥹�褬���ʤ��ä����Ϥ���ǺƸ���)
	private SessionID sid;
	private int direction;
	private int target_area;
	
	public int getTarget_area()
	{
		return target_area;
	}

	public void setTarget_area(int targetArea)
	{
		target_area = targetArea;
	}

	public MessageInfo()//�����
	{
		this.Address=null;
		this.headderflag=false;
		this.receiverflag=false;
		this.senderflag=false;
		this.s_hkey=null;
		this.s_rkey=null;
		this.nextID=null;
		this.sid = null;
		this.direction = -1;
	}
	
	public int getDirection()
	{
		return direction;
	}

	public void setDirection(int direction)
	{
		this.direction = direction;
	}

	public void SetAddress(MessagingAddress addr){
		this.Address = addr;
		return;
	}	
	
	public void SetHeadderflag(boolean flag ){
		this.headderflag = flag;
		return;		
	}
	
	public void SetReceiverflag(boolean flag){
		this.receiverflag = flag;
		return;
	}
	
	public void SetSenderflag(boolean flag){
		this.senderflag = flag;
		return;
	}

	public void SetSecretRKey(SecretKey key){
		this.s_rkey = key;
		return;
	}
	public void SetSecretHKey(SecretKey key){
		this.s_hkey = key;
		return;
	}
	
	public void SetNextID(ID id){
		this.nextID=id;
		return;
	}
	
	public void SetSessionID(SessionID id){
		this.sid=id;
		return;
	}
	
///
	
	public MessagingAddress getAddress(){
		return this.Address;
	}	
	
	public boolean getHeadderflag(){
		return this.headderflag;		
	}
	
	public boolean getReceiverflag(){
		return this.receiverflag;
	}
	
	public boolean getSenderflag(){
		return this.senderflag;
	}
	
	public SecretKey getSecretRKey(){
		return this.s_rkey;
		}
	public SecretKey getSecretHKey(){
		return this.s_hkey;
		}
	public ID getNextID(){
		return this.nextID;
	}
	public SessionID getSessionID(){
		return this.sid;
	}
	

}
