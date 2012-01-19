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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Set;

import ow.MessageObject;
import ow.dht.DHT;
import ow.dht.DHTConfiguration;
import ow.dht.DHTConfiguration.commFlag;
import ow.dht.DHTFactory;
import ow.dht.ValueInfo;
import ow.id.ID;
import ow.id.IDAddressPair;
import ow.messaging.util.MessagingUtility;
import ow.routing.RoutingException;
import ow.routing.RoutingHop;
import ow.routing.RoutingResult;

public final class Main {

	private final static short APPLICATION_ID = 0x01;
	private final static short APPLICATION_VERSION = 2;

	private final static int OW_PORT = DHTConfiguration.DEFAULT_CONTACT_PORT;
	private final static String JoinHOST = 
			//"10.192.41.113";
			//"133.68.187.100";//CSE
			//"133.68.186.70";//cs-d60
			"133.68.15.197";

	private enum Message{
		PUT("1"),
		GET("2"),
		STATUS("3"),
		CONSTRUCT("4"),
		COMMUNICATE("5"),
		RELAY_CHANGE("6"),
		GET_LOG("7"),
		NOT_NODE("");
		
		private final String name;
		
		private Message(String name){
			this.name = name;
		}
		
		@Override
		public String toString(){
			return name;
		}
		
		public static Message toMessage(String name){
			Message result = null;
			
			for(Message message : values()){
				if(message.toString().equals(name)){
					result = message;
					break;
				}
			}
			return result != null ? result : NOT_NODE;
		}
	}

	private enum NodeID{
		ONE("out-road0x01.ssn.nitech.ac.jp"),
		TWO("out-road0x02.ssn.nitech.ac.jp"),
		THREE("out-road0x03.ssn.nitech.ac.jp"),
		FOUR("mat-asus.matlab.nitech.ac.jp"),
		FIVE("syourin.matlab.nitech.ac.jp"),
		SIX("mat-desire.matlab.nitech.ac.jp"),
//		SEVEN("cs-d01"),
//		EIGHT("cs-d02"),
//		NINE("cs-d03"),
//		TEN("cs-d04"),
//		ELEVEN("cs-d05"),
//		TWELVE("cs-d06"),
//		THIRTEEN("cs-d07"),
//		FOURTEEN("cs-d08"),
//		FIFTEEN("cs-d09"),
//		SIXTEEN("cs-d10"),
//		SEVENTEEN("cs-d11"),
//		EIGHTEEN("cs-d12"),
//		NINETEEN("cs-d13"),
//		TWENTY("cs-d14"),
//		TWENTY_ONE("cs-d15"),
//		TWENTY_TWO("cs-d16"),
//		TWENTY_THREE("cs-d17"),
//		TWENTY_FOUR("cs-d18"),
//		TWENTY_FIVE("cs-d19"),
//		TWENTY_SIX("cs-d20"),
//		TWENTY_SEVEN("cs-d21"),
//		TWENTY_EIGHT("cs-d22"),
//		TWENTY_NINE("cs-d23"),
//		THIRTY("cs-d24"),
//		THIRTY_ONE("cs-d25"),
//		THIRTY_TWO("cs-d26"),
////		THIRTY_THREE("cs-d27"),
////		THIRTY_FOUR("cs-d28"),
////		THIRTY_FIVE("cs-d29"),
////		THIRTY_SIX("cs-d30"),
		SEVEN("cs-d01"),
		EIGHT("133.68.186.12"),
		NINE("133.68.186.13"),
		TEN("133.68.186.14"),
		ELEVEN("133.68.186.15"),
		TWELVE("133.68.186.16"),
		THIRTEEN("133.68.186.17"),
		FOURTEEN("133.68.186.18"),
		FIFTEEN("133.68.186.19"),
		SIXTEEN("133.68.186.20"),
		SEVENTEEN("133.68.186.21"),
		EIGHTEEN("133.68.186.22"),
		NINETEEN("133.68.186.23"),
		TWENTY("133.68.186.24"),
		TWENTY_ONE("133.68.186.25"),
		TWENTY_TWO("133.68.186.26"),
		TWENTY_THREE("133.68.186.27"),
		TWENTY_FOUR("133.68.186.28"),
		TWENTY_FIVE("133.68.186.29"),
		TWENTY_SIX("133.68.186.30"),
		TWENTY_SEVEN("133.68.186.31"),
		TWENTY_EIGHT("133.68.186.32"),
		TWENTY_NINE("133.68.186.33"),
		THIRTY("133.68.186.34"),
		THIRTY_ONE("133.68.186.35"),
		THIRTY_TWO("133.68.186.36"),
//		THIRTY_THREE(""),
//		THIRTY_FOUR(""),
//		THIRTY_FIVE(""),
//		THIRTY_SIX(""),
		HAKKOUDASAN("hakkoudasan.matlab.nitech.ac.jp"),
		CSE("cse.cs.nitech.ac.jp"),
		NOT_NODE("");
		
		private final String name;
		
		private NodeID(String name){
			this.name = name;
		}
		
		@Override
		public String toString(){
			return name;
		}
		
		public static NodeID toID(String name){
			NodeID result = null;
			
			for(NodeID nodeID : values()){
				if(nodeID.toString().equals(name)){
					result = nodeID;
					break;
				}
			}
			return result != null ? result : NOT_NODE;
		}
	}
	private static DHT<MessageObject> dht = null;
	private static DHTConfiguration dhtConfig = null;
	
	public static void main(String[] args) throws Exception{

		// アドレスの取得
		InetAddress ipAddr = getLocalAddress();
		// コンフィグ設定。変える場合は、自分でブートストラップを用意してください。
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
		//System.out.print("2 \n");
		MessagingUtility.HostAndPort hostAndPort =
			MessagingUtility.parseHostnameAndPort(ipAddr.getHostAddress(), OW_PORT);
		dhtConfig.setSelfAddress(hostAndPort.getHostName());
		dhtConfig.setSelfPort(hostAndPort.getPort());

		ID selfID = getSelfID(ipAddr);
		System.out.println("selfID : "+selfID);
		  
		dhtConfig.setMyID(selfID);
		dht = DHTFactory.getDHT(APPLICATION_ID, APPLICATION_VERSION,dhtConfig,selfID);
		//dht = DHTFactory.getDHT(APPLICATION_ID, APPLICATION_VERSION, dhtConfig, null);
		
		InetAddress bsIP = getBootstrapServer(ipAddr);
		if(bsIP == null) {
			System.out.print("Only mode... \n"+ ipAddr + "\n" + ipAddr.getHostAddress() + " " +ipAddr.getHostName()+ "\n");
		} else {
			System.out.print(bsIP + "\n" + bsIP.getHostAddress() + " " +bsIP.getHostName()+ "\n");
			dht.joinOverlay(bsIP.getHostAddress(), OW_PORT);
			
		}
		OUT:
			for(;;){
				BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
				String tmp=null;
				
				System.out.println("1:put 2:get 3:status 4:construct 5:communicate 6:relay 7:get_log");
				tmp = reader.readLine();
				
				switch(Message.toMessage(tmp)){
				case PUT:
					System.out.println("put");
					put();
					break;
				case GET:
					System.out.println("get");
					get();
					break;
				case STATUS:
					System.out.println("status");
					status();
					break;
				case CONSTRUCT:
					System.out.println("construct");
					construct();
					break;
				case COMMUNICATE:
					System.out.println("communicate");
					communicate();
					break;
				case RELAY_CHANGE:
					System.out.println("relay change");
					relayChange();
					break;
				case GET_LOG:
					System.out.println("get log");
					System.out.print(dhtConfig.globalSb.toString());
					break;
				default:
					break OUT;
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
	private static void status(){
		// show self address
		System.out.print("ID and address: " + dht.getSelfIDAddressPair() + "\n");
		
		// show routing table
		System.out.print("Routing table:" + "\n");
		String routingTableString = dht.getRoutingTableString().replaceAll("\n", "\n");
		System.out.print(routingTableString + "\n");
		
		// show last key and route
		System.out.print("Last key: ");
		System.out.print(dht.getLastKeyString() + "\n");
		
		RoutingResult routingRes = dht.getLastRoutingResult();
		if (routingRes != null) {
			long timeBase = -1L;
			
			System.out.print("Last route: ");
			System.out.print("[");
			for (RoutingHop hop: routingRes.getRoute()) {
				if (timeBase < 0L) {
					timeBase = hop.getTime();
				}
				
				System.out.print("\n" + " ");
				System.out.print(hop.getIDAddressPair() + " (");
				long time = hop.getTime() - timeBase;
				System.out.print(time + ")");
			}
			System.out.print("\n" + "]" + "\n");
			
			System.out.print("Last root candidates: ");
			System.out.print("[");
			for (IDAddressPair r: routingRes.getRootCandidates()) {
				System.out.print("\n");
				System.out.print(" " + r);
			}
			System.out.print("\n" + "]" + "\n");
		}
		
	}
	private static ArrayList<ID> constructNodeID = new ArrayList<ID>();
	private static void construct() throws IOException{
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		String targetID =reader.readLine();
		System.out.print("construct : "+targetID+"\n");
		ID constructTargetID=ID.getID(targetID, 20);
		if(dht.construct(constructTargetID, 8)){
			System.out.print("construct success");
			constructNodeID.add(constructTargetID);
			System.out.print(dhtConfig.globalSb.toString());
			//dhtConfig.globalSb.setLength(0);
		}
		else
			System.out.print("construct failed");
		//viewflipper.setDisplayedChild(0);
	}
	private static void communicate() throws IOException{
		System.out.println(constructNodeID);
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		System.out.println("ターゲットIDを選択してください．数字で0から"+constructNodeID.size());
		String targetNumber =reader.readLine();
		ID communicateTargetID = constructNodeID.get(Integer.getInteger(targetNumber));
		System.out.println("送信するメッセージを記入してください．");
		String communicateMessage =reader.readLine();
		try{
			dht.communicate(communicateTargetID, communicateMessage);
			System.out.print(dhtConfig.globalSb.toString());
			//viewflipper.setDisplayedChild(0);
		} catch(Exception e){
			System.out.print("error communicate\n");
			//viewflipper.setDisplayedChild(0);
		}
	}
	private static void relayChange() throws IOException{
		System.out.println(constructNodeID);
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		System.out.println("変更したい匿名路のIDを選択してください．数字で0から"+constructNodeID.size());
		String changeConstructNumberString =reader.readLine();
		int changeConstructNumber = Integer.parseInt(changeConstructNumberString);
		System.out.println("中継方法を選択してください．");
		System.out.println("1 : 中継拒否  2 : 中継時に暗号化・復号しない  3 : 通常の中継");
		String changeFlagNumber =reader.readLine();
		commFlag changeFlag= commFlag.Permit;
		switch(Integer.parseInt(changeFlagNumber)){
		case 1 :
			changeFlag = commFlag.Reject;
			break;
		case 2 :
			changeFlag = commFlag.Relay;
			break;
		case 3 :
			changeFlag = commFlag.Permit;
			break;
		default:
			System.out.println("もう一度選択し直してください");
			relayChange();
			return;
		}
		dhtConfig.setCommunicateMethodFlag(changeFlag, changeConstructNumber);
		System.out.println("中継方法を変更しました．"+changeConstructNumber+" "+changeFlag);
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
//				if(bsIP.equals("10.192.41.113")) {
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
		// ローカルホスト名とIPアドレスを取得
	    try {
	      InetAddress addr = InetAddress.getLocalHost();
	      if(addr.getHostAddress().equals("127.0.0.1")){
	    	  addr=InetAddress.getByName(getIPAddress(addr.getHostName()));
	      }
	      System.out.println("Local Host Name: " + addr.getHostName());
	      System.out.println("IP Address     : " + addr.getHostAddress());
	      return addr;
	    } catch (UnknownHostException e) {
	      e.printStackTrace();
	    }
//		Enumeration enuIfs = null;
//		try {
//			enuIfs = NetworkInterface.getNetworkInterfaces();
//		} catch (SocketException e) {
//			e.printStackTrace();
//		}
//		InetAddress ipAddr = null;
//		InetAddress tmpIpAddr = null;
//		if(null != enuIfs) {
//			check:
//			while(enuIfs.hasMoreElements()) {
//				//Log.d("DHT", "INTERFECE FOUND");
//				NetworkInterface nic = (NetworkInterface)enuIfs.nextElement();
//				// Log.d("DHT", "getDisplayName:\t" + nic.getDisplayName());
//				// Log.d("DHT", "getName:\t" + nic.getName());
//				Enumeration enuAddrs = nic.getInetAddresses();
//				while (enuAddrs.hasMoreElements()) {
//					ipAddr = (InetAddress)enuAddrs.nextElement();
//					//     Log.d("DHT", "getHostAddress:\t" + ipAddr.getHostAddress());
//					if(!ipAddr.getHostAddress().equals("127.0.0.1")&&!ipAddr.getHostAddress().equals("0.0.0.0")) {
//						tmpIpAddr=ipAddr;
//						break check;
//					}
//				}
//			}
//			ipAddr=tmpIpAddr;
//		}
//		return ipAddr;
		return null;
	}

	private static ID getSelfID(InetAddress ipAddr) {
		ID selfID;
		  switch(NodeID.toID(ipAddr.getHostName())){
		  case ONE :
			  selfID= ID.getID("0000000000000000000000000000000000000000", 20);
			  break;
		  case TWO :
			  selfID= ID.getID("8000000000000000000000000000000000000000", 20);
			  break;
		  case THREE :
			  selfID= ID.getID("4000000000000000000000000000000000000000", 20);
			  break;
		  case FOUR :
			  selfID= ID.getID("c000000000000000000000000000000000000000", 20);
			  break;
		  case FIVE :
			  selfID= ID.getID("2000000000000000000000000000000000000000", 20);
			  break;
		  case SIX :
			  selfID= ID.getID("a000000000000000000000000000000000000000", 20);
			  break;
		  case SEVEN :
			  selfID= ID.getID("6000000000000000000000000000000000000000", 20);
			  break;
		  case EIGHT :
			  selfID= ID.getID("e000000000000000000000000000000000000000", 20);
			  break;
		  case NINE :
			  selfID= ID.getID("1000000000000000000000000000000000000000", 20);
			  break;
		  case TEN :
			  selfID= ID.getID("9000000000000000000000000000000000000000", 20);
			  break;
		  case ELEVEN :
			  selfID= ID.getID("5000000000000000000000000000000000000000", 20);
			  break;
		  case TWELVE :
			  selfID= ID.getID("d000000000000000000000000000000000000000", 20);
			  break;
		  case THIRTEEN :
			  selfID= ID.getID("3000000000000000000000000000000000000000", 20);
			  break;
		  case FOURTEEN :
			  selfID= ID.getID("b000000000000000000000000000000000000000", 20);
			  break;
		  case FIFTEEN :
			  selfID= ID.getID("7000000000000000000000000000000000000000", 20);
			  break;
		  case SIXTEEN :
			  selfID= ID.getID("f000000000000000000000000000000000000000", 20);
			  break;
		  case SEVENTEEN :
			  selfID= ID.getID("0800000000000000000000000000000000000000", 20);
			  break;
		  case EIGHTEEN :
			  selfID= ID.getID("8800000000000000000000000000000000000000", 20);
			  break;
		  case NINETEEN :
			  selfID= ID.getID("4800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY :
			  selfID= ID.getID("c800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_ONE :
			  selfID= ID.getID("2800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_TWO :
			  selfID= ID.getID("a800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_THREE :
			  selfID= ID.getID("6800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_FOUR :
			  selfID= ID.getID("e800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_FIVE :
			  selfID= ID.getID("1800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_SIX :
			  selfID= ID.getID("9800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_SEVEN :
			  selfID= ID.getID("5800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_EIGHT :
			  selfID= ID.getID("d800000000000000000000000000000000000000", 20);
			  break;
		  case TWENTY_NINE :
			  selfID= ID.getID("3800000000000000000000000000000000000000", 20);
			  break;
		  case THIRTY :
			  selfID= ID.getID("b800000000000000000000000000000000000000", 20);
			  break;
		  case THIRTY_ONE :
			  selfID= ID.getID("7800000000000000000000000000000000000000", 20);
			  break;
		  case THIRTY_TWO :
			  selfID= ID.getID("f800000000000000000000000000000000000000", 20);
			  break;
//		  case THIRTY_THREE :
//			  selfID= ID.getID("f000000000000000000000000000000000000000", 20);
//			  break;
//		  case THIRTY_FOUR :
//			  selfID= ID.getID("0800000000000000000000000000000000000000", 20);
//			  break;
//		  case THIRTY_FIVE :
//			  selfID= ID.getID("f000000000000000000000000000000000000000", 20);
//			  break;
//		  case THIRTY_SIX :
//			  selfID= ID.getID("0800000000000000000000000000000000000000", 20);
//			  break;
		  case HAKKOUDASAN :
			  selfID= ID.getID("6000000000000000000000000000000000000000", 20);
			  break;
		  case CSE:
			  selfID= ID.getID("e000000000000000000000000000000000000000", 20);
			  break;
		  default :
			  selfID=null;
		  }
		return selfID;
	}
	private static String getIPAddress(String hostName) {
		// TODO 自動生成されたメソッド・スタブ
		String ipAddress;
		switch(NodeID.toID(hostName)){
		  case ONE :
			  ipAddress="133.68.42.171";

			  break;
		  case TWO :
			  ipAddress="133.68.42.172";
			  break;
		  case THREE :
			  ipAddress="133.68.42.173";
			  break;
		  case FOUR :
			  ipAddress="133.68.15.40";
			  break;
		  case FIVE :
			  ipAddress="133.68.15.179";
			  break;
		  case SIX :
			  ipAddress="133.68.15.28";
			  break;
		  case SEVEN :
			  ipAddress="133.68.186.11";
			  break;
		  case EIGHT :
			  ipAddress="133.68.186.12";
			  break;
		  case NINE :
			  ipAddress="133.68.186.13";
			  break;
		  case TEN :
			  ipAddress="133.68.186.14";
			  break;
		  case ELEVEN :
			  ipAddress="133.68.186.15";
			  break;
		  case TWELVE :
			  ipAddress="133.68.186.16";
			  break;
		  case THIRTEEN :
			  ipAddress="133.68.186.17";
			  break;
		  case FOURTEEN :
			  ipAddress="133.68.186.18";
			  break;
		  case FIFTEEN :
			  ipAddress="133.68.186.19";
			  break;
		  case SIXTEEN :
			  ipAddress="133.68.186.20";
			  break;
		  case SEVENTEEN :
			  ipAddress="133.68.186.21";
			  break;
		  case EIGHTEEN :
			  ipAddress="133.68.186.22";
			  break;
		  case NINETEEN :
			  ipAddress="133.68.186.23";
			  break;
		  case TWENTY :
			  ipAddress="133.68.186.24";
			  break;
		  case TWENTY_ONE :
			  ipAddress="133.68.186.25";
			  break;
		  case TWENTY_TWO :
			  ipAddress="133.68.186.26";
			  break;
		  case TWENTY_THREE :
			  ipAddress="133.68.186.27";
			  break;
		  case TWENTY_FOUR :
			  ipAddress="133.68.186.28";
			  break;
		  case TWENTY_FIVE :
			  ipAddress="133.68.186.29";
			  break;
		  case TWENTY_SIX :
			  ipAddress="133.68.186.30";
			  break;
		  case TWENTY_SEVEN :
			  ipAddress="133.68.186.31";
			  break;
		  case TWENTY_EIGHT :
			  ipAddress="133.68.186.32";
			  break;
		  case TWENTY_NINE :
			  ipAddress="133.68.186.33";
			  break;
		  case THIRTY :
			  ipAddress="133.68.186.34";
			  break;
		  case THIRTY_ONE :
			  ipAddress="133.68.186.35";
			  break;
		  case THIRTY_TWO :
			  ipAddress="133.68.186.36"; 
			  break;
//		  case THIRTY_THREE :
//			 ipAddress="133.68.186.37";
//			  break;
//		  case THIRTY_FOUR :
//			 ipAddress="133.68.186.38";
//			  break;
//		  case THIRTY_FIVE :
//			 ipAddress="133.68.186.39";
//			  break;
//		  case THIRTY_SIX :
//			 ipAddress="133.68.186.40";
//			  break;
		  case HAKKOUDASAN :
			  ipAddress="133.68.15.197";
			  break;
		  case CSE:
			  ipAddress="133.68.187.100";
			  break;
		  default :
			  ipAddress=null;
		  }
		return ipAddress;
	}

}