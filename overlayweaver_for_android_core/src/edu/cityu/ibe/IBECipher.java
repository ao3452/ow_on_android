/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.ibe;

import java.io.Serializable;

import edu.cityu.util.Point;

/**
 * This is a wrapper for IBE generated cipher<br>
 * <br>
 * It contains a random point (R = rP) and a cipher text byte array
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class IBECipher implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9189010547306649989L;
	private Point R;
	private byte[] cipher;

	/**
	 * 
	 * @return R, R=C2=g^r
	 */
	public Point getR()
	{
		return R;
	}

	/**
	 * Set R to with a new value
	 * 
	 * @param R
	 *                        the value to be copied
	 */
	public void setR(Point R)
	{
		this.R = R;
	}

	/**
	 * 
	 * @return cipher text data, cipher=c1
	 */
	public byte[] getCipher()
	{
		return cipher;
	}

	/**
	 * Set cipher to new value
	 * 
	 * @param cipher
	 *                        new value for cipher
	 */
	public void setCipher(byte[] cipher)
	{
		this.cipher = cipher;
	}

	/**
     *
     */
	public IBECipher()
	{
		this.R = new Point();
		cipher = null;
	}
}
