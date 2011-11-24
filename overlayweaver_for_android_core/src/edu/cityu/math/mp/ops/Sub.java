/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;

/**
 * Provides MP substraction methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Sub {

    /**
     * Set r to a - b<br>
     * Note: r can be a or b<br>
     * @param r
     * @param a
     * @param b
     * @return true if successful
     */
    public static boolean MP_sub(MP_INT r, MP_INT a, MP_INT b) {
        MP_INT temp;
        boolean add=false;
        boolean neg=false;
        boolean i;
        short max;

        if (a.neg) {
            if (b.neg) {
                temp = a;
                a = b;
                b = temp;
            }
            else {
                add = true;
                neg = true;
            }
        }
        else {
            if (b.neg) {
                add = true;
                neg = false;
            }
        }

        i = (a.top > b.top);
        if (add) {
            r.MP_expand(((i?a.top:b.top)+1)*Constants.DIGIT_BITS);
            if (i)
                Add.MP_qadd(r, a, b);
            else
                Add.MP_qadd(r, b, a);
            r.neg = neg;
            return true;
        }

        // do (a-b)
        max = (i?a.top:b.top);
        r.MP_expand(max*Constants.DIGIT_BITS);
        if (Cmp.MP_ucmp(a, b) < 0) {
            MP_qsub(r, b, a);
            r.neg = true;
        }
        else {
            MP_qsub(r, a, b);
            r.neg = false;
        }
        
        return true;
    }

    /**
     * Set r to a - b<br>
     * where<br>
     * 1) a and b are treated as unsigned --- r.neg will not be updated<br>
     * 2) |a| >= |b|<br>
     * 3) r has enough memory<br>
     *<br>
     * This function does not check the validity of the inputs for speed<br>
     *<br>
     * Note: r can be a or b<br>
     * @param r
     * @param a
     * @param b
     */
    public static void MP_qsub(MP_INT r, MP_INT a, MP_INT b) {
        short max, min;
        short t1, t2;
        int i;
        boolean c;

        max = a.top;
        min = b.top;

        c = false;
        for (i=0; i<min; i++) {
            t1 = a.d[i];
            t2 = b.d[i];
            if (c) {
                c = (t1 <= t2);
                t1 = (short) (t1 - t2 - 1);
            }
            else {
                c = (t1 < t2);
                t1 = (short) (t1 - t2);
            }

            r.d[i] = (short) (t1 & Constants.DIGIT_MASK);
        }

        if (c) {
            while (i < max) {
                t1 = a.d[i];
                t2 = (short) ((t1 - 1) & Constants.DIGIT_MASK);
                r.d[i] = t2;
                i++;
                if (t1 > t2) break;
            }
        }

        for (; i<max; i++)
            r.d[i] = a.d[i];

        r.top = max;
        r.MP_fix_top();
    }

}
