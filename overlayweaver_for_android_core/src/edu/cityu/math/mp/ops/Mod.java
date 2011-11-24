/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_CTX;
import edu.cityu.math.mp.MP_INT;

/**
 * Provides MP modular methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Mod {

    /**
     * Set r to a * b (mod m)<br>
     *<br>
     * Classical modular multiplication<br>
     * (Algorithm 14.28 on p.600 of HAC)<br>
     *<br>
     * Note: - For speed, this function would not check the validity of<br>
     * the inputs, i.e. a, b and m are positive integers<br>
     *       - Again for speed, the inputs a and b should be < m<br>
     * @param r
     * @param a
     * @param b
     * @param m
     * @param ctx
     * @return true if successful
     */
    public static boolean MP_mod_mul(MP_INT r, MP_INT a, MP_INT b, MP_INT m, MP_CTX ctx) {
        MP_INT t;

        t = ctx.t[ctx.tos++];

        if (Cmp.MP_cmp(a, b)==0) {
            if (!Mul.MP_sqr(t, a)) {
                ctx.tos--;
                return false;
            }
        }
        else {
            if (!Mul.MP_mul(t, a, b)) {
                ctx.tos--;
                return false;
            }
        }

        if (!Div.MP_div(null, r, t, m, ctx)) {
            ctx.tos--;
            return false;
        }

        return true;
    }

    /**
     * Set r to a^b mod m<br>
     *<br>
     * Classical Left-to-right binary exponentiation (repeated sqare-and-multiply)<br>
     * (Algorithm 14.79 on p.615 of HAC)<br>
     *<br>
     * Note: r can be a
     * @param r
     * @param a
     * @param b
     * @param m
     * @param ctx
     * @return true if successful
     */
    public static boolean MP_mod_exp(MP_INT r, MP_INT a, MP_INT b, MP_INT m, MP_CTX ctx) {
        int i, bits;
        MP_INT A, t1, t2;

        t1 = ctx.t[ctx.tos++];
        t2 = ctx.t[ctx.tos++];
        A = ctx.t[ctx.tos++];

        i = Cmp.MP_cmp(a, m);
        if (i == 0) {
            r.MP_zero();
            ctx.tos -= 3;
            return false;
        }
        else if (i > 0) {
            if (!Div.MP_div(null, t2, a, m, ctx)) {
                ctx.tos -= 3;
                return false;
            }
        }
        else {
            if (!t2.copy(a)) {
                ctx.tos -= 3;
                return false;
            }
        }

        bits = Conv.MP_num_bits(b) - 1;
        if (!t2.copy(a)) {
            ctx.tos -= 3;
            return false;
        }

        for (int j=--bits; j>=0; j--) {
            if (!Mul.MP_sqr(t1, A)) {
                ctx.tos -= 3;
                return false;
            }

            if (!Div.MP_div(null, A, t1, m, ctx)) {
                ctx.tos -= 3;
                return false;
            }

            if (Cmp.MP_is_bit_set(b, (short)j)) {
                if (!Mul.MP_mul(t1, A, t2)) {
                    ctx.tos -= 3;
                    return false;
                }

                if (!Div.MP_div(null, A, t1, m, ctx)) {
                    ctx.tos -= 3;
                    return false;
                }
            }
        }

        if (!r.copy(A)) {
            ctx.tos -= 3;
            return false;
        }

        return true;
    }
}
