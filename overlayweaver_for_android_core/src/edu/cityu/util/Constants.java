/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

import java.io.Serializable;

/**
 * Contains all constants used in the API
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Constants
{
	
	private static byte[] hi = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	private static byte[] lo = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	private static Element one = new Element(hi, lo);

	public static final int BIGINT_LEN = 19;
	public static final int CPELEMENT_LEN = 20;
	public static final int CONSTANT_M = 97;
	public static final int UINT_LEN = 8;
	public static final int ELEMENT_LEN = (CONSTANT_M - 1) / UINT_LEN + 1;
	public static final int POINT_LEN = ELEMENT_LEN * 4;

	// TODO: these two variables are not used yet
	public static final int UINT_MSB = 0x80;
	public static final int VALUE_K = 12;

	// constants used in MP_LIB
	public static final int MP_CTX_NUM = 12;
	public static final int MP_DEFAULT_BITS = 1280;
	public static final short DIGIT_BITS = 8;
	public static final short DIGIT_BITSl = 4;
	public static final short DIGIT_MASK = 0xFF;
	public static final short DIGIT_MASKl = 0x000F;
	public static final int DOUBLE_DIGIT_MASK = 0x0000FFFF;
	public static final short DIGIT_BYTES = 1;
	public static final byte DIGIT_HBIT = (byte) 0x80;

	public static final BigInt GORDER = BigInt.get_GORDER();
	public static final CpElement ECORDER = CpElement.get_ECORDER();
	public static final Element ELEMENT_ONE = one;
	/* default constructor of Element will generate a zero Element */
	public static final Element ELEMENT_ZERO = new Element();
	public static final Point ZERO_POINT = Point.get_ZERO_POINT();
	public static final Point g0 = Point.get_G0();
}
