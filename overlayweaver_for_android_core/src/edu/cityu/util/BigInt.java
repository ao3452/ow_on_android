/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

import java.io.Serializable;

import edu.cityu.math.mp.MP_CTX;
import edu.cityu.math.mp.MP_INT;
import edu.cityu.math.mp.ops.Add;
import edu.cityu.math.mp.ops.Conv;
import edu.cityu.math.mp.ops.Div;
import edu.cityu.math.mp.ops.Mod;

//import edu.cityu.test.Main;

/**
 * Data structure for representing big integers
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class BigInt implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public byte[] value = new byte[Constants.BIGINT_LEN];

	public BigInt()
	{
	}

	public BigInt(byte[] value)
	{
		if(value.length == Constants.BIGINT_LEN)
		{
			for(int i = 0; i < Constants.BIGINT_LEN; i++)
			{
				this.value[i] = value[i];
			}
		}
	}

	public void copy(BigInt a)
	{
		for(int i = 0; i < Constants.BIGINT_LEN; i++)
			this.value[i] = a.value[i];
	}

	public boolean expend(int size)
	{
		if(size <= this.value.length)
			return true;

		byte[] temp = new byte[size];
		for(int i = 0; i < this.value.length; i++)
		{
			temp[i] = this.value[i];
		}

		value = temp;
		return true;
	}

	/*
	 * for generating a GORDER
	 */
	public static BigInt get_GORDER()
	{
		byte[] value =
			{(byte) 0x7A, (byte) 0x46, (byte) 0xE0, (byte) 0x90, (byte) 0x1F, (byte) 0x72, (byte) 0x54,
				(byte) 0x6f, (byte) 0x8D, (byte) 0x3E, (byte) 0xBA, (byte) 0x71, (byte) 0x7E,
				(byte) 0x08, (byte) 0x64, (byte) 0x41, (byte) 0x35, (byte) 0xDE, (byte) 0x41};

		BigInt GORDER = new BigInt(value);
		return GORDER;
	}

	/*
	 * private function pad zero if len is not BIGINT_LEN
	 */
	private static void bigint_private_pad(BigInt A, int len, BigInt padA)
	{
		int i;
		for(i = 0; i < len; i++)
			padA.value[Constants.BIGINT_LEN - 1 - i] = A.value[len - 1 - i];

		for(i = 0; i < (Constants.BIGINT_LEN - len); i++)
			padA.value[i] = 0;
	}

	/*
	 * private function compute modular reminder of a MP_INT A over the subgroup order GORDER
	 */
	private static boolean bigint_private_INT_mod(MP_INT mp_A, BigInt modA)
	{
		int len;

		MP_CTX ctx = new MP_CTX();
		MP_INT q = new MP_INT();
		MP_INT r = new MP_INT();
		BigInt gorder = Constants.GORDER;
		MP_INT mp_GORDER = Conv.MP_bint2bn(gorder, Constants.BIGINT_LEN);
		//
		// System.out.println("mp_A="+Main.output_MP_INT(mp_A));
		// System.out.println("gorder="+Main.output_MP_INT(mp_GORDER));

		// System.out.println(Main.output_MP_INT(mp_GORDER));

		if(!Div.MP_div(q, r, mp_A, mp_GORDER, ctx))
		{
			return false;
		}

		// System.out.println("q="+Main.output_MP_INT(q));
		// System.out.println("r="+Main.output_MP_INT(r));

		if(r.MP_is_zero())
		{
			len = 0;
			// System.out.println("r is zero");
		}
		else
			len = Conv.MP_bn2bint(r, modA);

		// pad zero if the conversion result is not BigInt long
		bigint_private_pad(modA, len, modA);

		return true;
	}

	/*
	 * module motiplication for big integer C = A + B mod GORDER A, B, C can be the same address
	 */
	public static boolean bigint_add(BigInt A, BigInt B, BigInt C)
	{
		boolean rt;
		MP_INT mp_A = Conv.MP_bint2bn(A, Constants.BIGINT_LEN);
		MP_INT mp_B = Conv.MP_bint2bn(B, Constants.BIGINT_LEN);
		MP_INT mp_C = Conv.MP_bint2bn(C, Constants.BIGINT_LEN);

		Add.MP_add(mp_C, mp_A, mp_B);
		rt = bigint_private_INT_mod(mp_C, C);

		return rt;
	}

	/*
	 * module multiplication for big integer C = A*B mod GORDER A, B, C can be the same address
	 */
	public static boolean bigint_mult(BigInt A, BigInt B, BigInt C)
	{
		MP_INT mp_C = new MP_INT();
		MP_INT mp_A = Conv.MP_bint2bn(A, Constants.BIGINT_LEN);
		MP_INT mp_B = Conv.MP_bint2bn(B, Constants.BIGINT_LEN);
		MP_INT mp_GORDER = Conv.MP_bint2bn(Constants.GORDER, Constants.BIGINT_LEN);
		MP_CTX ctx = new MP_CTX();
		int len;

		if(!Mod.MP_mod_mul(mp_C, mp_A, mp_B, mp_GORDER, ctx))
			return false;

		if(mp_C.MP_is_zero())
			len = 0;
		else
			len = Conv.MP_bn2bint(mp_C, C);

		// pad zero if the conversion result is not BigInt long
		bigint_private_pad(C, len, C);

		return true;
	}

	/*
	 * convert a BigInt with least significant byte stored at low offset to one stored at high offset or convert a BigInt with least
	 * significant byte stored at high offset to one stored at low offset
	 * 
	 * convert A to c_A (A and c_A can be the same)
	 */
	public static void bigint_endian_conv(BigInt A, BigInt c_A)
	{
		BigInt temp = new BigInt();
		int i;

		for(i = 0; i < Constants.BIGINT_LEN; i++)
			temp.value[Constants.BIGINT_LEN - 1 - i] = A.value[i];

		c_A.copy(temp);
	}

	/*
	 * compute modular reminder of A over the subgroup order GORDER A could be modA rt 0 if A is the same as group order
	 */
	public static void bigint_mod(BigInt A, BigInt modA)
	{
		MP_INT mp_A = Conv.MP_bint2bn(A, Constants.BIGINT_LEN);

		bigint_private_INT_mod(mp_A, modA);
		// System.out.println("mp_A="+Main.output_MP_INT(mp_A));
		// System.out.println("modA="+Main.output_BigInt(modA));
	}
}
