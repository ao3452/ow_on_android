/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.be;

import edu.cityu.bp.BaseField;
import edu.cityu.bp.EtaT_Pairing;
import edu.cityu.bp.ExtField;
import edu.cityu.bp.PointArith;
import edu.cityu.util.BigInt;
import edu.cityu.util.Constants;
import edu.cityu.util.ExtElement;
import edu.cityu.util.Point;
import edu.cityu.util.Point_proj;

/**
 * Class for Broadcast Encryption
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class BE {

    /**
     * Setup API
     * @param A Integer
     * @param rarray Array of random BigInt
     * @param ra Random BigInt
     * @param rw Random BigInt
     * @param B Integer
     * @param dn Output: Private Key array of the length A*B in Point type, decompressed
     * @param PK Output: Public Key array of the length 2B+A in Point type, decompressed
     */
    public static void BE_setup(int A, int B, BigInt ra, BigInt rw, BigInt [] rarray, Point [] PK, Point [] dn) {
        int N;
        int a, b;
        int i;
        Point g0 = new Point();
        g0.copy(Constants.g0);

//        System.out.println("g0="+Main.output_Point(g0));

        //PK
        PointArith.point_mult(rw, g0, PK[0]);

        //g1, ..., gB
        for (i=1; i<=B; i++)
            PointArith.point_mult(ra, PK[i-1], PK[i]);

        if (B > 1) {
            //g(B+2), ..., g(2B)
            PointArith.point_mult(ra, PK[B], PK[B+1]);
            PointArith.point_mult(ra, PK[B+1], PK[B+1]);
        }
        for (i=(B+2); i<2*B; i++)
            PointArith.point_mult(ra, PK[i-1], PK[i]);

        //v1, ..., vA
        for (i=0; i<A; i++)
            PointArith.point_mult(rarray[i], PK[0], PK[i+2*B]);

        //Private keys di
        N = A*B;
        for (i=1; i<=N; i++) {
            a = (i-1)/B + 1;
            b = i % B;
            if (b == 0)
                b = B;
            PointArith.point_mult(rarray[a-1], PK[b], dn[i-1]);
        }
    }

    /**
     * encryption API
     *
     * @param t Random BigInt
     * @param PK Public Key array in decompressed form
     * @param S User subset
     * @param A Length of S
     * @param EK Encryption Key (session key)
     * @param Hdr Broadcast header
     * @param B Size of PK
     */
    public static void BE_encrypt(BigInt t, Point [] PK, byte [] S, int A, int B, Point [] Hdr, ExtElement EK) {
        int i, j, index;
        Point g_t1 = new Point();
        Point_proj ppt = new Point_proj();

        // HDR
        PointArith.point_mult(t, PK[0], Hdr[0]);

        for (i=1; i<=A; i++) {
            ppt.x.copy(PK[2*B+i-1].x);
            ppt.y.copy(PK[2*B+i-1].y);
            ppt.z.copy(Constants.ELEMENT_ONE);
            index = (i-1)*B;
            for (j=1; j<=B; j++) {
                if (((S[index/8] >> (index%8)) & 0x1) != 0)
                    PointArith.point_add_proj(ppt, PK[B+1-j], ppt);
                index++;
            }
            
            BaseField.base_element_inver(ppt.z, ppt.z);
            BaseField.base_element_mult(ppt.x, ppt.z, Hdr[i].x);
            BaseField.base_element_mult(ppt.y, ppt.z, Hdr[i].y);
            PointArith.point_mult(t, Hdr[i], Hdr[i]);
        }

        //EK, e(g(B+1), g)^t = e(gB, g1)^t = e(gB, t*g1)
        PointArith.point_mult(t, PK[1], g_t1);
        EtaT_Pairing.etaT_pairing(PK[B], g_t1, EK);
    }

    /**
     * decryption API
     * 
     * @param S User subset
     * @param A Size of S
     * @param B Size of PK
     * @param id User ID
     * @param PK Public Key
     * @param Hdr Header
     * @param di Private Key
     * @param EK Encryption Key (session key)
     */
    public static void BE_decrypt(byte [] S, int A, int B, Point [] PK, Point [] Hdr, int id, Point di, ExtElement EK) {
        ExtElement deno = new ExtElement();
        Point g_Sa = new Point();
        int a, b, j, index, gi;

        a = (id-1)/B+1;
        b = id%B;
        if (b == 0)
            b = B;

        g_Sa.copy(di);
        index = (a-1) * B;
        for (j=1; j<=B; j++) {
            if (((S[index/8] >> (index%8)) & 0x1) != 0) {
                gi = B-j+b;
                if (gi != B) {
                    if (gi < B)
                        gi++;
                    PointArith.point_add(g_Sa, PK[gi], g_Sa);
                }
            }
            index++;
        }

        //EK
        EtaT_Pairing.etaT_pairing(PK[b], Hdr[a], EK);
        EtaT_Pairing.etaT_pairing(g_Sa, Hdr[0], deno);
        ExtField.ext_element_inver(deno, deno);
        ExtField.ext_element_mult(EK, deno, EK);
    }


}

