/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.bp;

import edu.cityu.hash.SHA256;
import edu.cityu.math.mp.MP_CTX;
import edu.cityu.math.mp.MP_INT;
import edu.cityu.math.mp.ops.Conv;
import edu.cityu.math.mp.ops.Div;
//import edu.cityu.test.Main;
import edu.cityu.util.BigInt;
import edu.cityu.util.Constants;
import edu.cityu.util.CpElement;
import edu.cityu.util.Element;
import edu.cityu.util.Point;
import edu.cityu.util.Point_proj;

/**
 * Arithmetic on Point operations
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class PointArith {

    /**
     * The order of elliptic curve group we use is #E/7
     * HEX 7A46E0901F72546F8D3EBA717E08644135DE41
     * DEC 2726865189058261010774960798134976187171462721
     */

    /**
     * Compare two points
     * Return true if equal
     */
    public static boolean point_cmp(Point P, Point Q) {
        return P.equal(Q);
    }

    /**
     * Compute opposite of point P
     * Q = (-P)
     */
    public static void point_neg(Point P, Point Q) {
        BaseField.base_element_neg(P.y, Q.y);
        Q.x.copy(P.x);
    }

    /**
     * Compute the y coordinate, given the x coordinate
     * y^2 = x^3 - x + 1
     */
    public static boolean point_gety(Element x, Element y) {
        Element y2 = new Element();
        Element temp = new Element();

        // y^2
        BaseField.base_element_cube(x, y2);
        BaseField.base_element_sub(y2, x, y2);
        BaseField.base_element_add(y2, Constants.ELEMENT_ONE, y2);
        BaseField.base_element_mult(y2, y2, temp);
//        System.out.println(Main.output_Element(temp));
        BaseField.base_element_inver(temp, y);

        temp.copy(y2);
        for (int i=0; i<(Constants.CONSTANT_M-1)/2; i++) {
            BaseField.base_element_cube(temp, temp);
            BaseField.base_element_cube(temp, temp);
            BaseField.base_element_mult(temp, y, temp);
        }

        y.copy(temp);
        BaseField.base_element_mult(temp, temp, temp);
        if (!temp.equal(y2)) {
            y.copy(Constants.ZERO_POINT.x);
            return false;
        }
        return true;
    }

    /**
     * Compress num points to their compressed form
     * P[i] is compressed to CP[i]
     */
    public static void point_cps(Point [] P, CpElement [] CP, int num) {
        Element y = new Element();
        int flag;

        for (int i=0; i<num; i++) {
            point_gety(P[i].x, y);
//            System.out.println("cps y="+Main.output_Element(y));
            if (P[i].y.equal(y))
                flag = 0;
            else
                flag = 0x80;
            BaseField.base_elmt2cpelmt(P[i].x, CP[i]);
            CP[i].value[Constants.CPELEMENT_LEN-1] |= flag;
        }
    }

    /**
     * Decompress num compress points to their original form
     * CP[i] is decompressed to P[i]
     */
    public static void point_dcps(CpElement [] CP, Point [] P, int num) {
        int flag;
        CpElement cptemp = new CpElement();
        for (int i=0; i<num; i++) {
            cptemp.copy(CP[i]);
            flag = 0x80 & cptemp.value[Constants.CPELEMENT_LEN-1];
            cptemp.value[Constants.CPELEMENT_LEN-1] &= 0x7F;
            BaseField.base_cpelmt2elmt(cptemp, P[i].x);
            point_gety(P[i].x, P[i].y);
            if (flag != 0)
                BaseField.base_element_neg(P[i].y, P[i].y);
        }
    }

    /**
     * Add two points P and Q
     * R = P + Q
     */
    public static void point_add(Point P, Point Q, Point R) {
        Element slope = new Element();
        Element temp = new Element();
        Point r = new Point();

        // either point P or Q is zero
        if (point_cmp(P, Constants.ZERO_POINT)) {
            R.copy(Q);
            return;
        }
        if (point_cmp(Q, Constants.ZERO_POINT)) {
            R.copy(P);
            return;
        }

        // P = Q
        if (point_cmp(P, Q)) {
            point_doub(P, R);
            return;
        }

        // P is opposite of Q
        point_neg(Q, r);
        if (point_cmp(P, r)) {
            R.copy(Constants.ZERO_POINT);
            return;
        }

        // addition
        BaseField.base_element_sub(Q.y, P.y, slope);
        BaseField.base_element_sub(Q.x, P.x, temp);
        BaseField.base_element_inver(temp, temp);
        BaseField.base_element_mult(slope, temp, slope);

        BaseField.base_element_mult(slope, slope, temp);
        BaseField.base_element_cube(slope, slope);

        BaseField.base_element_add(P.x, Q.x, r.x);
        BaseField.base_element_sub(temp, r.x, R.x);

        BaseField.base_element_add(P.y, Q.y, r.y);
        BaseField.base_element_sub(r.y, slope, R.y);
    }

    /**
     * Compute double of point P
     */
    public static void point_doub(Point P, Point Q) {
        Element slope = new Element();
        Element temp = new Element();

        // if P is zero
        if (point_cmp(P, Constants.ZERO_POINT)) {
            Q.copy(Constants.ZERO_POINT);
            return;
        }

        BaseField.base_element_inver(P.y, slope);
        BaseField.base_element_mult(slope, slope, temp);
        BaseField.base_element_add(temp, P.x, Q.x);
        BaseField.base_element_cube(slope, slope);
        BaseField.base_element_add(slope, P.y, temp);
        BaseField.base_element_neg(temp, Q.y);
    }

    /**
     * Compute trible of P
     */
    public static void point_trip(Point P, Point Q) {
        Element temp = new Element();

        // if P is zero
        if (point_cmp(P, Constants.ZERO_POINT)) {
            Q.copy(Constants.ZERO_POINT);
            return;
        }

        BaseField.base_element_cube(P.x, temp);
        BaseField.base_element_cube(temp, temp);
        BaseField.base_element_sub(temp, Constants.ELEMENT_ONE, Q.x);
        BaseField.base_element_cube(P.y, temp);
        BaseField.base_element_cube(temp, temp);
        BaseField.base_element_neg(temp, Q.y);
    }

    /**
     * Add two points using projective coordinate
     * The projective coordinates of second input point are (x,y,1), so here we use its affine coordinate
     */
    public static void point_add_proj(Point_proj P, Point Q, Point_proj R) {
        Element A = new Element();
        Element B = new Element();
        Element C = new Element();
        Element D = new Element();

        // either P or Q is zero
        if (point_cmp((new Point(P.x, P.y)), Constants.ZERO_POINT)) {
            R.x.copy(Q.x);
            R.y.copy(Q.y);
            R.z.copy(Constants.ELEMENT_ONE);
            return;
        }

        if (point_cmp(Q, Constants.ZERO_POINT)) {
            R.copy(P);
            return;
        }

        /**
         * point P is the opposite of Q
         * first check whether q.x*p.z ?= p.x
         */
        BaseField.base_element_mult(Q.x, P.z, A);
        if (P.x.equal(A)) {
            BaseField.base_element_mult(Q.y, P.z, B);
            BaseField.base_element_neg(B, C);
            if (P.y.equal(C)) {
                R.x.copy(Constants.ZERO_POINT.x);
                R.y.copy(Constants.ZERO_POINT.y);
                R.z.copy(Constants.ELEMENT_ONE);
                return;
            }
        }

        BaseField.base_element_sub(A, P.x, A);
        BaseField.base_element_mult(Q.y, P.z, B);
        BaseField.base_element_sub(B, P.y, B);
        BaseField.base_element_cube(A, C);
        BaseField.base_element_mult(B, B, D);
        BaseField.base_element_mult(D, P.z, D);
        BaseField.base_element_sub(C, D, D);

        // R.x
        BaseField.base_element_mult(P.x, C, R.x);
        BaseField.base_element_mult(A, D, A);
        BaseField.base_element_sub(R.x, A, R.x);

        // R.y
        BaseField.base_element_mult(B, D, A);
        BaseField.base_element_mult(P.y, C, R.y);
        BaseField.base_element_sub(A, R.y, R.y);

        // R.z
        BaseField.base_element_mult(P.z, C, R.z);
    }

    /**
     * Tripling a point in projective coordinates
     * x = x^9 - z^9
     * y = -y^9
     * z = z^9
     */
    public static void point_trip_proj(Point_proj P, Point_proj Q) {
        if ((new Point(P.x, P.y)).equal(Constants.ZERO_POINT)) {
            Q.x.copy(Constants.ZERO_POINT.x);
            Q.y.copy(Constants.ZERO_POINT.y);
            Q.z.copy(Constants.ELEMENT_ONE);
            return;
        }

        BaseField.base_element_cube(P.z, Q.z);
        BaseField.base_element_cube(Q.z, Q.z);

        BaseField.base_element_cube(P.x, Q.x);
        BaseField.base_element_cube(Q.x, Q.x);
        BaseField.base_element_sub(Q.x, Q.z, Q.x);

        BaseField.base_element_cube(P.y, Q.y);
        BaseField.base_element_cube(Q.y, Q.y);
        BaseField.base_element_neg(Q.y, Q.y);
    }

    /**
     * Point scale multiplication k*P, the scale multiplier k is a BigInt type,
     * as the subgroup order is #E/7, which can be represented by a BigInt
     */
    public static boolean point_mult_affine(BigInt bigInt, Point P, Point Q) {
        int i;
        BigInt mod_k = new BigInt();
        MP_INT mp_k = new MP_INT();
        MP_INT three = Conv.MP_string2bn("3");
        MP_INT q = new MP_INT();
        MP_INT r = new MP_INT();
        MP_CTX ctx = new MP_CTX();

        int [] k3 = new int [Constants.CONSTANT_M];
        int k3_len;
        Point dP = new Point();
        Point result = new Point();

        // if P is zero
        if (point_cmp(P, Constants.ZERO_POINT)) {
            Q.copy(Constants.ZERO_POINT);
            return true;
        }

        // if multiplier is zero
        BigInt.bigint_mod(bigInt, mod_k);
        i = 0;
        while ((i<Constants.BIGINT_LEN) && (mod_k.value[i]==0)) {
            i++;
            if (i==Constants.BIGINT_LEN) {
                Q.copy(Constants.ZERO_POINT);
                return true;
            }
        }

        k3_len = 0;
        mp_k = Conv.MP_bint2bn(mod_k, Constants.BIGINT_LEN);
        while (!(mp_k.MP_is_zero() && k3_len<Constants.CONSTANT_M)) {
            q.clear();
            r.clear();
            if (!Div.MP_div(q, r, mp_k, three, ctx))
                return false;

            mp_k.clear();
            mp_k.copy(q);
            k3[k3_len] = Conv.MP_num_bits(r);
            k3_len++;
        }

        result.copy(Constants.ZERO_POINT);
        point_doub(P, dP);
        for (i=k3_len-1; i>=0; i--) {
            switch (k3[i]) {
                case 1:
                    point_add(P, result, result);
                    break;
                case 2:
                    point_add(dP, result, result);
                    break;
                default:
                    break;
            }
            if (i>0)
                point_trip(result, result);
        }
        Q.copy(result);
        return true;
    }

    /**
     * Conduct scale multiplication of points, in which multiplication is computed in coordinate forms
     */
    public static boolean point_mult(BigInt bigInt, Point P, Point Q) {
        BigInt mod_k = new BigInt();
        MP_INT mp_k = new MP_INT();
        MP_INT nine = Conv.MP_string2bn("9");
        MP_INT q = new MP_INT();
        MP_INT r = new MP_INT();
        MP_CTX ctx = new MP_CTX();

        int [] k_NAF = new int [(Constants.CONSTANT_M+1)/2];
        int k9_len;

        // point precomputation
        Point [] p_array = new Point [8];
        for (int i=0; i<8; i++)
            p_array[i] = new Point();
        Point_proj result = new Point_proj();
        int carry;
        int i;

        // if P is zero
        if (point_cmp(P, Constants.ZERO_POINT)) {
            Q.copy(Constants.ZERO_POINT);
            return true;
        }

        // if multiplier is zero, return zero
        BigInt.bigint_mod(bigInt, mod_k);
        i = 0;
        while ((i<Constants.BIGINT_LEN) && (mod_k.value[i]==0)) {
            i++;
            if (i == Constants.BIGINT_LEN) {
                Q.copy(Constants.ZERO_POINT);
                return true;
            }
        }

        // change the base of multiplier to radix 9 NAF
        k9_len = 0;
        carry = 0;
        mp_k = Conv.MP_bint2bn(mod_k, Constants.BIGINT_LEN);
//        System.out.println("mod_k="+Main.output_BigInt(mod_k));
        while (!(mp_k.MP_is_zero()) && (k9_len<((Constants.CONSTANT_M+1)/2))) {
            q.clear();
            r.clear();
            if (!Div.MP_div(q, r, mp_k, nine, ctx))
                return false;
            mp_k.clear();
            mp_k.copy(q);

//            System.out.println(k9_len);
            k_NAF[k9_len] = carry;
            if (Conv.MP_num_bits(r) > 0) {
                k_NAF[k9_len] += (r.d[0]&0x00FF);
            }

            if (k_NAF[k9_len] > 4)
                carry = 1;
            else
                carry = 0;
            if (k_NAF[k9_len] == 9)
                k_NAF[k9_len] = 0;
            k9_len++;
        }

        if (carry == 1)
            k_NAF[k9_len++] = 1;

        p_array[0].copy(P);
        point_doub(p_array[0], p_array[1]);
        point_trip(p_array[0], p_array[2]);
        point_doub(p_array[1], p_array[3]);
        for (i=0; i<4; i++)
            point_neg(p_array[3-1], p_array[4+i]);

        result.x.copy(Constants.ZERO_POINT.x);
        result.y.copy(Constants.ZERO_POINT.y);
        result.z.copy(Constants.ELEMENT_ONE);

        for (i=k9_len-1; i>=0; i--) {
            if(k_NAF[i] != 0) {
                point_add_proj(result, p_array[k_NAF[i]-1], result);
            }
            if (i>0) {
                point_trip_proj(result, result);
                point_trip_proj(result, result);
            }
        }

        BaseField.base_element_inver(result.z, result.z);
        BaseField.base_element_mult(result.x, result.z, Q.x);
        BaseField.base_element_mult(result.y, result.z, Q.y);

        return true;
    }

    /* Maps a string to a point on ECC*/
    public static Point map2Point(String msg) {
        Element x = new Element();
        Element y = new Element();
        Point result = new Point();

        String hashmessage = "";
        int k = 0;
        byte [] digest;
        while (true) {
//            System.out.println(k);
            hashmessage = k + msg;

            /* hash msg to D */
            digest = SHA256.SHA256(hashmessage);

            /* derive a abscissa x from D */
            int target_bit = 0;
            int source_bit = 0;
            while (target_bit < 96) {
                int source = 0;

                do {
                    source = getRawBits(source_bit, digest);
                    source_bit++;

                    if (source_bit >= 128) {
                        k++;
                        hashmessage = k + msg;
                        digest = SHA256.SHA256(hashmessage);
                        source_bit = 0;
                    }
                } while (source == 3);

                assignTargetBits(target_bit, source, x);
                target_bit++;
            }

            if ((digest[24] & 0x1) != 0) {
                x.lo[12] = 0x1;
                x.hi[12] = 0x0;
            }
            else if ((digest[25] & 0x1) != 0) {
                x.lo[12] = 0x0;
                x.hi[12] = 0x1;
            }
            else {
                x.lo[12] = 0x0;
                x.hi[12] = 0x0;
            }

//            System.out.println("x is \n"+Main.output_Element(x));

            /* try get y, repeat if fail */
            if (point_gety(x, y))
                break;      // success
            else
                k++;        // fail
        }
        result.x.copy(x);
        result.y.copy(y);

        return result;
    }

    /**
     * get raw 2-bits from a SHE256 digest
     * i.e. getRawBits(0, digest) will return [digest[0]>>7&0x1][digest[16]>>7&0x1]
     * @return 0, 1, 2, or 3
     */
    private static int getRawBits(int index, byte[] digest) {
//        System.out.println("source_bit="+index);
        int byte_index = index / 8;
        int bit_index = index % 8;

        int high = (digest[byte_index] >> (7-bit_index)) & 0x1;
        int low = (digest[byte_index+16] >> (7-bit_index)) & 0x1;

        return high*2+low;
    }

    /**
     * assign bits to an Element
     * i.e. assignTargetBits(0, 2, x) will set x.hi[bit0] = 1, x.lo[bit0] = 1
     *
     */
    private static void assignTargetBits(int index, int value, Element x) {
        int byte_index = index / 8;
        int bit_index = index % 8;
        byte byte_mask = (byte)0xFF;
        short one = 1;

        short high = 0;
        short low = 0;
        short mask = 0;

        if (value == 2) {
            high = 1;
            low = 0;
            mask = (short) (byte_mask ^ one << (7 - bit_index));

            x.hi[byte_index] |= (high << (7-bit_index));
            x.lo[byte_index] &= mask;
        }
        else if (value == 1) {
            high = 0;
            low = 1;
            mask = (short) (byte_mask ^ one << (7 - bit_index));

            x.hi[byte_index] &= mask;
            x.lo[byte_index] |= (low << (7-bit_index));
        }
        else {
            mask = (short) (byte_mask ^ one << (7 - bit_index));
            x.hi[byte_index] &= mask;
            x.lo[byte_index] &= mask;
        }
    }
}
