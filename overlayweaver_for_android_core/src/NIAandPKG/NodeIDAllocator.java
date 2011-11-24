package NIAandPKG;


import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mypackage.C;
import mypackage.MyUtility;

import ow.id.ID;
import ow.id.IDAddressPair;
import sun.misc.HexDumpEncoder;
import sun.nio.cs.ext.Big5;

public class NodeIDAllocator extends Thread
{
	private int nowJoinGroup = -1;
	private Map<ID, String> assignedIDs;	//save ID and IP address pair
	private Map<ID, String> lostIDs;
	private BigInteger two = new BigInteger("2");
	private List<ID> preparedIDs;
	private int numOfAssignedID = 0;
	
	/**
	 * 
	 */
	public NodeIDAllocator()
	{
		nowJoinGroup = -1;
		assignedIDs = Collections.synchronizedMap(new HashMap<ID, String>());
		lostIDs = Collections.synchronizedMap(new HashMap<ID, String>());
		preparedIDs = new ArrayList<ID>();
		numOfAssignedID = 0;
	}
	
	public void run()
	{
		Socket sock = null;
		int port = 4100;
		ServerSocket server = null;
		AllocationUnit au;
		
		try
		{
			server = new ServerSocket(port);
		}
		catch (IOException e)
		{
			System.err.println(e);
		}
		
		//System.out.println("Waiting...");
		
		while(true)
		{
			try
			{
				//waiting for connection
				sock = server.accept(); //
			//	System.out.println("Accept");
				
				//create thread
				au = new AllocationUnit(sock);
				au.start();
			}
			catch (IOException e)
			{
				System.err.println(e);
			}
		}
	}
	

	
	private class AllocationUnit extends Thread
	{
		private Socket sock;
		private ObjectInputStream ois;
		private ObjectOutputStream oos;
		
		public AllocationUnit(Socket sock)
		{
			this.sock = sock;
			try
			{
				ois = new ObjectInputStream(sock.getInputStream());
				oos = new ObjectOutputStream(sock.getOutputStream());
			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void run()
		{
			try
			{
			//	System.out.println("allocator thread is running");
				//read participating node's request
				Serializable[] recv = (Serializable[])ois.readObject();
				int msg_type = (Integer)recv[0];
				
				switch(msg_type)
				{
					case C.JOIN_NIA:
						String id = null;
						String s = (String)recv[1];
			//			System.out.println(s);
						
						if(s.equals("I want to join") == false)
							id = s;
						//TODO add verifying process
						
						//allocate ID
						ID assigningID = allocateID(sock.getInetAddress(), id);
						
						//HexDumpEncoder hexdumpencoder = new HexDumpEncoder();
						//System.out.println("Private Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(cp.getItsPriKey())));
						//System.out.println("Master Public Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(cp.getMasterPubKey())));
						
						//return assigned ID
			//			System.out.println("assign : " + assigningID);
						oos.writeObject(assigningID);
						oos.flush();
					
						oos.close();
						ois.close();
						sock.close();
						
						break;
						
					case C.LEAVE_REPORT_NIA:
						IDAddressPair left_node = (IDAddressPair)recv[1];
						lostIDs.put(left_node.getID(), left_node.getAddress().getHostAddress());
						
						//TODO implement renew and cover method
						
						break;

					default:
			//			System.out.println("Unknown message type : " + msg_type);
						break;
				}
				

			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public synchronized ID allocateID(InetAddress inetAddress, String id)
		{
		//	System.out.println("# now JoinGroup : " + nowJoinGroup);
		//	System.out.println("the number of assigned IDs : " + numOfAssignedID);
			
			ID ret = null;
		
			//first ID preparing
			if(nowJoinGroup < 0)
			{
				prepareIDs();
			}
			//If all IDs in a group are assigned, NIA prepares IDs in a new group.
			else if(numOfAssignedID >= (Math.pow(2, nowJoinGroup)))
			{
		//		System.out.println("new JoinGroup is allocated : " + nowJoinGroup);
				prepareIDs();
			}
			
			if(id != null)
			{
				ret = ID.getID(id, 20);
				
				if(assignedIDs.containsKey(ret) == false)
				{
					if(preparedIDs.contains(ret) == true)
					{
						preparedIDs.remove(ret);
						numOfAssignedID++;
					}
					assignedIDs.put(ret, inetAddress.toString());
					
					return(ret);
				}
			}
			
			if(preparedIDs.isEmpty() != true)
			{
				ret = preparedIDs.remove(0);
				assignedIDs.put(ret, inetAddress.toString());
				numOfAssignedID++;
			}
			//else
			//	System.out.println("preparedIDs are null");
		
			return(ret);
		}
		
		private int prepareIDs()
		{
			//do not consider that too many nodes are join.
			
			nowJoinGroup++;
			int groupSize = 1;
			
			preparedIDs = new ArrayList();
			
		//	System.out.println("now JoinGroup : " + nowJoinGroup);
			
			if(nowJoinGroup < 0)
			{
			//	System.out.println("invalid JoinGroup");
				return(-1);
			}
			else if(nowJoinGroup == 0)
			{
				BigInteger tmp = BigInteger.ZERO;
				ID id = ID.getID(tmp, 20);
				
				preparedIDs.add(id);
				return(nowJoinGroup);
			}
			else if(nowJoinGroup >= 1)
				groupSize = (int)Math.pow(2, nowJoinGroup - 1);//2 << (nowJoinGroup - 1);
			
			for(int i = groupSize; i < groupSize * 2; i++)
			{
				int reverse_i = reverseBitOrder(i);
				
				//System.out.println("i : " + i);
				
				byte[] bits = new byte[20];
				bits[0] = (byte)((reverse_i >>> 24 ) & 0xFF);
				bits[1] = (byte)((reverse_i >>> 16 ) & 0xFF);
				bits[2] = (byte)((reverse_i >>>  8 ) & 0xFF);
				bits[3] = (byte)((reverse_i >>>  0 ) & 0xFF);
				
				BigInteger tmp = new BigInteger(bits);
				ID id = ID.getID(tmp, 20);
				
				//System.out.println("ID : " + id.toString() + " is generated");
				
				if(assignedIDs.containsKey(id) == false)
					preparedIDs.add(id);
				else
				{
		//			System.out.println("already assigned ID");
					numOfAssignedID++;
				}
			}
			
			//randomize
			long seed = System.currentTimeMillis();
			Random rnd = new Random(seed);
			Collections.shuffle(preparedIDs, rnd);
			
			/*
			BigInteger groupSize = new BigInteger("4");
			if(nowGroupNum > 1)
			{
				groupSize = two.pow(nowGroupNum);
			}
			
			BigInteger lower = groupSize;
			BigInteger upper = groupSize.multiply(two);
			//ID[] preparedIDs = new ID[groupSize];
			List<ID> preparedIDs= new ArrayList();
			
			BigInteger tmp = lower;
			while(tmp.compareTo(upper) < 0)
			{
				byte[] id = new byte[20];
					tmp.toByteArray();
				BigInteger id = new BigInteger(i, 16);
				
				preparedIDs.add(arg0);
				
				tmp.add(BigInteger.ONE);
			}
			*/
			
			//for(ID t : preparedIDs)
			//{
			//	System.out.println(" # " + t.toString());
			//}
			
			return(nowJoinGroup);
		}
		

		
		public int reverseBitOrder(int bits)
		{
			bits = ((bits & 0x55555555) << 1) | ((bits & 0xaaaaaaaa) >> 1);
			bits = ((bits & 0x33333333) << 2) | ((bits & 0xcccccccc) >> 2);
			bits = ((bits & 0x0f0f0f0f) << 4) | ((bits & 0xf0f0f0f0) >> 4);
			bits = ((bits & 0x00ff00ff) << 8) | ((bits & 0xff00ff00) >> 8);
			bits = ((bits & 0x0000ffff) << 16) | ((bits & 0xffff0000) >> 16);
			
			bits ^= 0;
			return(bits);
		}
	}
}


