/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.bp;

//import edu.cityu.test.Main;
import edu.cityu.util.Constants;
import edu.cityu.util.Element;
import edu.cityu.util.ExtElement;
import edu.cityu.util.Point;

/**
 * Contains methods for EtaT Pairing
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class EtaT_Pairing {

    public static void lambda(ExtElement a, ExtElement b) {
        Element z0 = new Element();
        Element z1 = new Element();
        Element z2 = new Element();
        Element z3 = new Element();
        Element z4 = new Element();
        Element z5 = new Element();
        Element z6 = new Element();
        Element z7 = new Element();
        Element z8 = new Element();
        Element temp = new Element();

        BaseField.base_element_mult(a.ext[0].mid[0], a.ext[0].mid[2], z0);
        BaseField.base_element_mult(a.ext[1].mid[0], a.ext[1].mid[2], z1);
        BaseField.base_element_mult(a.ext[0].mid[1], a.ext[0].mid[2], z2);
        BaseField.base_element_mult(a.ext[1].mid[1], a.ext[1].mid[2], z3);

        BaseField.base_element_add(a.ext[0].mid[0], a.ext[1].mid[0], z4);
        BaseField.base_element_sub(a.ext[0].mid[2], a.ext[1].mid[2], temp);
        BaseField.base_element_mult(z4, temp, z4);

        BaseField.base_element_mult(a.ext[1].mid[0], a.ext[0].mid[1], z5);
        BaseField.base_element_mult(a.ext[0].mid[0], a.ext[1].mid[1], z6);

        BaseField.base_element_add(a.ext[0].mid[0], a.ext[1].mid[0], z7);
        BaseField.base_element_add(a.ext[0].mid[1], a.ext[1].mid[1], temp);
        BaseField.base_element_mult(z7, temp, z7);

        BaseField.base_element_add(a.ext[0].mid[1], a.ext[1].mid[1], z8);
        BaseField.base_element_sub(a.ext[0].mid[2], a.ext[1].mid[2], temp);
        BaseField.base_element_mult(z8, temp, z8);

        //B
        BaseField.base_element_add(Constants.ELEMENT_ONE, z0, b.ext[0].mid[0]);
        BaseField.base_element_add(b.ext[0].mid[0], z1, b.ext[0].mid[0]);
        BaseField.base_element_sub(b.ext[0].mid[0], z2, b.ext[0].mid[0]);
        BaseField.base_element_sub(b.ext[0].mid[0], z3, b.ext[0].mid[0]);

        BaseField.base_element_add(z1, z4, b.ext[1].mid[0]);
        BaseField.base_element_add(b.ext[1].mid[0], z5, b.ext[1].mid[0]);
        BaseField.base_element_sub(b.ext[1].mid[0], z0, b.ext[1].mid[0]);
        BaseField.base_element_sub(b.ext[1].mid[0], z6, b.ext[1].mid[0]);

        BaseField.base_element_sub(z7, z2, b.ext[0].mid[1]);
        BaseField.base_element_sub(b.ext[0].mid[1], z3, b.ext[0].mid[1]);
        BaseField.base_element_sub(b.ext[0].mid[1], z5, b.ext[0].mid[1]);
        BaseField.base_element_sub(b.ext[0].mid[1], z6, b.ext[0].mid[1]);

        BaseField.base_element_add(z0, z3, b.ext[1].mid[1]);
        BaseField.base_element_add(b.ext[1].mid[1], z8, b.ext[1].mid[1]);
        BaseField.base_element_sub(b.ext[1].mid[1], z2, b.ext[1].mid[1]);
        BaseField.base_element_sub(b.ext[1].mid[1], z1, b.ext[1].mid[1]);
        BaseField.base_element_sub(b.ext[1].mid[1], z4, b.ext[1].mid[1]);

        BaseField.base_element_add(z2, z3, b.ext[0].mid[2]);
        BaseField.base_element_add(b.ext[0].mid[2], z7, b.ext[0].mid[2]);
        BaseField.base_element_sub(b.ext[0].mid[2], z5, b.ext[0].mid[2]);
        BaseField.base_element_sub(b.ext[0].mid[2], z6, b.ext[0].mid[2]);

        BaseField.base_element_add(z3, z8, b.ext[1].mid[2]);
        BaseField.base_element_sub(b.ext[1].mid[2], z2, b.ext[1].mid[2]);
    }

    public static void root_3m(ExtElement a, ExtElement r) {
        BaseField.base_element_sub(a.ext[0].mid[0], a.ext[0].mid[1], r.ext[0].mid[0]);
	BaseField.base_element_add(r.ext[0].mid[0], a.ext[0].mid[2], r.ext[0].mid[0]);
	BaseField.base_element_sub(a.ext[1].mid[1], a.ext[1].mid[0], r.ext[1].mid[0]);
	BaseField.base_element_sub(r.ext[1].mid[0], a.ext[1].mid[2], r.ext[1].mid[0]);
	BaseField.base_element_add(a.ext[0].mid[1], a.ext[0].mid[2], r.ext[0].mid[1]);
	BaseField.base_element_add(a.ext[1].mid[1], a.ext[1].mid[2], r.ext[1].mid[1]);
	BaseField.base_element_neg(r.ext[1].mid[1], r.ext[1].mid[1]);

        r.ext[0].mid[2].set_hi(a.ext[0].mid[2].hi);
        r.ext[0].mid[2].set_lo(a.ext[0].mid[2].lo);
        BaseField.base_element_neg(a.ext[1].mid[2], r.ext[1].mid[2]);
    }

    public static void etaT_pairing(Point P, Point Q, ExtElement E) {
        ExtElement R1 = new ExtElement();
        Element r0 = new Element();
        Element b = Element.get_ELEMENT_ONE();
        Element d = Element.get_ELEMENT_ONE();
        Point p = new Point();
        Point q = new Point();
        int i;

        PointArith.point_trip(P, p);
        for (i=1; i<(Constants.CONSTANT_M-1)/2; i++) {
            PointArith.point_trip(p, p);
        }

        q.copy(Q);
        BaseField.base_element_neg(p.y, p.y);
        BaseField.base_element_add(p.x, q.x, r0);
        BaseField.base_element_add(r0, b, r0);

        // E = R0
        BaseField.base_element_mult(p.y, r0, r0);
        BaseField.base_element_neg(r0, E.ext[0].mid[0]);

        E.ext[1].mid[0].copy(q.y);
        E.ext[0].mid[1].copy(p.y);

        E.ext[0].mid[2].set_zero();
        E.ext[1].mid[1].set_zero();
        E.ext[1].mid[2].set_zero();

        for (i=0; i<=(Constants.CONSTANT_M-1)/2; i++) {
//            System.out.println("i="+i);
            BaseField.base_element_add(p.x, q.x, r0);
            BaseField.base_element_add(r0, d, r0);

//            System.out.println("b="+Main.output_Element(b));

            //R1
            BaseField.base_element_neg(r0, R1.ext[0].mid[1]);
            BaseField.base_element_mult(r0, r0, r0);
            BaseField.base_element_neg(r0, R1.ext[0].mid[0]);
            BaseField.base_element_mult(p.y, q.y, R1.ext[1].mid[0]);
            R1.ext[1].mid[1].set_zero();
            BaseField.base_element_neg(b, R1.ext[0].mid[2]);
            R1.ext[1].mid[2].set_zero();

//            System.out.println("R1="+Main.output_ExtElement(R1));
//            System.out.println("b after neg="+Main.output_Element(b));

            //E = R0 = (R0R1)^3
            ExtField.ext_element_mult(R1, E, E);
            ExtField.ext_element_cube(E, E);

            BaseField.base_element_neg(p.y, p.y);
            BaseField.base_element_cube(q.x, q.x);
            BaseField.base_element_cube(q.x, q.x);
            BaseField.base_element_cube(q.y, q.y);
            BaseField.base_element_cube(q.y, q.y);
            BaseField.base_element_sub(d, b, d);
        }

//        System.out.println(Main.output_ExtElement(E));

        root_3m(E, E);

//        System.out.println(Main.output_ExtElement(E));


        //final exp
        //use R1.ext[1] and R1.ext[0] to store temp value
        ExtField.mid_element_mult(E.ext[0], E.ext[1], R1.ext[0]);
        ExtField.mid_element_mult(E.ext[0], E.ext[0], E.ext[0]);
        ExtField.mid_element_mult(E.ext[1], E.ext[1], E.ext[1]);
        ExtField.mid_element_add(E.ext[0], E.ext[1], R1.ext[1]);
        ExtField.mid_element_inver(R1.ext[1], R1.ext[1]);
        ExtField.mid_element_sub(E.ext[0], E.ext[1], E.ext[0]);
        ExtField.mid_element_mult(E.ext[0], R1.ext[1], E.ext[0]);
        ExtField.mid_element_add(R1.ext[0], R1.ext[0], R1.ext[0]);
        ExtField.mid_element_neg(R1.ext[0], R1.ext[0]);
        ExtField.mid_element_mult(R1.ext[0], R1.ext[1], E.ext[1]);

        lambda(E, E);
        lambda(E, R1);

//        System.out.println(Main.output_ExtElement(E));
        
        for (i=0; i<=(Constants.CONSTANT_M-1)/2; i++) {
            ExtField.ext_element_cube(E, E);
        }

        ExtField.ext_element_inver(E, E);
        ExtField.ext_element_mult(E, R1, E);
    }
}
