/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.ibe;

import edu.cityu.bp.EtaT_Pairing;
import edu.cityu.hash.SHA256;
import edu.cityu.util.ExtElement;
import edu.cityu.util.Point;

/**
 * Contains IBE Decryption methods
 * 
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class IBEDecrypt
{

	/**
	 * IBE decrypt method
	 * 
	 * @param cipher
	 *                        Encrypted cipher object, contains C1 and C2
	 * @param privateKey
	 *                        receiver's private key
	 * @return decrypted plaintext bytes
	 */
	public static byte[] decrypt(IBECipher cipher, Point privateKey)
	{
		ExtElement rawSessionKey = new ExtElement();
		int k = 0;
		Point R = cipher.getR();
		byte[] cipherInBytes = cipher.getCipher();

		EtaT_Pairing.etaT_pairing(privateKey, R, rawSessionKey);
		int msgLen = cipherInBytes.length;
		byte[] msgInBytes = new byte[msgLen];
		int msgIndex = 0;
		int digestIndex = 0;

		while(true)
		{
			byte[] rawSessionKeyInBytes = rawSessionKey.getBytes();
			byte[] hashSource = Utils.concatenate(k, rawSessionKeyInBytes);
			byte[] digest = SHA256.SHA256(hashSource); // digest has length 32;

			while((msgIndex < msgLen) && (digestIndex < 32))
			{
				msgInBytes[msgIndex] = (byte) (cipherInBytes[msgIndex] ^ digest[digestIndex]);
				msgIndex++;
				digestIndex++;
			}

			if(msgIndex >= msgLen)
				break;
			else
				digestIndex = 0;
		}

		return msgInBytes;
	}
}
