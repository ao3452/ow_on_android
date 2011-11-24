package mypackage;

import javax.crypto.SecretKey;

import ow.id.ID;

/**
 * 送信者が「匿名路の構築」、「匿名路を修復」などで利用するための情報
 * 具体的には得迷路の局所的な情報を保存する
 * 送信者はこのクラスを配列にでもして保持しておけ。
 * @author nozomu
 *
 */

public class LocalRouteInfo {
	private ID nodeID;
	private SecretKey sharedKey;
	
	/*
	 * このクラスが保持するノードが
	 * ・中継ノード（非バックアップ）	= 0
	 * ・中継ノード（バックアップ）	= 1
	 * ・受信ノード					= 2
	 * のどれかを表す
	 */
	private int nodeType;
	
	/*
	 * コンストラクタ（privateにするべきかなぁ？）
	 */
	public LocalRouteInfo(ID id, SecretKey skey, int type){
		this.nodeID = id;
		this.sharedKey = skey;
		this.nodeType = type;
	}
	
	/**
	 * 匿名路を構成する中継ノードの情報を保持するインスタンスを生成する
	 * @param id 中継ノードのID
	 * @param skey 中継ノードと共有する共通鍵
	 * @return 匿名路の局所的な情報を保持するインスタンス
	 */
	static LocalRouteInfo generateInfoAsRelay(ID id, SecretKey skey){
		LocalRouteInfo lrci = new LocalRouteInfo(id, skey, 0);
		return lrci;
	}
	
	/**
	 * 匿名路を構成するバックアップノードの情報を保持するインスタンスを生成する
	 * @param id このノード（バックアップノード）のID
	 * @param skey このノード（バックアップノード）と共有する共通鍵
	 * @return 匿名路の局所的な情報を保持するインスタンス
	 */
	static LocalRouteInfo generateInfoAsBackup(ID id, SecretKey skey){
		LocalRouteInfo lrci = new LocalRouteInfo(id, skey ,1);
		return lrci;
	}
	
	/**
	 * 得迷路を構成する受信ノードの情報を保持するインスタンスを生成する
	 * @param id このノード（受信ノード）のID
	 * @param skey このノード（受信ノード）と共有する共通鍵
	 * @return 匿名路の局所的な情報を保持するインスタンス
	 */
	static LocalRouteInfo generateInfoAsReceiver(ID id, SecretKey skey){
		LocalRouteInfo lrci = new LocalRouteInfo(id, skey ,2);
		return lrci;
	}
	
	/**
	 * このクラスが保持する情報が中継ノード（非バックアップノード）であるかどうか判定
	 * @return 中継ノード（非バックアップノード）ならtrue。それ以外ならfalse
	 */
	public boolean isRelay(){
		if(this.nodeType == 0)
			return true;
		return false;
	}
	
	/**
	 * このクラスが保持する情報がバックアップノードであるかどうか判定
	 * @return バックアップノードならtrue。それ以外ならfalse
	 */
	public boolean isBackup(){
		if(this.nodeType == 1)
			return true;
		return false;
	}
	
	/**
	 * このクラスが保持する情報が受信ノードであるかどうか判定
	 * @return 受信ノードならtrue。それ以外ならfalse
	 */
	public boolean isReceiver(){
		if(this.nodeType == 2)
			return true;
		return false;
	}
	
	/**
	 * このクラスが保持するノードのタイプを返却
	 * @return
	 * 	0:中継ノード（非バックアップノード）
	 * 	1:バックアップノード
	 *	2:受信ノード
	 */
	public int getNodeType(){
		return this.nodeType;
	}
	
	/**
	 * このクラスが保持するノードIDを返却
	 * @return ノードのID
	 */
	public ID getNodeID(){
		return this.nodeID;
	}
	
	/**
	 * このクラスが保持する共有鍵を返却
	 * @return このノードとの共有鍵
	 */
	public SecretKey getSharedKey(){
		return this.sharedKey;
	}
	
	/**
	 * このクラスの情報を表現する文字列を返す
	 * 色々改良の余地あり
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
	 * このクラスがもつIDを表す文字列を返す
	 * @return
	 */
	public String toStringID(){
		return this.nodeID.toString();
	}
}
