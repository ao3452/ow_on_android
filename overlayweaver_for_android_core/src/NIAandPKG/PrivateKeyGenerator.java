package NIAandPKG;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mypackage.C;
import mypackage.MyUtility;

import ow.id.ID;
import sun.misc.HexDumpEncoder;

import edu.cityu.ibe.PublicKeyGenerator;
import edu.cityu.util.Point;

public class PrivateKeyGenerator extends Thread
{
	PublicKeyGenerator kg;
	Map<ID, InetAddress> assignedID;
	
	public PrivateKeyGenerator()
	{
		kg = new PublicKeyGenerator();
		assignedID = Collections.synchronizedMap(new HashMap<ID, InetAddress>());
	}
	
	public void run()
	{
		Socket sock = null;
		int port = 4101;
		ServerSocket server = null;
		GenerationUnit gu;
		//CommunicationUnitForNIA CUfNIA = new CommunicationUnitForNIA();
		//CUfNIA.start();
		
		try
		{
			server = new ServerSocket(port);
		}
		catch (IOException e)
		{
			System.err.println(e);
		}
		
	//	System.out.println("Waiting...");
		
		while(true)
		{
			try
			{
				//waiting for connection
				sock = server.accept(); //
		//		System.out.println("Accept");
				
				//create thread
				gu = new GenerationUnit(sock);
				gu.start();
			}
			catch (IOException e)
			{
				System.err.println(e);
			}
		}
	}
	
	private Point generatePrivateKey(String ID)
	{
		Point privateKey = kg.getPrivateKey(ID);
		return(privateKey);
	}
	
	private CommonParameter  retCommonParam()
	{
		CommonParameter cp = new CommonParameter();
		kg.getMPK();
		
		return(cp);
	}
	
	private class GenerationUnit extends Thread
	{
		private Socket sock;
		private ObjectInputStream ois;
		private ObjectOutputStream oos;
		
		public GenerationUnit(Socket sock)
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
			//	System.out.println("generator thread is running");
				//read participating node's request
				Serializable[] recv = (Serializable[])ois.readObject();
				int msg_type = (Integer)recv[0];
				
				String id;
				Point privateKey;
				Point MPK;
				Serializable ret[];
				
				switch(msg_type)
				{
					case C.GET_NODE_PRIVATE_KEY:
						id = (String)recv[1];
					//	System.out.println("Generate private key : " + id);
						//TODO verify certification of NIA (not implemented)
						
						//generation private key
						privateKey = generatePrivateKey(id);
						MPK = kg.getMPK();
						
						//HexDumpEncoder hexdumpencoder = new HexDumpEncoder();
						//System.out.println("# Private Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(privateKey)));
						//System.out.println("# Master Public Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(MPK)));
						
						ret = new Serializable[2];
						ret[0] = MyUtility.object2Bytes(privateKey);
						ret[1] = MyUtility.object2Bytes(MPK);
						//return assigned ID
						oos.writeObject(ret);
						oos.flush();
					
						oos.close();
						ois.close();
						sock.close();
						break;
						
					case C.GET_SERVICE_PRIVATE_KEY:
						id = (String)recv[1];
					//	System.out.println("Generate private key : " + id);
						//TODO verify certification of NIA (not implemented)
						
						//generation private key
						privateKey = generatePrivateKey(id);
						MPK = kg.getMPK();
						
						//HexDumpEncoder hexdumpencoder = new HexDumpEncoder();
						//System.out.println("# Private Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(privateKey)));
						//System.out.println("# Master Public Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(MPK)));
						
						ret = new Serializable[2];
						ret[0] = MyUtility.object2Bytes(privateKey);
						ret[1] = MyUtility.object2Bytes(MPK);
						//return assigned ID
						oos.writeObject(ret);
						oos.flush();
					
						oos.close();
						ois.close();
						sock.close();
						break;
					default:
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
	}

	/*
	private class CommunicationUnitForNIA extends Thread
	{
		public CommunicationUnitForNIA()
		{
			
		}
		
		public void run()
		{
			Socket sock = null;
			int port = 4102;
			ServerSocket server = null;
			GenerationUnit gu;
			CommunicationUnitForNIA CUfNIA = new CommunicationUnitForNIA();
			
			CUfNIA.start();
			
			try
			{
				server = new ServerSocket(port);
			}
			catch (IOException e)
			{
				System.err.println(e);
			}
			
			System.out.println("Waiting...");
			
			while(true)
			{
				try
				{
					//waiting for connection
					sock = server.accept(); //
					System.out.println("Accept");
					
					//create thread
					gu = new GenerationUnit(sock);
					gu.start();
				}
				catch (IOException e)
				{
					System.err.println(e);
				}
			}
		}
	}
	*/
}

