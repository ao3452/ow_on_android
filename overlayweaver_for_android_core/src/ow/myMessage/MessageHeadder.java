package ow.myMessage;

import java.io.Serializable;

import javax.crypto.SecretKey;

import ow.id.ID;

public class MessageHeadder implements Serializable{

	/**メッセージのヘッダ部
	 * 
	 */
	private static final long serialVersionUID = 8848716768229879625L;
	private ID nextID;//つぎのID
	private byte[] headderData;//暗号かされたヘッダのデータ
	private SecretKey s_key;//共通鍵
	
	public MessageHeadder()
	{
		this.nextID=null;
		this.headderData=null;
		this.s_key=null;
	}
	
	public MessageHeadder(ID id)
	{
		this.headderData = null;
		this.nextID = id;
		this.s_key = null;
		
	}
	
	public MessageHeadder(ID id,byte[] headderData){
		this.headderData = headderData;
		this.nextID=id;
		this.s_key=null;
		
	}
	public void setSecretKey(SecretKey key){
		this.s_key=key;
	}
	
	public ID getID(){
		ID retID = this.nextID;
		this.nextID=null;
		return retID;
	}
	
	
	
	
	
	
	
	

}
