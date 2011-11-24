package mypackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.crypto.BadPaddingException;

import ow.SessionID.SessionID;

public class MyUtility
{
	/**
	 * バイト列に変換する。
	 * @param o 変換したいもの
	 * @return バイト列に変換されたもの
	 * @throws IOException
	 */
	public static byte[] object2Bytes(Object o) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
			oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		baos.close();
		return baos.toByteArray();
	}
	
	/**
	 * バイト列からオブジェクトに変換する。
	 * @param raw
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object bytes2Object(byte raw[]) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object o = ois.readObject();
		ois.close();
		bais.close();
		return o;
	}
	
	public static byte[] readData(String filename, String directory)
	{
		try{
			//System.out.println("read file : " + directory + filename);
			// ファイルの中身を一気に読み込む
			File file = new File(directory + filename);
			byte[] buf = new byte[(int) file.length()];
			FileInputStream fi =  new FileInputStream(file);
			
			//read
			fi.read(buf);
			fi.close();
			
			//System.out.println("read complete");
			return(buf);
			
		} catch(FileNotFoundException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch(IOException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
		return(null);
	}
	
	public static byte[] readData(String filename)
	{
		try{
			//System.out.println("read file : " + filename);
			// ファイルの中身を一気に読み込む
			File file = new File(filename);
			byte[] buf = new byte[(int) file.length()];
			FileInputStream fi =  new FileInputStream(file);
			
			//read
			fi.read(buf);
			fi.close();
			
			//System.out.println("read complete");
			return(buf);
			
		} catch(FileNotFoundException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch(IOException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
		return(null);
	}
	
	
	public static byte[] PKCS5Padding(byte[] data)
	{
		final int block_size = 16;
		int pad_len = block_size - (data.length % block_size);
		ByteBuffer byteBuf = ByteBuffer.allocate(data.length + pad_len);
		
		//if(pad_len > block_size)
		//	System.out.println("invalid padding length @ Padding");
		
		//下位8ビット切り出し
		byte pad = (byte)((pad_len >>> 0 ) & 0xFF);
		
		//System.out.println("padding length : " + pad);
		
		byteBuf.put(data);
		for(int i = 0; i < pad_len; i++)
		{
			byteBuf.put(pad);
		}
		
		return(byteBuf.array());
	}
	
	public static byte[] PKCS5UnPadding(byte[] data) throws BadPaddingException
	{
		final int block_size = 16;
		int pad_len = data[data.length - 1] & 0xFF;
		
		if(pad_len > block_size)
		{
			//System.out.println("invalid padding length @ UnPadding");
			throw new BadPaddingException();
		}
		
		for(int i = 0; i < pad_len; i++)
		{
			if(data[data.length - 1 - i] != (data[data.length - 1] & 0xFF))
			{
				//System.out.println("invalid padding @ UnPadding @ " + System.currentTimeMillis());
				throw new BadPaddingException();
			}
		}
		
		int unpad_len = data.length - pad_len;
		
		return(Arrays.copyOf(data, unpad_len));
	}
	
	public static BigInteger SHA1Hash(byte[] data)
	{
		SessionID hash= new SessionID();
		hash.initSessionID(data, "SHA-1");
		return(hash.getSessionID_BI());
	}
}
