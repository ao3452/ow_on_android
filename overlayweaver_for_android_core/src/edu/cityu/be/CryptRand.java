/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.be;

import edu.cityu.rand.Generator;
import edu.cityu.util.BigInt;
import edu.cityu.util.Constants;

/**
 * Contains methods for random number generator
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class CryptRand {

    private static Generator g = new Generator();

    /**
     * generate len bytes of random data and return
     * @param len length of random data in bytes
     * @return the generated random data
     */
    public static byte[] CryptRand(int len) {
        int i = len/16;
        int r = len%16;
        int index = 0;

        byte[] rand = new byte[len];
        for (int j=0; j<i; j++) {
            byte[] temp = g.genRand();
            for (int k=0; k<16; k++)
                rand[index++] = temp[k];
        }
        byte[] temp = g.genRand();
        for (int k=0; k<r; k++)
            rand[index++] = temp[k];

        return rand;
    }

    /**
     * Generate a random BigInt, which is neither equal to zero or group order
     * @return BigInt with random value
     */
    public static BigInt RandBigInt() {
        int tag = 1;
        BigInt zero = new BigInt();
        BigInt r = new BigInt();

        while (tag != 0) {
            byte[] temp = CryptRand(Constants.BIGINT_LEN);
            byte[] value = new byte [Constants.BIGINT_LEN];

            for (int i=0; i<Constants.BIGINT_LEN; i++) {
                value[i] = temp[i];
            }

            r = new BigInt(value);

            if (r.equals(zero) || r.equals(Constants.GORDER))
                tag = 1;
            else
                tag = 0;
        }

        return r;
    }
}
