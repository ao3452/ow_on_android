/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.bp;

//import edu.cityu.test.Main;
import edu.cityu.util.Element;
import edu.cityu.util.ExtElement;
import edu.cityu.util.MidElement;

/**
 * Methods for arithmetic on Extention Field
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class ExtField {

    /**
     * intermediate field operations GF(3^m^3)
     */
    public static void mid_element_add(MidElement a, MidElement b, MidElement c) {
        BaseField.base_element_add(a.mid[0], b.mid[0], c.mid[0]);
        BaseField.base_element_add(a.mid[1], b.mid[1], c.mid[1]);
        BaseField.base_element_add(a.mid[2], b.mid[2], c.mid[2]);
    }

    public static void mid_element_sub(MidElement a, MidElement b, MidElement c) {
        BaseField.base_element_sub(a.mid[0], b.mid[0], c.mid[0]);
        BaseField.base_element_sub(a.mid[1], b.mid[1], c.mid[1]);
        BaseField.base_element_sub(a.mid[2], b.mid[2], c.mid[2]);
    }

    public static void mid_element_mult(MidElement a, MidElement b, MidElement c) {
        Element d1 = new Element();
        Element d2 = new Element();
        Element d3 = new Element();
        Element temp = new Element();

        //pre-calculate d1
        BaseField.base_element_add(a.mid[1], a.mid[0], temp);
        BaseField.base_element_add(b.mid[1], b.mid[0], d1);
        BaseField.base_element_mult(temp, d1, d1);

        //pre-calculate d2
        BaseField.base_element_add(a.mid[2], a.mid[0], temp);
        BaseField.base_element_add(b.mid[2], b.mid[0], d2);
        BaseField.base_element_mult(temp, d2, d2);

        //pre-calculate d3
        BaseField.base_element_add(a.mid[2], a.mid[1], temp);
        BaseField.base_element_add(b.mid[2], b.mid[1], d3);
        BaseField.base_element_mult(temp, d3, d3);

        BaseField.base_element_mult(a.mid[0], b.mid[0], c.mid[0]);
        BaseField.base_element_mult(a.mid[1], b.mid[1], c.mid[1]);
        BaseField.base_element_mult(a.mid[2], b.mid[2], c.mid[2]);

        //compute d1, d2, d3
        BaseField.base_element_sub(d1, c.mid[1], d1);
        BaseField.base_element_sub(d1, c.mid[0], d1);

        BaseField.base_element_add(d2, c.mid[1], d2);
        BaseField.base_element_sub(d2, c.mid[2], d2);
        BaseField.base_element_sub(d2, c.mid[0], d2);

        BaseField.base_element_sub(d3, c.mid[2], d3);
        BaseField.base_element_sub(d3, c.mid[1], d3);

        //compute C
        BaseField.base_element_add(d1, d3, c.mid[1]);
        BaseField.base_element_add(c.mid[1], c.mid[2], c.mid[1]);
        BaseField.base_element_add(c.mid[0], d3, c.mid[0]);
        BaseField.base_element_add(c.mid[2], d2, c.mid[2]);
    }

    public static void mid_element_cube(MidElement a, MidElement b) {
        BaseField.base_element_cube(a.mid[0], b.mid[0]);
        BaseField.base_element_cube(a.mid[1], b.mid[1]);
        BaseField.base_element_cube(a.mid[2], b.mid[2]);

        BaseField.base_element_add(b.mid[0], b.mid[1], b.mid[0]);
        BaseField.base_element_add(b.mid[0], b.mid[2], b.mid[0]);
        BaseField.base_element_sub(b.mid[1], b.mid[2], b.mid[1]);
    }

    /**
     * A(u)^-1
     */
    public static void mid_element_inver(MidElement a, MidElement b) {
        Element delta = new Element();
        Element temp = new Element();
        Element a02 = new Element();
        Element a12 = new Element();
        Element a22 = new Element();

        //compute a0^2, a1^2, a2^3
        BaseField.base_element_mult(a.mid[0], a.mid[0], a02);
        BaseField.base_element_mult(a.mid[1], a.mid[1], a12);
        BaseField.base_element_mult(a.mid[2], a.mid[2], a22);

        //compute delta
        BaseField.base_element_sub(a.mid[0], a.mid[2], temp);
        BaseField.base_element_mult(temp, a02, delta);

        BaseField.base_element_sub(a.mid[1], a.mid[0], temp);
        BaseField.base_element_mult(temp, a12, temp);
        BaseField.base_element_add(delta, temp, delta);

        BaseField.base_element_sub(a.mid[0], a.mid[1], temp);
        BaseField.base_element_add(temp, a.mid[2], temp);
        BaseField.base_element_mult(temp, a22, temp);
        BaseField.base_element_add(delta, temp, delta);

//        System.out.println(Main.output_Element(delta));
        BaseField.base_element_inver(delta, delta);
//        System.out.println(Main.output_Element(delta));

        //compute C
        BaseField.base_element_add(a02, a22, a02);
        BaseField.base_element_sub(a02, a12, a02);
        BaseField.base_element_mult(a.mid[1], a.mid[2], temp);
        BaseField.base_element_sub(a02, temp, a02);
        BaseField.base_element_mult(a.mid[0], a.mid[2], temp);
        BaseField.base_element_sub(a02, temp, a02);

        //for c2
        BaseField.base_element_sub(a12, temp, a12);
        BaseField.base_element_mult(a02, delta, a02);

        BaseField.base_element_sub(a12, a22, a12);
        BaseField.base_element_mult(a12, delta, a12);

        BaseField.base_element_mult(a.mid[0], a.mid[1], temp);
        BaseField.base_element_sub(a22, temp, a22);
        BaseField.base_element_mult(a22, delta, a22);

        b.mid[0].copy(a02);
        b.mid[1].copy(a22);
        b.mid[2].copy(a12);
    }

    public static void mid_element_neg(MidElement a, MidElement b) {
        BaseField.base_element_neg(a.mid[0], b.mid[0]);
        BaseField.base_element_neg(a.mid[1], b.mid[1]);
        BaseField.base_element_neg(a.mid[2], b.mid[2]);
    }

    /**
     * A multiply B in GF(3^m^6)
     * C = A*B mod F(x)
     */
    public static void ext_element_mult(ExtElement A, ExtElement B, ExtElement C) {
        MidElement a0b0 = new MidElement();
        MidElement a1b1 = new MidElement();
        MidElement a1a0b1b0 = new MidElement();

        mid_element_add(A.ext[0], A.ext[1], a0b0);
        mid_element_add(B.ext[0], B.ext[1], a1b1);
        mid_element_mult(a0b0, a1b1, a1a0b1b0);
        mid_element_mult(A.ext[0], B.ext[0], a0b0);
        mid_element_mult(A.ext[1], B.ext[1], a1b1);

        mid_element_sub(a0b0, a1b1, C.ext[0]);
        mid_element_sub(a1a0b1b0, a1b1, a1a0b1b0);
        mid_element_sub(a1a0b1b0, a0b0, C.ext[1]);
    }

    /**
     * Compute cube of A in GF(3^m^6)
     * B = A^3
     */
    public static void ext_element_cube(ExtElement A, ExtElement B) {
        mid_element_cube(A.ext[0], B.ext[0]);
        mid_element_cube(A.ext[1], B.ext[1]);

        mid_element_neg(B.ext[1], B.ext[1]);
    }

    /**
     * Compute inverse of A in GF(3^m^6)
     * B = A^(-1)
     */
    public static void ext_element_inver(ExtElement A, ExtElement B) {
        MidElement temp0 = new MidElement();
        MidElement temp1 = new MidElement();

        mid_element_mult(A.ext[0], A.ext[0], temp0);
        mid_element_mult(A.ext[1], A.ext[1], temp1);
        mid_element_add(temp0, temp1, temp0);
        mid_element_inver(temp0, temp0);
        mid_element_mult(temp0, A.ext[0], B.ext[0]);
        mid_element_neg(A.ext[1], temp1);
        mid_element_mult(temp0, temp1, B.ext[1]);
    }
}
