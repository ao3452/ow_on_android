/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;

/**
 * Provides MP bit-wise shifting methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Shift {

    /**
     * Set r to 2*a<br>
     * Note: r can be a<br>
     * @param r
     * @param a
     * @return true if successful
     */
    public static boolean MP_lshift1(MP_INT r, MP_INT a) {
        short c, t;

        if (r != a) {
            r.neg = a.neg;
            r.top = a.top;
        }
        if (!r.MP_expand((a.top+1)*Constants.DIGIT_BITS)) return false;

        c = 0;
        for (int i=0; i<a.top; i++) {
            t = a.d[i];
            r.d[i] = (short) (((t << 1) | c) & Constants.DIGIT_MASK);
            c = (short) (((t & Constants.DIGIT_HBIT) != 0) ? 1 : 0);
        }

        if (c != 0) {
            r.d[r.top] = 1;
            r.top++;
        }
        return true;
    }

    /**
     * Set r to lower ceiling of a/2<br>
     * Note: r can be a<br>
     * @param r
     * @param a
     * @return true if successful
     */
    public static boolean MP_rshift1(MP_INT r, MP_INT a) {
        short c, t;

        if (a.MP_is_zero()) {
            r.top = 0;
            r.d[0] = 0;
            return true;
        }

        if (a != r) {
            if (!r.MP_expand(a.top*Constants.DIGIT_BITS)) return false;
            r.top = a.top;
            r.neg = a.neg;
        }

        c = 0;
        for (int i=a.top-1; i>=0; i--) {
            t = a.d[i];
            r.d[i] = (short) (((t >> 1) & Constants.DIGIT_MASK) | c);
            c = ((t&1) != 0)?Constants.DIGIT_HBIT:0;
        }
        r.MP_fix_top();
        return true;
    }

    /**
     * Shift (MP_INT from) n bits to the left and put the result to (MP_INT to)<br>
     *<br>
     * Note: -(MP_INT from) and (MP_INT to) can be overlapped if n is +ve and from<=to; otherwise, from >= to<br>
     * @param to
     * @param from
     * @param n
     * @return true if successful
     */
    public static boolean MP_lshift(MP_INT to, MP_INT from, int n) {
        int num_digit, lb, rb;
        short l;

        to.neg = from.neg;
        if (!to.MP_expand(from.top*Constants.DIGIT_BITS+n)) return false;

        num_digit = n / Constants.DIGIT_BITS;
        lb = n % Constants.DIGIT_BITS;
        rb = Constants.DIGIT_BITS - lb;

        to.d[from.top+num_digit] = 0;
        if (lb == 0)
            for (int i=from.top-1; i>=0; i--)
                to.d[num_digit+i] = from.d[i];
        else
            for (int i=from.top-1; i>=0; i--) {
                l = from.d[i];
                to.d[num_digit+i+1] |= (l>>rb)&Constants.DIGIT_MASK;
                to.d[num_digit+i] = (short) ((l << lb) & Constants.DIGIT_MASK);
            }

        for (int i=0; i<num_digit; i++)
            to.d[i] = 0;
        
        to.top = (short) (from.top + num_digit + 1);
        to.MP_fix_top();
        return true;
    }

    /**
     * Shift (MP_INT from) n bits to the right and put the result to (MP_INT to)<br>
     *<br>
     * Note: (MP_INT from) and (MP_INT to) can be overlapped if (to-from)<n for +ve n<br>
     * @param to
     * @param from
     * @param n
     * @return true if successful
     */
    public static boolean MP_rshift(MP_INT to, MP_INT from, int n) {
        int nd, rb, lb;
        short l, tmp, j;
        int from_index, to_index;

        nd = n / Constants.DIGIT_BITS;
        rb = n % Constants.DIGIT_BITS;
        lb = Constants.DIGIT_BITS - rb;

        if (nd > from.top) {
            to.top = 0;
            return true;
        }

        if (to != from) {
            to.neg = from.neg;
            if (!to.MP_expand((from.top-nd+1)*Constants.DIGIT_BITS)) return false;
        }

        from_index = nd;
        to_index = 0;
        j = (short) (from.top - nd);
        to.top = j;

        if (rb == 0) {
            for (int i=j+1; i>0; i--)
                to.d[to_index++] = from.d[from_index++];
        }
        else {
            l = from.d[from_index++];
            for (int i=1; i<j; i++) {
                tmp = (short) ((l >> rb) & Constants.DIGIT_MASK);
                l = from.d[from_index++];
                to.d[to_index++] = (short) ((tmp | (l << lb)) & Constants.DIGIT_MASK);
            }
            to.d[to_index++] = (short) ((l >> rb) & Constants.DIGIT_MASK);
        }

        to.d[to_index] = 0;
        to.MP_fix_top();
        return true;
    }



}
