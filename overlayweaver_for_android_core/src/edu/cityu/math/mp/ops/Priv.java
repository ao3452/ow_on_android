/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;

/**
 * Provides MP primitive methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Priv {

    public static short mul(short [] r, int rp, short a, short w, short c) {
        int t;
        t = w * a + c;
        r[rp] = Ldigit(t);
        c = Hdigit(t);
        return c;
    }

    public static int mul(short [] r, int rp, short a, short w, int c) {
        int t;
        t = w * a + c;
        r[rp] = Ldigit(t);
        c = Hdigit(t);
        return c;
    }

    public static short Ldigit(int t) {
        short d = (short) (t & Constants.DIGIT_MASK);

        return d;
    }

    public static short Hdigit(int t) {
        short d = (short) ((t >> Constants.DIGIT_BITS) & Constants.DIGIT_MASK);

        return d;
    }

    public static short Ldigit(long t) {
        short d = (short) (t & Constants.DIGIT_MASK);

        return d;
    }

    public static short Hdigit(long t) {
        short d = (short) ((t >> Constants.DIGIT_BITS) & Constants.DIGIT_MASK);

        return d;
    }

    public static short mul_add(short [] r, int rp, short a, short w, short c) {
        int t;
        t = w * a + c + r[rp];
        r[rp] = Ldigit(t);
        c = Hdigit(t);
        return c;
    }

    public static int mul_add_double(short [] r, int rp, short a, short w, int c) {
        long t;
        t = w * a * 2 + c + r[rp];
        r[rp] = Ldigit(t);
        c = (short) ((t >> Constants.DIGIT_BITS) & Constants.DOUBLE_DIGIT_MASK);
        return c;
    }

    public static int MP_num_bytes(MP_INT a) {
        return (Conv.MP_num_bits(a) + 7)/8;
    }

    public static short LBITS(short a) {
        short d = (short) (a & Constants.DIGIT_MASKl);
        return d;
    }

    public static short HBITS(short a) {
        short d = (short) ((a >> Constants.DIGIT_BITSl) & Constants.DIGIT_MASKl);
        return d;
    }

    public static short HBITS1(short a) {
        short d = (short) ((a & Constants.DIGIT_MASKl) << Constants.DIGIT_BITSl);
        return d;
    }

    public static void mul64(short [] lh, short bl, short bh) {
        short m, ml, lt, ht;
        lt = lh[0];
        ht = lh[1];
        m = (short) (bh * lt);
        lt = (short) (bl * lt);
        ml = (short) (bl * ht);
        ht = (short) (bh * ht);
        m += ml;
        if ((m & Constants.DIGIT_MASK) < ml)
            ht += HBITS1((short)1);
        ht += HBITS(m);
        ml = HBITS1(m);
        lt += ml;
        if ((lt & Constants.DIGIT_MASK) < ml)
            ht++;
        lh[0] = lt;
        lh[1] = ht;
    }
}
