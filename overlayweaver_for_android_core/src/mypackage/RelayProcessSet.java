package mypackage;

import javax.crypto.SecretKey;

import ow.messaging.MessagingAddress;

/**
 * 匿名通信で前ノードからメッセージを受け取った後、次のノードへ送るための処理や送信内容を蓄えるクラス
 * メッセージを受け取った後、各ノードはヘッダをキー、
 * ・次のノードの情報	: MessagingAddress dest
 * ・共通鍵				: SecretKey key
 * ・次ノードに送る情報	: byte[] sendHeader
 * ・暗号化か復号か		: int decOrEnc
 * ・構築中の匿名路番号 : int number  // 1/3 廣瀬が追加
 * 上記の4つの組み合わせをバリューとして保存する
 * このクラスではバリューとなるセットをセットとして保持する。
 * 
 * もっと適切なクラス名無いかな？
 * 
 * @author nozomu
 *
 */

public class RelayProcessSet {
	
	/*
	 * byte[] sendHeaderだがこれだけは悩んでいる。
	 * つぎのノードに対して、復号したヘッダを渡すか、hashCodeを実行して得たハッシュ値を渡すか。
	 * コストやらを考えるとハッシュ値を渡した方が良いような気がする。
	 * 
	 * 2011.10.03
	 * 実験して結果が良い方にしよう。
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
