/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

import java.io.Serializable;

/**
 * Data structure representing Element
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Element implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9011437076467349325L;
	/**
	 * Higher bit of a word of an Element
	 */
	public byte[] hi = new byte[Constants.ELEMENT_LEN];
	/**
	 * Lower bit of a word of an Element
	 */
	public byte[] lo = new byte[Constants.ELEMENT_LEN];

	/**
     *
     */
	public Element()
	{
		// seems java will auto initialize all var to 0
		// for (int i=0; i<Constants.ELEMENT_LEN; i++) {
		// this.hi[i] = 0;
		// this.lo[i] = 0;
		// }
	}

	/**
	 * Constructor with initial value
	 * 
	 * @param hi
	 *                        higher bits initial value
	 * @param lo
	 *                        lower bits initial value
	 */
	public Element(byte[] hi, byte[] lo)
	{
		this.set_hi(hi);
		this.set_lo(lo);
	}

	/**
	 * Copy hi array into hi[] in this object
	 * 
	 * @param hi
	 *                        value of hi
	 */
	public void set_hi(byte[] hi)
	{
		if(hi.length == Constants.ELEMENT_LEN)
		{
			for(int i = 0; i < Constants.ELEMENT_LEN; i++)
				this.hi[i] = hi[i];
		}
	}

	/**
	 * Copy lo array into lo[] in this object
	 * 
	 * @param lo
	 *                        value of lo
	 */
	public void set_lo(byte[] lo)
	{
		if(lo.length == Constants.ELEMENT_LEN)
		{
			for(int i = 0; i < Constants.ELEMENT_LEN; i++)
				this.lo[i] = lo[i];
		}
	}

	/**
	 * Clear this object, reset value to 0
	 */
	public void set_zero()
	{
		for(int i = 0; i < Constants.ELEMENT_LEN; i++)
		{
			this.hi[i] = 0;
			this.lo[i] = 0;
		}
	}

	/**
	 * Copy another Element into this object
	 * 
	 * @param ele
	 *                        the Element to be copied
	 */
	public void copy(Element ele)
	{
		this.set_hi(ele.hi);
		this.set_lo(ele.lo);
	}

	/**
	 * Get an Element one
	 * 
	 * @return Element one
	 */
	public static Element get_ELEMENT_ONE()
	{
		Element one = new Element();
		one.lo[0] = 1;
		return one;
	}

	/**
	 * Test to see if two Elements are equal
	 * 
	 * @param a
	 *                        The Element to be tested
	 * @return true if equal, false if different
	 */
	public boolean equal(Element a)
	{
		for(int i = 0; i < Constants.ELEMENT_LEN; i++)
		{
			if(this.hi[i] != a.hi[i] || this.lo[i] != a.lo[i])
				return false;
		}
		return true;
	}

}
