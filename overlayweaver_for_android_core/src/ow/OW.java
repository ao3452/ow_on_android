package ow;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ow.dht.DHT;
import ow.dht.DHTConfiguration;
import ow.dht.DHTFactory;
import ow.messaging.util.MessagingUtility;

public class OW {

	private final static short APPLICATION_ID = 0x01;
	private final static short APPLICATION_VERSION = 2;
	
	private DHT<MessageObject> dht = null;
	private final static int OW_PORT = DHTConfiguration.DEFAULT_CONTACT_PORT;
	
	public void start(String[] args) {
		
		if(args.length != 1) {
			//System.out.println("Usege: [host]");
			return;
		}
		
		try {
			String host = args[0];
			
			DHTConfiguration dhtConfig = DHTFactory.getDefaultConfiguration();
			dhtConfig.setImplementationName("ChurnTolerantDHT");
			dhtConfig.setMessagingTransport("TCP");
			dhtConfig.setSelfPortRange(300);
			dhtConfig.setRoutingAlgorithm("Chord");
			dhtConfig.setRoutingStyle("Iterative");
			dhtConfig.setDirectoryType("VolatileMap");
			dhtConfig.setWorkingDirectory("./hash");
			dhtConfig.setDoUPnPNATTraversal(false);
			dhtConfig.setContactPort(OW_PORT);
			MessagingUtility.HostAndPort hostAndPort =
				MessagingUtility.parseHostnameAndPort(host, OW_PORT);
			dhtConfig.setSelfAddress(hostAndPort.getHostName());
			dhtConfig.setSelfPort(hostAndPort.getPort());
			
			dht = DHTFactory.getDHT(APPLICATION_ID, APPLICATION_VERSION, dhtConfig, null);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		loopExec();
	}
	
	private List<Runnable> execList = Collections.synchronizedList(new LinkedList<Runnable>());
	
	public void addExec(Runnable runb) {
		execList.add(runb);
	}
	
	private void loopExec() {
		while(true) {
			
			try {
				Thread.sleep(30000);
				
				synchronized (execList) {
					for(Runnable run: execList) {
						new Thread(run).start();
					}
					execList.clear();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
}
