/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp.ops;

import edu.cityu.math.mp.MP_INT;
import edu.cityu.util.Constants;

/**
 * Provides MP adding methods
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Add {

    /**
     * MP Addition
     * Note: s can be a or b
     * @param s The result of a+b is stored in s
     * @param a Operand
     * @param b Operand
     * @return true if successful
     */
    public static boolean MP_add(MP_INT s, MP_INT a, MP_INT b) {
        boolean i;
        MP_INT temp;

        // only one is negative
        if (a.neg ^ b.neg) {
            if (a.neg) {
                temp = a;
                a = b;
                b = temp;
            }
        
            // expend will not shink
            s.MP_expand(((a.top > b.top)?a.top:b.top)*Constants.DIGIT_BITS);

            if (Cmp.MP_ucmp(a, b) < 0) {
                Sub.MP_qsub(s, b, a);
                s.neg = true;
            }
            else {
                Sub.MP_qsub(s, a, b);
                s.neg = false;
            }
            
            return true;
        }

        // same sign
        if (a.neg)
            s.neg = true;
        else
            s.neg = false;

        // allocate one more digit for carry
        s.MP_expand((((a.top > b.top)?a.top:b.top)+1)*Constants.DIGIT_BITS);

        if (a.top > b.top)
            MP_qadd(s, a, b);
        else
            MP_qadd(s, b, a);

        return true;
    }

    /**
     * Quick Addition<br>
     * Note: s can be a or b
     * @param s Used to store the result
     * @param a Operand |a| >= |b|
     * @param b Operand
     */
    public static void MP_qadd(MP_INT s, MP_INT a, MP_INT b) {
        int i;
        short max, min;
        short t1, t2;
        boolean c;

        max = a.top;
        min = b.top;
        s.top = max;

        c = false;
        for (i=0; i<min; i++) {
            t1 = a.d[i];
            t2 = b.d[i];

            if (c) {
                c = (t2 >= ((~t1)&Constants.DIGIT_MASK));
                t2 = (short) ((t1 + t2 + 1) & Constants.DIGIT_MASK);
            }
            else {
//                c = (t2 > ((~t1)&Constants.DIGIT_MASK));
                t2 = (short) ((t1 + t2) & Constants.DIGIT_MASK);
                c = (t2 < t1);
            }

            s.d[i] = t2;
        }

        if (c) {
            while (i < max) {
                t1 = a.d[i];
                t2 = (short) ((t1 + 1) & Constants.DIGIT_MASK);
                s.d[i] = t2;
                c = (t2 < t1);
                i++;
                if (!c) break;
            }
            if ((i>=max) && c) {
                s.d[i]=1;
                s.top++;
            }
        }

        for (; i<max; i++) {
            s.d[i] = a.d[i];
        }
    }
}
