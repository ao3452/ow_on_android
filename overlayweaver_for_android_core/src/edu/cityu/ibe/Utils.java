/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.ibe;

/**
 * Contains utiltiy methods supporting IBE encryption/decryption
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class Utils {
    /**
     * Append integer k (32 bits) as 4 bytes to source array
     * @param k integer to be appended
     * @param source array to be appended
     * @return new array with k appended to source
     */
    public static byte[] concatenate(int k, byte[] source) {
        byte[] result = new byte[source.length+4];

        int i = 0;
        for (i=0; i<source.length; i++) {
            result[i] = source[i];
        }

        result[i++] = (byte) (k >> 24 & 0xFF);
        result[i++] = (byte) (k >> 16 & 0xFF);
        result[i++] = (byte) (k >> 8 & 0xFF);
        result[i++] = (byte) (k & 0xFF);

        return result;
    }
}
