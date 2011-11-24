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
 * ƿ̾ϩ���ۤ����塢ľ�ܤ�������Ρ��ɤ�Υæ���Ƥʤ�����ǧ���뤿��Υ��饹 ľ�ܤ������������ݻ������ꥹ�Ȥ��������Ū�˳�ǧ����
 * ���ʤߤ�Surveilance��"�ƻ롢��ĥ��"�Ȥ�����̣�� �ޤ��ҤȤĤ������ˤʤä��͡�
 * 
 * @author nozomu
 * 
 */

public class NodeSurveilance {

	/*
	 * �Ρ��ɤν�ʣ��ȯ�����ʤ��褦��List�Ǥʤ�Set�ǥΡ��ɤ����
	 */
	private Set<IDAddressPair> nodeSet;
	
	/*
	 * Ping��Ȥ����ѿ��ʤʤ�ǥ��㥹�Ȥ��Ƥ��������������դ�ʬ����ʤ���
	 */
	private RoutingRuntime runtime;

	/*
	 * �Ρ��ɤ�Υæ���Τ����������Ԥ������롣 ���κ�dht�Υ᥽�åɤ����Ѥ���Τ��ݻ����Ƥ����ʤǤ��뤫�ɤ����԰¤Ǥ���
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
	 * ������ƿ̾ϩ����������ä��Ȥ��ˡ��ƻ��оݤȤ���������Ρ��ɤ��ɲä��뤿��δؿ� IDAddressPair�ʳ���Ȥ����⡣
	 * 
	 * @param dest
	 *            ����������Ρ��ɤξ���
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
				// ���ƥ졼�����Ǹ�ޤ��褿���ǽ�����
				if(!target.hasNext())
					target = nodeSet.iterator();
				else
					target.next();
				pair = (IDAddressPair)target;
				// ������node���Ф���Ping���ꤲ��
				try {
					// ȿ�����ʤ����DHT���Ф���Υæ�����Τ���
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
