package ow.SessionID;

import java.security.*;
import java.io.*;
import java.math.BigInteger;

public class SessionID implements Serializable
{
	private static final long serialVersionUID = -5502820400668813482L;
	//セッションID（バイト列）
	private byte[] SID;
	//セッションID（10進整数）
	private BigInteger SID_BI;

	public SessionID()
	{
		SID = null;
		SID_BI = null;
	}

	/**
	 * initialize (calculate) SessionID.
	 * 
	 * @param msg
	 *                       message
	 * @param alg
	 *                        algorithm of message digest
	 */
	public void initSessionID(byte[] msg, String alg)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance(alg);// initialize MessageDigest object
			md.update(msg);	// set data which source of digest
			SID = md.digest();	// calc digest
			
			// to hexadecimal
			StringBuffer dec = new StringBuffer("");
			for(int i = 0; i < SID.length; i++)
			{
				int val = SID[i] & 0xFF;
				if(val < 16)
				{
					dec.append("0");
				}
				dec.append(Integer.toString(val, 16));
			}
			
			SID_BI = new BigInteger(dec.toString(), 16);

			printDigest(SID);
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * セッションIDのバイト配列を得て文字列に変換して出力
	 * Get byte array of SessionID and convert String.
	 * Print digest.
	 * @param digest
	 *                        ダイジェスト
	 */
	private void printDigest(byte[] digest)
	{
		
		StringBuffer dec = new StringBuffer("");
		for(int i = 0; i < digest.length; i++)
		{
			int val = digest[i] & 0xFF;
			if(val < 16)
			{
				dec.append("0");
			}
			dec.append(Integer.toString(val, 16)).append(" ");
		}
		
		//System.out.println("SID hexd : " + dec.toString());
		
		
		//System.out.println(SID_BI);
	}
	
	/**
	 * verify digest
	 * @param alg
	 *                        message digest algorithm
	 * @param msg
	 *                        message
	 */
	public void validateDigest(byte[] msg, String alg)
	{
		try
		{
			//Initialize MessageDigest object
			MessageDigest md = MessageDigest.getInstance(alg);
			md.update(msg);
			byte[] calcDigest = md.digest();
			printDigest(calcDigest);
			// verify digest
			if(MessageDigest.isEqual(SID, calcDigest))
			{
			//	System.out.println("This message is valid.");
			}
			else
			{
				//System.out.println("This message is invalid.");
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public byte[] getSessionID()
	{
		return(SID);
	}
	
	public BigInteger getSessionID_BI()
	{
		return(SID_BI);
	}
	
	/**
	 * set SessionID
	 * @param digest
	 */
	public void setSessionID(BigInteger digest)
	{
		SID = digest.toByteArray();
		SID_BI = digest;
	}
	
	/**
	 * convert byte array to BigInteger
	 * @param src
	 * @return
	 */
	public static BigInteger Bytes2BI(byte[] src)
	{
		// ダイジェストを16進にする。
		StringBuffer dec = new StringBuffer("");
		for(int i = 0; i < src.length; i++)
		{
			int val = src[i] & 0xFF;
			if(val < 16)
			{
				dec.append("0");
			}
			dec.append(Integer.toString(val, 16));
		}
		
		return(new BigInteger(dec.toString(), 16));
	}
}




