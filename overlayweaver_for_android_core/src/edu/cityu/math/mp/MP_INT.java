/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp;

import edu.cityu.util.Constants;

/**
 * Data structure for representing MP Integer
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class MP_INT {

    public short [] d;
    public short top;
    public short max;
    public boolean neg;

    public MP_INT() {
        this.top = 0;
        this.neg = false;
        this.max = Constants.MP_DEFAULT_BITS/Constants.DIGIT_BITS;
        this.d = new short [max];
    }

    public MP_INT(int size) {
        this.top = 0;
        this.neg = false;
        this.max = (short) size;
        this.d = new short [max];
    }

    public MP_INT(MP_INT source) {
        this.copy(source);
    }

    public void clear() {
        for (int i=0; i<d.length; i++)
            this.d[i] = 0;
        this.top = 0;
        this.neg = false;
    }

    public boolean copy(MP_INT source) {
        this.top = source.top;
        this.max = source.max;
        this.neg = source.neg;

        this.d = new short [source.d.length];
        for (int i=0; i<this.d.length; i++)
            this.d[i] = source.d[i];
        return true;
    }

    public boolean copy_d(MP_INT source, int index) {
        if (this.max < (source.top-index))
           if (!this.MP_expand((source.top-index)*Constants.DIGIT_BITS)) return false;

        for (int i=index; i<source.top; i++) {
            this.d[i-index] = source.d[i];
        }
        
        return true;
    }

    public boolean MP_mask_bits(short n) {
        short b, w;
        w = (short) (n / Constants.DIGIT_BITS);
        b = (short) (n % Constants.DIGIT_BITS);

        if (w >= this.top)
            return false;

        if (b == 0)
            this.top = w;
        else {
            this.top = (short) (w + 1);
            this.d[w] &= ~(Constants.DIGIT_MASK<<b);
            while ((w >= 0) && (this.d[w] == 0)) {
                this.top--;
                w--;
            }
        }
        return true;
    }

    public boolean MP_expand(int bits) {
        short n = (short) ((bits - 1) / Constants.DIGIT_BITS + 1);

        if (n <= this.max)
            return true;

        short [] newD = new short [n];
        for (int i=0; i<this.d.length; i++)
            newD[i] = this.d[i];
        this.d = newD;
        this.max = n;

        return true;
    }

    public void MP_fix_top() {
        while (this.top>0 && this.d[this.top-1]==0)
            this.top--;
    }

    public boolean MP_is_zero() {
        return ((this.top<=1) && (this.d[0]==0));
    }

    public boolean MP_is_one() {
        return ((this.top<=1) && (this.d[0]==1));
    }

    public boolean MP_is_odd() {
        if ((this.d[0]&1) == 0)
            return false;
        else
            return true;
    }

    // make this a MP_INT = 0
    public void MP_zero() {
        this.top = 0;
        this.d[0] = 0;
    }

    public void MP_one() {
        this.top = 1;
        this.d[0] = 1;
    }

}
