/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.ibe;

import java.io.Serializable;

import edu.cityu.be.CryptRand;
import edu.cityu.bp.PointArith; //import edu.cityu.test.Main;
import edu.cityu.util.BigInt;
import edu.cityu.util.Constants;
import edu.cityu.util.Point;

/**
 * Contains methods for third party key generator<br>
 * <br>
 * Including private key generation, master public key generation
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class PublicKeyGenerator implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	BigInt t;
	Point MPK;

	/**
     *
     */
	public PublicKeyGenerator()
	{
		//System.out.println("initialize PKG");
		Point g0 = Constants.g0;
		t = CryptRand.RandBigInt();
		MPK = new Point();
		// System.out.println(Main.output_BigInt(t));
		PointArith.point_mult(t, g0, MPK);
	}

	/**
	 * refresh the generator object, generates new random t and master public key
	 */
	public void refresh()
	{
		Point g0 = Constants.g0;
		t = CryptRand.RandBigInt();
		MPK = new Point();
		PointArith.point_mult(t, g0, MPK);
	}

	/**
	 * Generate receiver's private key i.e. Da = tH1(IDa);
	 * 
	 * @param ID
	 *                        receiver's public key
	 * @return receiver's private key as type of Point
	 */
	public Point getPrivateKey(String ID)
	{
		Point hashID = PointArith.map2Point(ID);
		Point pKey = new Point();
		PointArith.point_mult(this.t, hashID, pKey);
		return pKey;
	}

	/**
	 * 
	 * @return master public key
	 */
	public Point getMPK()
	{
		return MPK;
	}

}
