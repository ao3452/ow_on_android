/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

import java.io.Serializable;

/**
 * Data structure to represent a point
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Point implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -522947826876867400L;
	/**
	 * Coordinate x of a point
	 */
	public Element x;
	/**
	 * Coordinate y of a point
	 */
	public Element y;

	/**
     *
     */
	public Point()
	{
		this.x = new Element();
		this.y = new Element();
	}

	/**
	 * Constructor with initial value
	 * 
	 * @param x
	 *                        x's initial value
	 * @param y
	 *                        y's initial value
	 */
	public Point(Element x, Element y)
	{
		this.x = new Element();
		this.y = new Element();
		this.x.copy(x);
		this.y.copy(y);
	}

	/**
	 * Copy another Point into this object
	 * 
	 * @param b
	 *                        the object to be copied
	 */
	public void copy(Point b)
	{
		this.x.copy(b.x);
		this.y.copy(b.y);
	}

	/**
	 * Generate and return an zero point object
	 * 
	 * @return zero point object
	 */
	public static Point get_ZERO_POINT()
	{
		Point zero = new Point();
		zero.x.hi[9] = (byte) 0xFF;
		zero.x.hi[10] = (byte) 0xFF;
		zero.x.hi[11] = (byte) 0xFF;
		zero.x.hi[12] = (byte) 0xFF;
		return zero;
	}

	/**
	 * Generate and return a g0 point object
	 * 
	 * @return g0 point object
	 */
	public static Point get_G0()
	{
		Point g0 = new Point();
		byte[] x_hi =
			{(byte) 0x0D, (byte) 0x84, (byte) 0xC0, (byte) 0x12, (byte) 0x50, (byte) 0x11, (byte) 0xA0,
				(byte) 0x01, (byte) 0x80, (byte) 0xE1, (byte) 0xE8, (byte) 0xD4, (byte) 0x1};
		byte[] x_lo =
			{(byte) 0x52, (byte) 0x30, (byte) 0x12, (byte) 0x01, (byte) 0xA4, (byte) 0x6A, (byte) 0x07,
				(byte) 0x72, (byte) 0x5B, (byte) 0x1C, (byte) 0x13, (byte) 0x22, (byte) 0x0};
		byte[] y_hi =
			{(byte) 0xC0, (byte) 0x00, (byte) 0x08, (byte) 0x61, (byte) 0x75, (byte) 0x15, (byte) 0x8A,
				(byte) 0x10, (byte) 0x56, (byte) 0x00, (byte) 0x04, (byte) 0x41, (byte) 0x01};
		byte[] y_lo =
			{(byte) 0x34, (byte) 0xAD, (byte) 0xD1, (byte) 0x0E, (byte) 0x80, (byte) 0x60, (byte) 0x54,
				(byte) 0xAD, (byte) 0x08, (byte) 0x65, (byte) 0x62, (byte) 0x2C, (byte) 0x00};

		g0.x.set_hi(x_hi);
		g0.x.set_lo(x_lo);
		g0.y.set_hi(y_hi);
		g0.y.set_lo(y_lo);
		return g0;
	}

	/**
	 * Test and determine if two points are equal in value
	 * 
	 * @param a
	 *                        the point to be tested against this object
	 * @return true if equal, false if different
	 */
	public boolean equal(Point a)
	{
		if(this.x.equal(a.x) && this.y.equal(a.y))
			return true;
		else
			return false;
	}

}
