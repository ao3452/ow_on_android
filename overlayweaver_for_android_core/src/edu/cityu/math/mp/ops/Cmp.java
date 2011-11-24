/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;


/**
 * Provides MP comparison methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Cmp {

    /**
     * Compare a and b
     * @param a Operand
     * @param b Operand
     * @return 1 if a > b, 0 if a = b, -1 if a < b
     */
    public static int MP_cmp(MP_INT a, MP_INT b) {
        int gt, lt;
        short t1, t2;

        if (a == null || b == null) {
            if (a != null) return -1;
            else if (b != null) return 1;
            else return 0;
        }

        if (a.neg != b.neg) {
            if (a.neg) return -1;
            else return 1;
        }

        if (!a.neg) {
            gt = 1;
            lt = -1;
        }
        else {
            gt = -1;
            lt = 1;
        }

        if (a.top > b.top) return gt;
        if (a.top < b.top) return lt;

        for (int i=a.top-1; i>=0; i--) {
            t1 = a.d[i];
            t2 = b.d[i];
            if (t1 > t2) return gt;
            if (t1 < t2) return lt;
        }
        return 0;
    }

    /**
     * Compare |a| and |b|
     * @param a Operand
     * @param b Operand
     * @return 1 if a > b, 0 if a = b, -1 if a < b
     */
    public static int MP_ucmp(MP_INT a, MP_INT b) {
        short t1, t2;

        if (a.top != b.top)
            return (a.top>b.top?1:-1);

        for (int i=a.top-1; i>=0; i--) {
            t1 = a.d[i];
            t2 = b.d[i];
            if (t1 != t2)
                return (t1>t2?1:-1);
        }
        return 0;
    }

    /**
     * Test if the n-th bit of (MP_INT a) is 1, n starts at 0
     * @param a
     * @param n
     * @return true if set, false if not
     */
    public static boolean MP_is_bit_set(MP_INT a, short n) {
        short i, j;

        if (n < 0) return false;
        i = (short) (n / Constants.DIGIT_BITS);
        j = (short) (n % Constants.DIGIT_BITS);
        if (a.top <= i)
            return false;

        if ((a.d[i] & ((short)1<<j)) == 0)
            return false;
        else
            return true;
    }

}
