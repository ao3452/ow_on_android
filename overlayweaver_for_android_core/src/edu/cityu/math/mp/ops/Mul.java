/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;

/**
 * Provides MP multiplication methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Mul {
    /**
     * MP multiplication<br>
     * This is Algorithm 14.12 on p.595 of HAC<br>
     * Note: r must be different from a and b
     *
     * @param r used to store the result
     * @param a Operand
     * @param b Operand
     * @return true if successful
     */
    public static boolean MP_mul(MP_INT r, MP_INT a, MP_INT b) {
        short al, bl, rlen;
        int ap, bp, rp;

        al = a.top;
        bl = b.top;
        if (al==0 || bl==0) {
            r.top = 0;
            return true;
        }

        rlen = (short) (al + bl);
        if (!r.MP_expand(rlen*Constants.DIGIT_BITS)) return false;
        r.top = rlen;
        r.neg = a.neg ^ b.neg;
        ap = 0;
        bp = 0;
        rp = 0;

        r.d[al] = MP_mul_digit(r.d, a.d, al, b.d[bp++]);
        rp++;
        for (int i=1; i<bl; i++) {
            r.d[rp+al] = MP_mul_add_digit(r.d, rp, a.d, al, b.d[bp++]);
            rp++;
        }

        if (r.d[rlen-1]==0) r.top--;    // like MP_fix_top but only need to check the MSB
        return true;
    }

    /**
     * Set r to a*w where a has num digits and w has only one digit.<br>
     * The original value pointed by rp is overridden<br>
     * e.g. a = 95, num = 2 and w = 6 in radix 10;<br>
     *      r = 70 and 5 is returned.<br>
     * @param r
     * @param a
     * @param num
     * @param w
     * @return carry
     */
    public static short MP_mul_digit(short[] r, short[] a, int num, short w) {
        short carry = 0;
        int rp = 0, ap = 0;

        while (true) {
            carry = Priv.mul(r, rp, a[ap], w, carry);
            if (--num == 0) break;
            carry = Priv.mul(r, rp+1, a[ap+1], w, carry);
            if (--num == 0) break;
            carry = Priv.mul(r, rp+2, a[ap+2], w, carry);
            if (--num == 0) break;
            carry = Priv.mul(r, rp+3, a[ap+3], w, carry);
            if (--num == 0) break;

            ap+=4;
            rp+=4;
        }

        return carry;
    }

    /*
     * Set r to r + (a*w) where a has num digits and w is only one digit.
     * The origianl value pointed by r is overridden
     *
     * Return a carry
     *
     * e.g. r = 99, a = 95, num = 2 and w = 6 in radix 10;
     *      r = 69 and 6 is returned.
     */
    private static short MP_mul_add_digit(short[] r, int rp, short[] a, int num, short w) {
        short carry = 0;
        int rrp = rp, ap = 0;

        while (true) {
            carry = Priv.mul_add(r, rrp, a[ap], w, carry);
            if (--num == 0) break;
            carry = Priv.mul_add(r, rrp+1, a[ap+1], w, carry);
            if (--num == 0) break;
            carry = Priv.mul_add(r, rrp+2, a[ap+2], w, carry);
            if (--num == 0) break;
            carry = Priv.mul_add(r, rrp+3, a[ap+3], w, carry);
            if (--num == 0) break;

            ap+=4;
            rrp+=4;
        }

        return carry;
    }

    /*
     * Set r to a*a
     * This is Algorithm 14.16 on p.597 of HAC.  It should be almost twice
     * the speed of MP_mul(r, a, a) when the machine supports TETRA_DIGIT.
     * Currently only EIGHT_BIT and SIXTEEN_BIT supports TETRA_DIGIT.
     * Otherwise, this algorithm may be SLOWER than MP_mul(r, a, a)!
     *
     * Note: -r must not be a.
     *       -It gives 23% improvement over MP_mul(r, a, a) running on a Palm III
     *       -It takes 199msec to compute the square of a 1024-bit integer when DIGIT is (unsigned char)
     */
    /**
     * Set r to a*a<br>
     * This is Algorithm 14.16 on p.597 of HAC.  It should be almost twice<br>
     * the speed of MP_mul(r, a, a) when the machine supports TETRA_DIGIT.<br>
     * Currently only EIGHT_BIT and SIXTEEN_BIT supports TETRA_DIGIT.<br>
     * Otherwise, this algorithm may be SLOWER than MP_mul(r, a, a)!<br>
     *<br>
     * Note: -r must not be a.<br>
     *       -It gives 23% improvement over MP_mul(r, a, a) running on a Palm III<br>
     *       -It takes 199msec to compute the square of a 1024-bit integer when DIGIT is (unsigned char)<br>
     * @param r must not be a
     * @param a
     * @return true if successful
     */
    public static boolean MP_sqr(MP_INT r, MP_INT a) {
        short al, rlen;
        int carry;

        al = a.top;
        if (al == 0) {
            r.top = 0;
            return true;
        }

        rlen = (short) ((al << 1) + 1);
        if (!r.MP_expand(rlen*Constants.DIGIT_BITS)) return false;
        r.top = rlen;
        r.neg = false;

        for (int i=0; i<rlen; i++) r.d[i] = 0;
        for (int i=0; i<al; i++) {
            carry = r.d[i<<1];
            carry = Priv.mul(r.d, i<<1, a.d[i], a.d[i], carry);
            for (int j=i+1; j<al; j++) {
                carry = Priv.mul_add_double(r.d, i+j, a.d[i], a.d[j], carry);
            }
            r.d[i+al] += Priv.Ldigit(carry);
            r.d[i+al+1] = Priv.Hdigit(carry);
        }

        r.MP_fix_top();
        return true;
    }

}
