package ow.SessionID;

import java.io.Serializable;

import javax.crypto.SecretKey;

import ow.id.ID;
import ow.messaging.InetMessagingAddress;
import ow.messaging.MessagingAddress;

public class MessageInfo implements Serializable {

	/**
	 * SessionIDと対応付けてテーブルに保存する情報(情報は以下)
	 */
	private static final long serialVersionUID = -2781434384383204620L;
	private MessagingAddress Address;//次の宛先であるIPアドレス
	private boolean headderflag;//ヘッダを復号するノードかどうか
	private boolean receiverflag;//受信者かどうか
	private boolean senderflag;
	private SecretKey s_rkey ;//復号する共通鍵(受信者用)
	private SecretKey s_hkey ;//復号する共通鍵(ヘッダ用)
	private ID nextID;//次の宛先であるID(IPアドレス先がいなかった時はこれで再検索)
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

	public MessageInfo()//初期化
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
