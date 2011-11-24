/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.ibe;

import edu.cityu.be.CryptRand;
import edu.cityu.bp.EtaT_Pairing;
import edu.cityu.bp.PointArith;
import edu.cityu.hash.SHA256; //import edu.cityu.test.Main;
import edu.cityu.util.BigInt;
import edu.cityu.util.Constants;
import edu.cityu.util.ExtElement;
import edu.cityu.util.Point;

/**
 * Contains IBE Encryption methods
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class IBEEncrypt
{

	/**
	 * IBE encrypt method
	 * 
	 * @param ID
	 *                        public key of the receiver
	 * @param msgInBytes
	 *                        plaintext in byte array
	 * @param MPK
	 *                        master public key from the third party key generator
	 * @return cipher object, contains C1 and C2
	 */
	public static IBECipher encrypt(String ID, byte[] msgInBytes, Point MPK)
	{
		IBECipher cipherObj = new IBECipher();
		BigInt r = CryptRand.RandBigInt();
		ExtElement rawSessionKey = new ExtElement();
		Point R = new Point();
		Point P = Constants.g0;
		int k = 0;

		// set R
		PointArith.point_mult(r, P, R);
		cipherObj.setR(R);

		// hash ID to point, Q = H1(ID)
		Point publicKey = PointArith.map2Point(ID);
		// compuer Q = Q^r
		PointArith.point_mult(r, publicKey, publicKey);
		// get e(Q, T)
		EtaT_Pairing.etaT_pairing(publicKey, MPK, rawSessionKey);

		int cipherLen = msgInBytes.length;
		byte[] cipherInBytes = new byte[cipherLen];
		int cipherIndex = 0;
		int digestIndex = 0;

		while(true)
		{
			byte[] rawSessionKeyInBytes = rawSessionKey.getBytes();
			byte[] hashSource = Utils.concatenate(k, rawSessionKeyInBytes);
			byte[] digest = SHA256.SHA256(hashSource); // digest has length 32;

			while((cipherIndex < cipherLen) && (digestIndex < 32))
			{
				cipherInBytes[cipherIndex] =
					(byte) (msgInBytes[cipherIndex] ^ digest[digestIndex]);
				cipherIndex++;
				digestIndex++;
			}

			if(cipherIndex >= cipherLen)
				break;
			else
				digestIndex = 0;
		}

		cipherObj.setCipher(cipherInBytes);
		return cipherObj;
	}
}
