package ow.anonymous;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ow.dht.DHT;
import ow.id.IDAddressPair;
import ow.messaging.MessageSender;
import ow.routing.RoutingRuntime;
import ow.routing.RoutingService;

/**
 * 匿名路を構築した後、直接の送信先ノードが離脱してないか確認するためのクラス 直接の送信先情報を保持したリストをもち、定期的に確認する
 * ちなみにSurveilanceは"監視、見張り"という意味。 またひとつお利口になったね。
 * 
 * @author nozomu
 * 
 */

public class NodeSurveilance {

	/*
	 * ノードの重複が発生しないようにListでなくSetでノードを管理
	 */
	private Set<IDAddressPair> nodeSet;
	
	/*
	 * Pingを使える変数（なんでキャストしてるんだろう？そこら辺が分かんない）
	 */
	private RoutingRuntime runtime;

	/*
	 * ノードの離脱を検知したら送信者に伝える。 その際dhtのメソッドを利用するので保持しておく（できるかどうか不安です）
	 */
	private DHT dht;
	
	private MessageSender sender;

	public NodeSurveilance(DHT dht, RoutingService routingSvc) {
		this.dht = dht;
		this.runtime = (RoutingRuntime) routingSvc;
		this.nodeSet = new HashSet<IDAddressPair>();
		this.sender = this.runtime.getMessageSender();
	};

	/**
	 * 新しい匿名路情報を受け取ったときに、監視対象として送信先ノードを追加するための関数 IDAddressPair以外を使うかも。
	 * 
	 * @param dest
	 *            次の送信先ノードの情報
	 */
	public synchronized void addDirectDestNode(IDAddressPair dest) {
		nodeSet.add(dest);
	}
	
	public synchronized void removeDirectDestNode(IDAddressPair dest){
		nodeSet.remove(dest);
	}

	private final class AnonymousRouteFixer implements Runnable{
		
		public void run() {
			Iterator<IDAddressPair> target = nodeSet.iterator();
			IDAddressPair pair;
			while(true){
				// イテレータが最後まで来た場合最初に戻る
				if(!target.hasNext())
					target = nodeSet.iterator();
				else
					target.next();
				pair = (IDAddressPair)target;
				// ここでnodeに対してPingを投げる
				try {
					// 反応がなければDHTに対して離脱を通知する
					if(!runtime.ping(sender, pair)){
						dht.notice(pair.getID());
						removeDirectDestNode(pair);
					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	

}
