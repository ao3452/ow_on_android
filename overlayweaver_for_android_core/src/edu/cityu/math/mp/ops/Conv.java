/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.BigInt;
import edu.cityu.util.Constants;

/**
 * Provides MP convertion methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Conv {

    /**
     * Convert a BigInt s to MP_INT and return it as (MP_INT)<br>
     * len is the length of s in number of bytes.<br>
     * <br>
     * Note: ignore negative (s->neg)
     * @param s The BigInt to be converted
     * @param len Length of s, in bytes
     * @return Converted MP_INT
     */
    public static MP_INT MP_bint2bn(BigInt s, int len) {
        MP_INT ret = null;
        int n, i, m, index;
        short l;
        
        ret = new MP_INT();
        if (ret == null) {
            return null;
        }
        
        l = 0;
        index = 0;
        n = len;
        if (n==0) {
            ret.top = 0;
            return ret;
        }
        
        if (!ret.MP_expand(n*Constants.DIGIT_BITS))
            return null;
        
        i = ((n-1)/Constants.DIGIT_BYTES) + 1;
        m = (n-1) % Constants.DIGIT_BYTES;
        
        ret.top = (short) i;
        while (n-- > 0) {
            l = (short) ((l << 8L & Constants.DIGIT_MASK) | s.value[index++]);
            if (m-- == 0) {
                ret.d[--i]=(short) (l & 0xff);
                l = 0;
                m = Constants.DIGIT_BYTES - 1;
            }
        }
        
//        System.out.println(ret.d[ret.top-1]);
//        System.out.println(ret.d[ret.top-1] == 0);
        ret.MP_fix_top();
        return ret;
    }
    
    /*
     * Convert (MP_INT a) to a (BigInt to)
     * Return length of to
     * Note: ignore negative
     */
    /**
     * Convert (MP_INT a) to a (BigInt to)<br>
     * Note: ignore negative
     * @param a
     * @param to
     * @return length of BitInt to
     */
    public static int MP_bn2bint(MP_INT a, BigInt to) {
        int n, i, index;
        short l;
        byte [] temp = new byte [Constants.BIGINT_LEN];
        
        n = i = Priv.MP_num_bytes(a);
        index = 0;
        while (i-- > 0) {
            l = a.d[i/Constants.DIGIT_BYTES];
            temp[index++] = (byte) ((l >> (8 * (i % Constants.DIGIT_BYTES))) & Constants.DIGIT_MASK);
        }

        to.copy(new BigInt(temp));
        return n;
    }

    /**
     * Convert a number in String a to MP_INT<br>
     * a is in HEX
     * @param a
     * @return Converted MP_INT
     */
    public static MP_INT MP_string2bn(String a) {
        MP_INT ret;
        boolean neg = false;
        short [] raw;
        short raw_digit, l, k;
        int i, j, m, h;

        if ((a == null) || (a.length()==0))
            return null;
        if (a.startsWith("-")) {
            a = a.substring(1);
            neg = true;
        }

        ret = new MP_INT();
        raw = new short [a.length()];

        for (i=0; i<a.length(); i++) {
            if ((raw_digit = (short) Character.digit(a.charAt(i), 16)) != -1)
                raw[i] = raw_digit;
            else
                break;
        }

        ret.MP_expand(i*4);

        j = i;
        m = 0;
        h = 0;
        while (j > 0) {
            m = ((Constants.DIGIT_BYTES*2)<=j)?(Constants.DIGIT_BYTES*2):j;
            l = 0;
            while (true) {
                k = raw[j-m];
                l = (short) ((l << 4) | k);

                if (--m <= 0) {
                    ret.d[h++] = l;
                    break;
                }
            }
            j -= (Constants.DIGIT_BYTES*2);
        }

        ret.top = (short) h;
        ret.MP_fix_top();
        ret.neg = neg;

        return ret;
    }

    /**
     * Convert (MP_INT a) to a hex string
     * @param a
     * @return
     */
    public static String MP_bn2string(MP_INT a) {
        String hs = "0123456789ABCDEF";
        String strP = "";
        short v;
        boolean z = false;

        if (a == null)
            return null;

        if (a.neg)
            strP += "-";
        if (a.top == 0)
            strP += "0";

        for (int i=a.top-1; i>=0; i--) {
            for (int j=Constants.DIGIT_BITS-8; j>=0; j-=8) {
                v = (short) ((a.d[i] >> (long) j) & 0xFF);
                if (z || (v!=0)) {
                    strP += hs.charAt(v>>4);
                    strP += hs.charAt(v&0x0F);
                    z = true;
                }
            }
        }

        return strP;
    }

    /**
     * Count number of bits of a MP_INT
     * @param a
     * @return Number of bits of (MP_INT a)
     */
    public static int MP_num_bits(MP_INT a) {
        short l = 0;
        int i;

        a.MP_fix_top();
        if (a.top == 0) return 0;
        l = (short) (a.d[a.top - 1] & 0xff);
        i = (a.top-1)*Constants.DIGIT_BITS;

        return (i+MP_num_bits_digit(l));
    }

    /**
     * Count number of bits of a digit
     * @param l
     * @return number of bits of a digit
     */
    public static int MP_num_bits_digit(short l) {
        byte [] bits = {
            0,1,2,2,3,3,3,3,4,4,4,4,4,4,4,4,
            5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
            8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        };

        return (bits[l]);
    }

}
