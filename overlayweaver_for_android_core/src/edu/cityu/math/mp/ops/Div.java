/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_CTX;
import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;

/**
 * Provides MP division methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Div {

    /**
     * (h | 1)/d
     * @param h
     * @param l
     * @param d
     * @return (h | 1) / d
     */
    public static short MP_div2(short h, short l, short d) {
        short ret;

        ret = (short) (((((int) h) << Constants.DIGIT_BITS) | l) / ((int) d));
        return ret;
    }

    /**
     * Set q and r to the quotient and remainder of (dividend / divisor)<br>
     *<br>
     * Note: - For computing modular reduction (i.e. dividend mod divisor)<br>
     *         call the function as MP_div(NULL, r, dividend, divisor, ctx).<br>
     *       - Check 14.2.5 on p.598 of HAC for details.<br>
     *<br>
     * Division: (dividend) divided by (divisor) equals (quotient) plus (remaider).<br>
     * @param q
     * @param r
     * @param dividend
     * @param divisor
     * @param ctx
     * @return true if successful
     */
    public static boolean MP_div(MP_INT q, MP_INT r, MP_INT dividend, MP_INT divisor, MP_CTX ctx) {
        MP_INT tmp, wdividend, sdividend, sdivisor, result;
        int norm_shift, dividend_n, divisor_n, loop;
        int j;
        short d0, d1;
        int wdividend_p, result_p;

        if (dividend.MP_is_zero()) {
            if (q != null) {
                q.top = 0;
                q.d[0] = 0;
            }
            return true;
        }

        if (divisor.MP_is_zero())
            return false;

        if (Cmp.MP_ucmp(dividend, divisor) < 0) {
            if (r != null)
                if (r.copy(dividend) == false)
                    return false;
            if (q != null) {
                q.top = 0;
                q.d[0] = 0;
            }
            return true;
        }

        tmp = ctx.t[ctx.tos];
        tmp.neg = false;
        sdividend = ctx.t[ctx.tos+1];
        sdivisor = ctx.t[ctx.tos+2];
        if (q == null)
            result = ctx.t[ctx.tos+3];
        else
            result = q;

        // First we normalise the numbers (check 14.23 on p.598 of HAC for details)
        norm_shift = Constants.DIGIT_BITS - (Conv.MP_num_bits(divisor) % Constants.DIGIT_BITS);
        Shift.MP_lshift(sdivisor, divisor, norm_shift);
        sdivisor.neg = false;
        norm_shift += Constants.DIGIT_BITS;
        Shift.MP_lshift(sdividend, dividend, norm_shift);
        sdividend.neg = false;
        divisor_n = sdivisor.top;
        dividend_n = sdividend.top;
        loop = dividend_n - divisor_n;

//        System.out.println(Main.output(sdivisor));
//        System.out.println(Main.output(sdividend));
//        System.out.println(divisor_n+" "+dividend_n+" "+loop);

        /*
         * setup a 'window' into sdividend
         * This is the part that corresponds to the current 'area' being divided
         */
        wdividend = new MP_INT();
        wdividend.copy_d(sdividend, loop);
        wdividend.top = (short) divisor_n;
        wdividend.max = sdividend.max;
        wdividend.neg = false;

        // get the top 2 words of sdivisor
        d0 = sdivisor.d[divisor_n - 1];
        d1 = (divisor_n == 1)?0:sdivisor.d[divisor_n-2];

        // pointer to the 'top' of snum
        wdividend_p = dividend_n-1;     // it will be used as index of sdividend.d[] directly

        // Setup to 'result'
        result.neg = (dividend.neg ^ divisor.neg);
        result.top = (short) loop;
        if (!result.MP_expand((loop+1)*Constants.DIGIT_BITS)) return false;
        result_p = loop-1;

        // space for temp
        if (!tmp.MP_expand((divisor_n+1)*Constants.DIGIT_BITS)) return false;

        if (Cmp.MP_ucmp(wdividend, sdivisor) >= 0) {
            Sub.MP_qsub(wdividend, wdividend, sdivisor);
            update_sdividend(sdividend, wdividend, loop);
            result.d[result_p] = 1;
            result.d[result.top - 1] = 1;
        }
        else
            result.top--;

        result_p--;

        for (int i=0; i<loop-1; i++) {
            short qu, n0, n1;
            short l0;

            wdividend.copy_d(sdividend, loop-i-1);
            wdividend.top++;

//            System.out.println("Start of loop: wdividend= "+Main.output(wdividend));

            n0 = sdividend.d[wdividend_p];
            n1 = sdividend.d[wdividend_p-1];
            if (n0 == d0)
                qu = Constants.DIGIT_MASK;
            else
                qu = MP_div2(n0, n1, d0);

            {
                short t1l, t1h, t2l, t2h, t3l, t3h, ql, qh, t3t;
                short [] para = new short [2];
                t1h = n0;
                t1l = n1;

                while (true) {
                    t2l = Priv.LBITS(d1);
                    t2h = Priv.HBITS(d1);
                    ql = Priv.LBITS(qu);
                    qh = Priv.HBITS(qu);

//                    System.out.println("Before mul64: qu= "+qu+" t2l= "+t2l+"; t2h= "+t2h+"; ql= "+ql+"; qh= "+qh);

                    para[0] = t2l;
                    para[1] = t2h;
                    Priv.mul64(para, ql, qh);
                    t2l = para[0];
                    t2h = para[1];

//                    System.out.println("After mul64: qu= "+qu+" t2l= "+t2l+"; t2h= "+t2h+"; ql= "+ql+"; qh= "+qh);

                    t3t = Priv.LBITS(d0);
                    t3h = Priv.HBITS(d0);
                    para[0] = t3t;
                    para[1] = t3h;
                    Priv.mul64(para, ql, qh);
                    t3t = para[0];
                    t3h = para[1];

                    t3l = (short) (t1l - t3t);
                    if (t3l > t1l)
                        t3h++;
                    t3h = (short) (t1h - t3h);

                    if (t3h != 0) break;
                    if (t2h < t3l) break;
                    if ((t2h==t3l) && (t2l<=sdividend.d[wdividend_p-2])) break;

                    qu--;
                }
            }

//            System.out.println("qu= "+qu);

            l0 = Mul.MP_mul_digit(tmp.d, sdivisor.d, divisor_n, qu);
            tmp.d[divisor_n] = l0;
            for (j=divisor_n+1; j>0; j--)
                if (tmp.d[j-1] != 0) break;
            tmp.top = (short) j;

//            System.out.println("tmp= "+Main.output(tmp));

            j = wdividend.top;
            Sub.MP_sub(wdividend, wdividend, tmp);
            update_sdividend(sdividend, wdividend, loop-i-1);

//            System.out.println("wdividend= "+Main.output(wdividend));
//            System.out.println("sdividend= "+Main.output(sdividend));

            sdividend.top = (short) (sdividend.top + wdividend.top - j);

            if (wdividend.neg) {
                qu--;
                j = wdividend.top;
                Add.MP_add(wdividend, wdividend, sdivisor);
                update_sdividend(sdividend, wdividend, loop-i-1);
                sdividend.top += wdividend.top - j;
            }

            result.d[result_p--] = qu;
            wdividend_p--;
//            System.out.println(Main.output(result));
        }

        if (r != null) {
            Shift.MP_rshift(r, sdividend, norm_shift);
            r.neg = dividend.neg;
        }

        return true;
    }

    private static void update_sdividend(MP_INT sdividend, MP_INT wdividend, int sstart) {
        int i;
        sdividend.MP_expand((sstart+wdividend.top)*Constants.DIGIT_BITS);

        for (i=sstart; i<sdividend.top; i++) {
            if ((i-sstart)>=wdividend.top) {
                if (i < sdividend.top) {
                    for (; i<sdividend.top; i++)
                    sdividend.d[i] = 0;
                }
                break;
            }
                
            sdividend.d[i] = wdividend.d[i-sstart];
        }

        if ((i-sstart) < wdividend.top) {
            for (; (i-sstart)<wdividend.top; i++)
                sdividend.d[i] = wdividend.d[i-sstart];
        }

    }

}
