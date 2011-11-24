package NIAandPKG;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.*;

import com.sun.org.apache.xpath.internal.axes.SelfIteratorNoPredicate;

import edu.cityu.util.Point;

import ow.id.ID;

import mypackage.C;
import mypackage.MyUtility;

import sun.misc.HexDumpEncoder;

public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//int test = 1;
		NodeIDAllocator NIA = new NodeIDAllocator();
		PrivateKeyGenerator PKG = new PrivateKeyGenerator();
		//test = NIA.reverseBitOrder(test);
		
		//System.out.println(test);
		//System.out.println(Integer.MIN_VALUE);
		
		NIA.start();
		PKG.start();
		//System.out.println("parent thread");
		
		//joinRequest();
	}
	
	public static void joinRequest()
	{
		/*
		try
		{
			   
			// TrustStore���ɤ߹���
			KeyStore trust_store = KeyStore.getInstance("JKS");

			// changit ��clientTrust���ä��Ȥ��Υѥ����
			char[] trust_pass = "changeit".toCharArray();
			trust_store.load(new FileInputStream("C:\\Temp\\clientTrust"), trust_pass);
			   
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(trust_store);
			   
			// �����åȤ���������
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), null);
			SSLSocketFactory sf = context.getSocketFactory();
			SSLSocket soc = (SSLSocket)sf.createSocket("192.168.1.100", 51004);
			soc.startHandshake();
			   
			   // �����ϥ��ȥ꡼����������
			ObjectOutputStream oos = new ObjectOutputStream(soc.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(soc.getInputStream());
			   
			// ���ϥ��ȥ꡼��˥��֥������Ȥ�񤭽Ф�
			oos.writeObject(args[0]);
			   
			// ���ϥ��ȥ꡼�फ�饪�֥������Ȥ��������
			String str = (String)ois.readObject();
			   
			// �����Фν�����̤�ɽ������
			System.out.println("string to uppercase : " + str);
			   
			// ���ȥ꡼�ࡦ�����åȤ��Ĥ���
			oos.close();
			ois.close();
			soc.close();
		}
		catch(KeyStoreException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/
		
		ObjectInputStream ois;
		ObjectOutputStream oos;
		
		Socket sock;
		try
		{
			for(int i = 0; i < 32; i++)
			{
				//System.out.println("connect to server");
				sock = new Socket(C.ADDR_NIA , C.PORT_NIA);
				
				oos = new ObjectOutputStream(sock.getOutputStream());
				ois = new ObjectInputStream(sock.getInputStream());
			
			//	System.out.println("request");
				//oos = new ObjectOutputStream(sock.getOutputStream());
				
				String s = "I want to join";
				oos.writeObject(s);
				oos.flush();
				
				//ois = new ObjectInputStream(sock.getInputStream());
				ID assignedID = (ID)ois.readObject();
			//	System.out.println(assignedID);
				
				
					
				//CommonParameter cp = (CommonParameter)ois.readObject();
				
				//HexDumpEncoder hexdumpencoder = new HexDumpEncoder();
				//System.out.println("Private Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(cp.getItsPriKey())));
				//System.out.println("Master Public Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(cp.getMasterPubKey())));
				
				oos.close();
				ois.close();
				sock.close();
				
				
				sock = new Socket(C.ADDR_PKG , C.PORT_PKG);
				
				oos = new ObjectOutputStream(sock.getOutputStream());
				ois = new ObjectInputStream(sock.getInputStream());
			
			//	System.out.println("request to PKG");
				//oos = new ObjectOutputStream(sock.getOutputStream());
				
				String id = assignedID.toString();
				oos.writeObject(id);
				oos.flush();
				
				Serializable ret[] = new Serializable[2];
				ret = (Serializable[]) ois.readObject();
				
				Point priKey = (Point)MyUtility.bytes2Object((byte[])ret[0]);
				Point MPK = (Point)MyUtility.bytes2Object((byte[])ret[1]);
				
				//HexDumpEncoder hexdumpencoder = new HexDumpEncoder();
				//System.out.println("Private Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(priKey)));
				//System.out.println("Master Public Key : " + hexdumpencoder.encodeBuffer(MyUtility.object2Bytes(MPK)));
				
				oos.close();
				ois.close();
				sock.close();
			}
		}
		catch(UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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
