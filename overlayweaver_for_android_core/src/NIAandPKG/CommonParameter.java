package NIAandPKG;

import java.io.Serializable;

import edu.cityu.util.Point;

/**
 * @author tanaka
 *
 */
public class CommonParameter implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//master public key
	Point masterPubKey;
	//its own private key
	Point itsPriKey;
	//hash function
	//String hash = "SHA-1";
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub

	}
	
	public Point getMasterPubKey()
	{
		return masterPubKey;
	}

	public void setMasterPubKey(Point masterPubKey)
	{
		this.masterPubKey = masterPubKey;
	}

	public Point getItsPriKey()
	{
		return itsPriKey;
	}

	public void setItsPriKey(Point itsPriKey)
	{
		this.itsPriKey = itsPriKey;
	}
/*
	public String getHash()
	{
		return hash;
	}

	public void setHash(String hash)
	{
		this.hash = hash;
	}
*/
}
