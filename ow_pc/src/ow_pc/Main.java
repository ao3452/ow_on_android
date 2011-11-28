package ow_pc;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Set;

import ow.MessageObject;
import ow.dht.DHT;
import ow.dht.DHTConfiguration;
import ow.dht.DHTFactory;
import ow.dht.ValueInfo;
import ow.id.ID;
import ow.messaging.util.MessagingUtility;
import ow.routing.RoutingException;

public final class Main {

	private final static short APPLICATION_ID = 0x01;
	private final static short APPLICATION_VERSION = 2;

	private final static int OW_PORT = DHTConfiguration.DEFAULT_CONTACT_PORT;
	private final static String JoinHOST = //"10.192.41.113";
	                                            //"133.68.187.100";//CSE
			                                      "133.68.15.197";
enum Mesage{
	put,
	get,
	init,
}

	private static DHT<MessageObject> dht = null;
	private static DHTConfiguration dhtConfig = null;

	public static void main(String[] args) throws Exception{

		// 繧｢繝峨Ξ繧ｹ縺ｮ蜿門ｾ�
		InetAddress ipAddr = getLocalAddress();
		// 繧ｳ繝ｳ繝輔ぅ繧ｰ險ｭ螳壹�螟峨∴繧句�蜷医�縲∬�蛻�〒繝悶�繝医せ繝医Λ繝��繧堤畑諢上＠縺ｦ縺上□縺輔＞縲�
		dhtConfig = DHTFactory.getDefaultConfiguration();
		dhtConfig.setImplementationName("ChurnTolerantDHT");
		dhtConfig.setMessagingTransport("TCP");
		dhtConfig.setContactPort(OW_PORT);
		dhtConfig.setSelfPortRange(4);
		dhtConfig.setDoUPnPNATTraversal(false);
		dhtConfig.setRoutingAlgorithm("Chord");
		dhtConfig.setRoutingStyle("Iterative");
		dhtConfig.setDirectoryType("VolatileMap");
		dhtConfig.setWorkingDirectory("./hash");
		//logView.append("2 \n");
		MessagingUtility.HostAndPort hostAndPort =
			MessagingUtility.parseHostnameAndPort(ipAddr.getHostAddress(), OW_PORT);
		dhtConfig.setSelfAddress(hostAndPort.getHostName());
		dhtConfig.setSelfPort(hostAndPort.getPort());

			dht = DHTFactory.getDHT(APPLICATION_ID, APPLICATION_VERSION,dhtConfig,null);
			//dht = DHTFactory.getDHT(APPLICATION_ID, APPLICATION_VERSION, dhtConfig, null);

		InetAddress bsIP = getBootstrapServer(ipAddr);
		if(bsIP == null) {
			System.out.print("Only mode... \n"+ ipAddr + "\n" + ipAddr.getHostAddress() + " " +ipAddr.getHostName()+ "\n");
		} else {
			System.out.print(bsIP + "\n" + bsIP.getHostAddress() + " " +bsIP.getHostName()+ "\n");
			dht.joinOverlay(bsIP.getHostAddress(), OW_PORT);

		}
		for(;;){
			BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
			String tmp=null;

				System.out.println("1.2.3");
				tmp = reader.readLine();

			int a=0;
			if (tmp.equals("put"))
				a=1;
			else if(tmp.equals("get"))
				a=2;
			else if(tmp.equals("init"))
				a=3;
			else if(tmp.equals("exit"))
				break;

			switch(a){
			case 1:

					System.out.println("1");
					put();

				break;
			case 2:

					System.out.println("2");
					get();

				break;
			case 3:
				System.out.println("3");
				init();
				break;
			default:
				break;
			}
		}
	}
	private static void put() throws Exception{
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		String key =reader.readLine();
		String value =reader.readLine();
		dht.put(ID.getSHA1BasedID(key.getBytes()), new MessageObject(value));
		System.out.print("PUT command : Key=" + key + "  Value=" + value + "\n");
	}
	private static void get() throws IOException{
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		String key =reader.readLine();
		Set<ValueInfo<MessageObject>> values = null;
		values = dht.get(ID.getSHA1BasedID(key.getBytes()));
		for(ValueInfo<MessageObject> mo : values) {
			System.out.print("GET command : Key=" + key + "  Value=" + mo.getValue().getFirst() + "\n");
		}
	}
	private static void init(){

	}
	 private static InetAddress getBootstrapServer(InetAddress hostIP) {

	    	//InetSocketAddress iSock = new InetSocketAddress(JoinHOST, OW_PORT);
			Socket socket = null;
			InputStream in = null;
			OutputStream out = null;
			try {
				//socket = new Socket();
				socket = new Socket(JoinHOST,OW_PORT);
				//socket.connect(iSock, 3000);
				out = socket.getOutputStream();
				ObjectOutputStream oout = new ObjectOutputStream(out);
				oout.writeObject(hostIP.getHostName());
				//oout.writeObject(hostIP.getHostAddress());
				//oout.writeObject("hakkoudasan.matlab.nitech.ac.jp");

				in = socket.getInputStream();
			    ObjectInputStream oin = new ObjectInputStream(in);
			    String bsIP = (String)oin.readObject();
			    if(bsIP.equals(hostIP.getHostName())) {
//			    if(bsIP.equals("10.192.41.113")) {
			    //if(bsIP.equals("hakkoudasan.matlab.nitech.ac.jp")) {
			    	return null;
			    }
			    InetAddress iNetAddr = InetAddress.getByName(bsIP);
			    return iNetAddr;

			} catch (Exception e) {
			} finally {
				try {
					if (socket != null) {
						socket.close();
						socket = null;
					}
				} catch (IOException e) {
					/* ignore */
				}
			}
			return null;

	    }

	    private static InetAddress getLocalAddress() {
			Enumeration enuIfs = null;
			try {
				enuIfs = NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			InetAddress ipAddr = null;
			InetAddress tmpIpAddr = null;
			if(null != enuIfs) {
				//check:
			    while(enuIfs.hasMoreElements()) {
			    	//Log.d("DHT", "INTERFECE FOUND");
			        NetworkInterface nic = (NetworkInterface)enuIfs.nextElement();
			       // Log.d("DHT", "getDisplayName:\t" + nic.getDisplayName());
			       // Log.d("DHT", "getName:\t" + nic.getName());
			        Enumeration enuAddrs = nic.getInetAddresses();
			        while (enuAddrs.hasMoreElements()) {
			            ipAddr = (InetAddress)enuAddrs.nextElement();
			       //     Log.d("DHT", "getHostAddress:\t" + ipAddr.getHostAddress());
			            if(!ipAddr.getHostAddress().equals("127.0.0.1")&&!ipAddr.getHostAddress().equals("0.0.0.0")) {
			            	tmpIpAddr=ipAddr;
			            	break;
			            }
			        }
			    }
		        ipAddr=tmpIpAddr;
			}
			return ipAddr;
		}
}