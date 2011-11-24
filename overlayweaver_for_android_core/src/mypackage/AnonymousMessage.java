package mypackage;

import java.io.Serializable;

import ow.id.ID;
import ow.id.IDAddressPair;

/**
 * このクラスは匿名通信を行う際に利用されるメッセージを表します。
 * 匿名通信では以下の手続きを経て通信を行います
 * ・匿名路の構築
 * ・匿名路の修復
 * ・匿名性のある通信
 * 基本的には上三つの全てをこのクラスで実装したい
 * 
 * まだまだ構造を悩み中
 * 
 * @author nozomu
 *
 */

public class AnonymousMessage implements Serializable{

	
	private static final long serialVersionUID = 1L;
	
	private IDAddressPair dest;
	private IDAddressPair src;
	private byte[] constructInfo;
	private byte[] body;
	private byte[] primeKey;
	
	public AnonymousMessage(){}
	
	public AnonymousMessage getConstructMessage(ID nextID, byte[] header){
		AnonymousMessage am = new AnonymousMessage();
		return am;
	}
	
	/**
	 * このメッセージのヘッダを設定する
	 * @param nextID
	 */
	public void setHeader(byte[] header){
		this.constructInfo = header;
	}
	
	/**
	 * このメッセージのヘッダを返す（この関数は要らないかもね〜）
	 * @return
	 */
	public byte[] getHeader(){
		return this.constructInfo;
	}
	
	/**
	 * このメッセージの送信内容を設定する
	 * @param body
	 */
	public void setBody(byte[] body){
		this.body = body;
	}
	
	/**
	 * このメッセージのbodyを返す
	 * @return
	 */
	public byte[] getBody(){
		return this.body;
	}
	
	
}
