/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.bp;

import edu.cityu.util.Constants;
import edu.cityu.util.CpElement;
import edu.cityu.util.Element;

/**
 * Arithmetic for Base Field
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class BaseField {
    /**
     * This function has limited application that element_len should not exceed 255
     *
     * Be noted that, if this routine is used for subsraction, i.e., b.hi is exchanged with b.lo
     * It is not allowed that b point to the same memory location as c.
     */
    private static void private_element_add(byte [] ahi, byte [] alo, byte [] bhi, byte [] blo, int digits, byte [] chi, byte [] clo) {
        int ti;
        byte [] temp_padd = new byte[Constants.ELEMENT_LEN+1];

        for (ti=0; ti<digits; ti++) {
            temp_padd[ti] = (byte) ((ahi[ti] | alo[ti]) & (bhi[ti] | blo[ti]));
            chi[ti] = (byte) ((ahi[ti] | bhi[ti]) ^ temp_padd[ti]);
            clo[ti] = (byte) ((alo[ti] | blo[ti]) ^ temp_padd[ti]);
        }
    }

    /**
     * For inverse function use
     * Compute the degree of Element a, and writes the degree to "degree"
     * The definition of degree is the value of most significant non-zero bit of an element
     *
     * Be noted that degree should not exceed 255
     */
    private static short private_element_deg(Element a) {
        int i = Constants.ELEMENT_LEN - 1;
        short degree = 0;

        if ((a.hi[i]==1) || (a.lo[i]==1))
            degree = Constants.CONSTANT_M - 1;
        else {
            i--;
            while ((a.hi[i]==0) && (a.lo[i]==0)) {
                i--;
            }
            degree = (short) (i * Constants.UINT_LEN);
            i = Constants.UINT_LEN - 1;
            while ((((a.hi[degree/Constants.UINT_LEN]& 0xFF) >>>i)==0) && (((a.lo[degree/Constants.UINT_LEN]& 0xFF) >>>i)==0))
                    i--;
            degree += i;
        }

        return degree;
    }

    /**
     * For inverse function use
     * Shift the Element a to the left with len bits, and writes the value to Element b
     * Use to calculate x^len
     */
    private static void private_element_shift(byte [] ahi, byte [] alo, byte [] bhi, byte [] blo, int eLen, int shift) {
        short byte_shift;
        short bit_shift;

        int i;
        byte_shift = (short) (shift / Constants.UINT_LEN);
        bit_shift = (short) (shift % Constants.UINT_LEN);
        for (i=0; i<(eLen-byte_shift); i++) {
            bhi[eLen-1-i] = ahi[eLen-1-byte_shift-i];
            blo[eLen-1-i] = alo[eLen-1-byte_shift-i];
        }

        for (i=0; i<byte_shift; i++) {
            bhi[i] = 0;
            blo[i] = 0;
        }

        bhi[eLen-1] = (byte) (bhi[eLen - 1] << bit_shift & Constants.DIGIT_MASK);
        blo[eLen-1] = (byte) (blo[eLen - 1] << bit_shift & Constants.DIGIT_MASK);
        if (bit_shift!= 0) {
            for (i=(eLen-2); i>=0; i--) {
                bhi[i+1] = (byte) ((bhi[i] & 0xFF) >>> (Constants.UINT_LEN-bit_shift) | bhi[i+1]);
                blo[i+1] = (byte) ((blo[i] & 0xFF) >>> (Constants.UINT_LEN-bit_shift) | blo[i+1]);
                bhi[i] = (byte) (bhi[i] << bit_shift & Constants.DIGIT_MASK);
                blo[i] = (byte) (blo[i] << bit_shift & Constants.DIGIT_MASK);
            }
        }
    }

    /**
     * Cube one byte a to 3 bytes c[3]
     */
    private static void private_void_byte_cube(byte a, byte [] c) {
        byte [] ct0 = {(byte)0x0, (byte)0x1, (byte)0x8, (byte)0x9, (byte)0x40, (byte)0x41, (byte)0x48, (byte)0x49};
        byte [] ct1 = {(byte)0x0, (byte)0x2, (byte)0x10, (byte)0x12, (byte)0x80, (byte)0x82, (byte)0x90, (byte)0x92};
        byte [] ct2 = {(byte)0x0, (byte)0x4, (byte)0x20, (byte)0x24};

        c[0] = ct0[(a & 0x07)];
        c[1] = ct1[((a & 0xFF) >>> 3) & 0x07];
        c[2] = ct2[((a & 0xFF) >>> 6) & 0x03];
    }

    /**
     * Num should range from 0 to 242
     */
    private static void private_byte_int2elmt(byte src, byte [] hi_lo) {
        short modulus;
        short orvalue;
        int i;
        short num = (short) (src & 0xFF);

        hi_lo[0] = 0;
        hi_lo[1] = 0;
        orvalue = 0x10;
        modulus = 81;

        /**
         * Compute element from highest bit to lowest one
         * i.e., from 5th bit to 1st bit
         */
        for (i=5; i>0; i--) {
            if (num >= (modulus << 1 & Constants.DIGIT_MASK)) {
                hi_lo[0] |= orvalue;
            }
            else if (num >= modulus) {
                hi_lo[1] |= orvalue;
            }

            if (i>1) {
                num = (byte) (num % modulus);
                modulus /= 3;
                orvalue >>>= 1;
            }
        }
    }

    private static void private_byte_elmt2int(byte hi, byte lo, byte [] value, int index) {
        int i;
        byte adder;
        byte andvalue;

        value[index] = 0;
        adder = 1;
        andvalue = 0x1;

        /**
         * Compute from low bit to high bit
         * If ith bit is not zero, add 3^i to the num
         */
        for (i=0; i<5; i++) {
            if ((hi & andvalue)!=0) {
                value[index] += adder*2;
            }
            else if ((lo & andvalue) != 0) {
                value[index] += adder;
            }

            if (i!=4) {
                andvalue = (byte) (andvalue << 1 & Constants.DIGIT_MASK);
                adder *= 3;
            }
        }
    }

    /**
     * C(x) := A(x) + B(x) in GF(3^m)
     *
     * This routime allows C(x) to be A(x) or B(x)
     */
    public static void base_element_add(Element A, Element B, Element C) {
        private_element_add(A.hi, A.lo, B.hi, B.lo, Constants.ELEMENT_LEN, C.hi, C.lo);
    }

    /**
     * C(x) := A(x) - B(x) := A(x) + (-B(x)) in GF(3^m)
     *
     * Note: in GF(3^m), if B(x) = (hi, lo), then -B(x) = (lo, hi).
     * Hence -B(x) is simply the "bitwise swapping" between HIGH bits and LOW bits.
     *
     * This routime also allows C(x) to be A(x) or B(x)
     */
    public static void base_element_sub(Element A, Element B, Element C) {
        byte [] thi = new byte [Constants.ELEMENT_LEN];
        for (int i=0; i<Constants.ELEMENT_LEN; i++)
            thi[i] = B.hi[i];
        private_element_add(A.hi, A.lo, B.lo, thi, Constants.ELEMENT_LEN, C.hi, C.lo);
    }

    /**
     * Used to copy an array to another
     */
    private static void private_memcpy(byte [] dest, byte [] src, int dest_start, int src_start, int len) {
        for (int i=0; i<len; i++)
            dest[dest_start+i] = src[src_start+i];
    }

    /**
     * C(x) := A(x) * B(x) in GF(3^m)
     * This routine allows C(x) to be A(x) or B(x)
     * For GF(3^97) only
     */
    public static void base_element_mult(Element A, Element B, Element C) {
        short j;
        int k;
        byte uk, res;
        byte [][] temp_mult = new byte [8][Constants.ELEMENT_LEN];
        byte [][] longtemp_mult = new byte [2][2*Constants.ELEMENT_LEN];


        byte [] longtemp_hi = new byte [Constants.ELEMENT_LEN];
        byte [] longtemp_lo = new byte [Constants.ELEMENT_LEN];

        // "01" is B
        // precomputation for "10", stored in temp_mult[0,1][];
        for (j=(Constants.ELEMENT_LEN-1); j>0; j--) {
            temp_mult[0][j] = (byte) (B.hi[j] << 1 & Constants.DIGIT_MASK);
            temp_mult[0][j] |= (B.hi[j-1] & 0xFF) >>> (Constants.UINT_LEN-1);
            temp_mult[1][j] = (byte) (B.lo[j] << 1 & Constants.DIGIT_MASK);
            temp_mult[1][j] |= (B.lo[j-1] & 0xFF) >>> (Constants.UINT_LEN-1);
        }
        temp_mult[0][0] = (byte) (B.hi[0] << 1 & Constants.DIGIT_MASK);
        temp_mult[1][0] = (byte) (B.lo[0] << 1 & Constants.DIGIT_MASK);

        // precomputation for "11", stored in temp_mult[2,3][]
        private_element_add(temp_mult[0], temp_mult[1], B.hi, B.lo, Constants.ELEMENT_LEN, temp_mult[2], temp_mult[3]);

        // precomputation for "12", stored in temp_mult[4,5][]
        private_element_add(temp_mult[0], temp_mult[1], B.lo, B.hi, Constants.ELEMENT_LEN, temp_mult[4], temp_mult[5]);

        for (int i=0; i<Constants.ELEMENT_LEN; i++) {
            temp_mult[6][i] = B.hi[i];
            temp_mult[7][i] = B.lo[i];
        }

        // step 1: multiplication
        // left-right comb multiplication method
        for (k=6; k>=0; k-=2) {
            for (j=0; j<Constants.ELEMENT_LEN; j++) {
                uk = (byte) ((((byte) ((A.hi[j] & 0xFF) >>> k)) & 0x3) << 2 & Constants.DIGIT_MASK);
                uk |= (((A.lo[j] & 0xFF) >>> k) & 0x3);
                switch (uk) {
                    case 1:
                        uk = 6;
                        break;
                    case 4:
                        uk = 7;
                        break;
                    case 2:
                        uk = 0;
                        break;
                    case 3:
                        uk = 2;
                        break;
                    case 6:
                        uk = 4;
                        break;
                    case 8:
                        uk = 1;
                        break;
                    case 9:
                        uk = 5;
                        break;
                    case 12:
                        uk = 3;
                        break;
                    default:
                        uk = 8;
                        break;
                }
                if (uk != 8) {
                    if (uk % 2 == 0)
                        res = (byte) (uk + 1);
                    else
                        res = (byte) (uk - 1);

                    // copy longtemp_mult[]+j to a new array in order to use private_element_add()
                    // copy longtemp_hi/lo back after addition
                    private_memcpy(longtemp_hi, longtemp_mult[0], 0, j, Constants.ELEMENT_LEN);
                    private_memcpy(longtemp_lo, longtemp_mult[1], 0, j, Constants.ELEMENT_LEN);
                    private_element_add(longtemp_hi, longtemp_lo, temp_mult[uk], temp_mult[res], Constants.ELEMENT_LEN, longtemp_hi, longtemp_lo);
                    private_memcpy(longtemp_mult[0], longtemp_hi, j, 0, Constants.ELEMENT_LEN);
                    private_memcpy(longtemp_mult[1], longtemp_lo, j, 0, Constants.ELEMENT_LEN);
                    
//                    for (int z=0; z<Constants.ELEMENT_LEN*2; z++) {
//                        System.out.print(Integer.toHexString(longtemp_mult[0][z])+" ");
//                    }
//                    System.out.println();
                }
            }
            if (k!=0) {
                for (j=(2*Constants.ELEMENT_LEN-1); j>0; j--) {
                    longtemp_mult[0][j] = (byte) (longtemp_mult[0][j] << 2 & Constants.DIGIT_MASK);
                    longtemp_mult[0][j] |= (longtemp_mult[0][j-1] & 0xFF) >>> (Constants.UINT_LEN-2);
                    longtemp_mult[1][j] = (byte) (longtemp_mult[1][j] << 2 & Constants.DIGIT_MASK);
                    longtemp_mult[1][j] |= (longtemp_mult[1][j-1] & 0xFF) >>> (Constants.UINT_LEN-2);
                }
                longtemp_mult[0][0] = (byte) (longtemp_mult[0][0] << 2 & Constants.DIGIT_MASK);
                longtemp_mult[1][0] = (byte) (longtemp_mult[1][0] << 2 & Constants.DIGIT_MASK);
            }
        }

        // step 2: reduction
        // we do faster reduction here

        // shift bits 97 to 192 and add to c0 to c95
        // copy longtemp_mult[0]/[1] out, but they remain unchanged hence unneccessary to copy back
        private_memcpy(longtemp_hi, longtemp_mult[0], 0, 12, Constants.ELEMENT_LEN);
        private_memcpy(longtemp_lo, longtemp_mult[1], 0, 12, Constants.ELEMENT_LEN);
        private_element_shift(longtemp_hi, longtemp_lo, temp_mult[0], temp_mult[1], Constants.ELEMENT_LEN, 7);

        byte [] temp_hi = new byte [Constants.ELEMENT_LEN];
        byte [] temp_lo = new byte [Constants.ELEMENT_LEN];
        private_memcpy(temp_hi, temp_mult[0], 0, 1, Constants.ELEMENT_LEN-1);
        private_memcpy(temp_lo, temp_mult[1], 0, 1, Constants.ELEMENT_LEN-1);
        private_element_add(longtemp_mult[0], longtemp_mult[1], temp_hi, temp_lo, Constants.ELEMENT_LEN-1, C.hi, C.lo);

        // shift bits 97 to 180 and minus to c12 to c95
        private_element_shift(longtemp_hi, longtemp_lo, temp_mult[0], temp_mult[1], Constants.ELEMENT_LEN-2, 3);
        temp_mult[0][0] &= 0xF0;
        temp_mult[1][0] &= 0xF0;

        // substraction
        private_memcpy(temp_hi, C.hi, 0, 1, Constants.ELEMENT_LEN-2);
        private_memcpy(temp_lo, C.lo, 0, 1, Constants.ELEMENT_LEN-2);
        private_element_add(temp_hi, temp_lo, temp_mult[1], temp_mult[0], Constants.ELEMENT_LEN-2, temp_hi, temp_lo);
        private_memcpy(C.hi, temp_hi, 1, 0, Constants.ELEMENT_LEN-2);
        private_memcpy(C.lo, temp_lo, 1, 0, Constants.ELEMENT_LEN-2);

        // shift bits 182 to 192 and add to c0 to c10
        private_memcpy(longtemp_hi, longtemp_mult[0], 0, 22, 3);
        private_memcpy(longtemp_lo, longtemp_mult[1], 0, 22, 3);
        private_element_shift(longtemp_hi, longtemp_lo, temp_mult[0], temp_mult[1], 3, 2);
        temp_mult[0][2] &= 0x07;
        temp_mult[1][2] &= 0x07;

        // substraction
        private_memcpy(temp_hi, temp_mult[0], 0, 1, 2);
        private_memcpy(temp_lo, temp_mult[1], 0, 1, 2);
        private_element_add(C.hi, C.lo, temp_lo, temp_hi, 2, C.hi, C.lo);

        // shift bits 182 to 192 and add to c12 to c22
        private_element_shift(longtemp_hi, longtemp_lo, temp_mult[0], temp_mult[1], 3, 6);
        temp_mult[0][1] &= 0xF0;
        temp_mult[1][1] &= 0xF0;
        temp_mult[0][2] &= 0x7F;
        temp_mult[1][2] &= 0x7F;

        // using longtemp_hi/lo for temp storage of C.hi/lo
        private_memcpy(longtemp_hi, C.hi, 0, 1, 2);
        private_memcpy(longtemp_lo, C.lo, 0, 1, 2);
        private_memcpy(temp_hi, temp_mult[0], 0, 1, 2);
        private_memcpy(temp_lo, temp_mult[1], 0, 1, 2);
        private_element_add(longtemp_hi, longtemp_lo, temp_hi, temp_lo, 2, longtemp_hi, longtemp_lo);
        private_memcpy(C.hi, longtemp_hi, 1, 0, 2);
        private_memcpy(C.lo, longtemp_lo, 1, 0, 2);

        // bit c96 = c96 - c181
        temp_mult[0][0] = (byte) (((longtemp_mult[0][22] & 0xFF) >>> 5) & 0x01);
        temp_mult[1][0] = (byte) (((longtemp_mult[1][22] & 0xFF) >>> 5) & 0x01);
        temp_mult[0][1] = (byte) (longtemp_mult[0][12] & 0x01);
        temp_mult[1][1] = (byte) (longtemp_mult[1][12] & 0x01);

        private_memcpy(temp_hi, temp_mult[0], 0, 1, 1);
        private_memcpy(temp_lo, temp_mult[1], 0, 1, 1);
        private_element_add(temp_hi, temp_lo, temp_mult[1], temp_mult[0], 1, longtemp_hi, longtemp_lo);
        private_memcpy(C.hi, longtemp_hi, 12, 0, 1);
        private_memcpy(C.lo, longtemp_lo, 12, 0, 1);
    }

    /**
     * Cubing algorithms is modified in this version
     * Instead of cubing and reducting in traditional way, we directly use regroupping (mapping, also called permutation) to do modular cubing
     *
     * For GF(3^97) only
     */
    public static void base_element_cube(Element A, Element B) {
        /**
         * The result of cubing B0 to B96 is grouped to C0[4], C1[4], C2[4], and c96.
         * C0[4] = {c0, c3, c6, ..., c93}, and so for C1[4] and C2[4]
         */
        byte [] C0_hi = new byte [4];
        byte [] C0_lo = new byte [4];
        byte [] C1_hi = new byte [4];
        byte [] C1_lo = new byte [4];
        byte [] C2_hi = new byte [4];
        byte [] C2_lo = new byte [4];
        byte [] temp = new byte [3];
        int i;

        byte [] temp_C_hi = new byte [4];
        byte [] temp_C_lo = new byte [4];
        byte [] temp_B_hi = new byte [4];
        byte [] temp_B_lo = new byte [4];

        // First do one bit shift for a[33] to a[96] and move a[32] to a[96]
        // Store the result in B
        // a0 to a31
        private_memcpy(B.hi, A.hi, 0, 0, 4);
        private_memcpy(B.lo, A.lo, 0, 0, 4);
        temp[0] = (byte) (A.hi[4] & 0x01);
        temp[1] = (byte) (A.lo[4] & 0x01);
        for (i=4; i<(Constants.ELEMENT_LEN-1); i++) {
            B.hi[i] = (byte) (((A.hi[i] & 0xFF) >>> 1) | (A.hi[i + 1] << 7 & Constants.DIGIT_MASK));
            B.lo[i] = (byte) (((A.lo[i] & 0xFF) >>> 1) | (A.lo[i + 1] << 7 & Constants.DIGIT_MASK));
        }
        B.hi[Constants.ELEMENT_LEN-1] = temp[0];
        B.lo[Constants.ELEMENT_LEN-1] = temp[1];

        // C0
        private_memcpy(C0_hi, B.hi, 0, 0, 4);
        private_memcpy(C0_lo, B.lo, 0, 0, 4);

        temp[0] = (byte) (B.hi[11] & 0x0F);  //a89 to a92
        temp[1] = (byte) (B.lo[11] & 0x0F);  //a89 to a92
        byte [] temp_temp = new byte[4];
        temp_temp[0] = temp[1];
        private_element_add(C0_hi, C0_lo, temp, temp_temp, 1, C0_hi, C0_lo);    //C00
        temp[0] = (byte) (temp[0] << 4 & Constants.DIGIT_MASK);
        temp[1] = (byte) (temp[1] << 4 & Constants.DIGIT_MASK);
        temp_temp[0] = temp[1];
        private_element_add(C0_hi, C0_lo, temp_temp, temp, 1, C0_hi, C0_lo);    //C00 substraction

        temp[0] = (byte) ((B.hi[11] & 0xFF) >>> 4);    //a93 to a96
        temp[1] = (byte) ((B.lo[11] & 0xFF) >>> 4);    //a93 to a96
        temp_temp[0] = temp[1];
        private_element_add(C0_hi, C0_lo, temp, temp_temp, 1, C0_hi, C0_lo);    //C00
        
        temp_C_hi[0] = C0_hi[1];
        temp_C_lo[0] = C0_lo[1];
        private_element_add(temp_C_hi, temp_C_lo, temp_temp, temp, 1, temp_C_hi, temp_C_lo);    //C01 substraction
        C0_hi[1] = temp_C_hi[0];
        C0_lo[1] = temp_C_lo[0];

        // C1
        C1_hi[0] = (byte) ((B.hi[8] & 0x0F) | (B.hi[8] << 4 & Constants.DIGIT_MASK));     //a65 to a68 & a65 to a68
        C1_lo[0] = (byte) ((B.lo[8] & 0x0F) | (B.lo[8] << 4 & Constants.DIGIT_MASK));     //a65 to a68 & a65 to a68
        temp[0] = (byte) (B.hi[8] & 0xF0);   //a69 to a72
        temp[1] = (byte) (B.lo[8] & 0xF0);   //a69 to a72
        temp_temp[0] = temp[1];
        private_element_add(C1_hi, C1_lo, temp, temp_temp, 1, C1_hi, C1_lo);
        temp[0] = (byte) ((B.hi[7] & 0xF0) | ((B.lo[7] & 0xFF) >>> 4));
        temp[1] = (byte) ((B.lo[7] & 0xF0) | ((B.hi[7] & 0xFF) >>> 4));
        temp_temp[0] = temp[1];
        private_element_add(C1_hi, C1_lo, temp, temp_temp, 1, C1_hi, C1_lo);

        for (i=1; i<4; i++) {
            C1_hi[i] = (byte) (((B.hi[7 + i] & 0xFF) >>> 4) | (B.hi[8 + i] << 4 & Constants.DIGIT_MASK));
            C1_lo[i] = (byte) (((B.lo[7 + i] & 0xFF) >>> 4) | (B.lo[8 + i] << 4 & Constants.DIGIT_MASK));
            temp_C_hi[0] = C1_hi[i];
            temp_C_lo[0] = C1_lo[i];
            temp_B_hi[0] = B.hi[7+i];
            temp_B_lo[0] = B.lo[7+i];
            private_element_add(temp_C_hi, temp_C_lo, temp_B_hi, temp_B_lo, 1, temp_C_hi, temp_C_lo);   //C1i
            C1_hi[i] = temp_C_hi[0];
            C1_lo[i] = temp_C_lo[0];
            temp_B_hi[0] = B.hi[8+i];
            temp_B_lo[0] = B.lo[8+i];
            private_element_add(temp_C_hi, temp_C_lo, temp_B_hi, temp_B_lo, 1, temp_C_hi, temp_C_lo);   //C1i
            C1_hi[i] = temp_C_hi[0];
            C1_lo[i] = temp_C_lo[0];
        }

        // C2
        C2_hi[0] = (byte) ((B.hi[4] & 0x0F) | (B.lo[4] << 4 & Constants.DIGIT_MASK));
        C2_lo[0] = (byte) ((B.lo[4] & 0x0F) | (B.hi[4] << 4 & Constants.DIGIT_MASK));
        temp[0] = (byte) (B.hi[4] & 0xF0);
        temp[1] = (byte) (B.lo[4] & 0xF0);
        temp_temp[0] = temp[1];
        private_element_add(C2_hi, C2_lo, temp, temp_temp, 1, C2_hi, C2_lo);
        for (i=1; i<4; i++) {
            C2_hi[i] = (byte) (((B.lo[3 + i] & 0xFF) >>> 4) | (B.lo[4 + i] << 4 & Constants.DIGIT_MASK));
            C2_lo[i] = (byte) (((B.hi[3 + i] & 0xFF) >>> 4) | (B.hi[4 + i] << 4 & Constants.DIGIT_MASK));
            temp_C_hi[0] = C2_hi[i];
            temp_C_lo[0] = C2_lo[i];
            temp_B_hi[0] = B.hi[4+i];
            temp_B_lo[0] = B.lo[4+i];
            private_element_add(temp_C_hi, temp_C_lo, temp_B_hi, temp_B_lo, 1, temp_C_hi, temp_C_lo);
            C2_hi[i] = temp_C_hi[0];
            C2_lo[i] = temp_C_lo[0];
        }

        // start cubing
        for (i=0; i<4; i++) {
            byte [] cube_result = new byte [3];
            private_void_byte_cube(C0_hi[i], cube_result);
            private_memcpy(B.hi, cube_result, 3*i, 0, 3);
            private_void_byte_cube(C0_lo[i], cube_result);
            private_memcpy(B.lo, cube_result, 3*i, 0, 3);

            private_void_byte_cube(C1_hi[i], temp);     //cube C1
            B.hi[3*i] |= temp[0] << 1 & Constants.DIGIT_MASK;
            B.hi[3*i+1] |= temp[1] << 1 & Constants.DIGIT_MASK;
            B.hi[3*i+2] |= temp[2] << 1 & Constants.DIGIT_MASK;
            if ((temp[1]&0x80) != 0) {
                B.hi[3*i+2] |= 0x01;
            }

            private_void_byte_cube(C1_lo[i], temp);     //cube C1
            B.lo[3*i] |= temp[0] << 1 & Constants.DIGIT_MASK;
            B.lo[3*i+1] |= temp[1] << 1 & Constants.DIGIT_MASK;
            B.lo[3*i+2] |= temp[2] << 1 & Constants.DIGIT_MASK;
            if ((temp[1]&0x80) != 0) {
                B.lo[3*i+2] |= 0x01;
            }

            private_void_byte_cube(C2_hi[i], temp);     //cube C2
            B.hi[3*i] |= temp[0] << 2 & Constants.DIGIT_MASK;
            B.hi[3*i+1] |= temp[1] << 2 & Constants.DIGIT_MASK;
            B.hi[3*i+2] |= temp[2] << 2 & Constants.DIGIT_MASK;
            if ((temp[0]&0x40) != 0) {
                B.hi[3*i+1] |= 0x01;
            }
            if ((temp[1]&0x80) != 0) {
                B.hi[3*i+2] |= 0x02;
            }

            private_void_byte_cube(C2_lo[i], temp);     //cube C2
            B.lo[3*i] |= temp[0] << 2 & Constants.DIGIT_MASK;
            B.lo[3*i+1] |= temp[1] << 2 & Constants.DIGIT_MASK;
            B.lo[3*i+2] |= temp[2] << 2 & Constants.DIGIT_MASK;
            if ((temp[0]&0x40) != 0) {
                B.lo[3*i+1] |= 0x01;
            }
            if ((temp[1]&0x80) != 0) {
                B.lo[3*i+2] |= 0x02;
            }
        }
    }

    /**
     * B = A^(-1) mod F(x)
     * A, B can point to the same memory
     *
     * For GF(3^97) only
     */
    public static void base_element_inver(Element A, Element B) {
        Element fx = new Element();
        byte [] hi = {0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        byte [] lo = {0x0, 0x10, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x2};
        fx.set_hi(hi);
        fx.set_lo(lo);

        Element g2 = new Element();
        Element u = new Element();
        Element v = new Element();
        Element trans = new Element();
        short deg_u=0, deg_v=0;
        short t = 0;
        boolean equal = false;
        int j = 0;

        /**
         * check whether input is zero, which does not have inverse
         * here we simply return zero value
         */
        if (A.equal(g2)) {
            B.set_zero();
            return;
        }

        u.copy(A);
        v.copy(fx);
        B.set_zero();

        B.lo[0] = 1;

        deg_u = private_element_deg(u);
        deg_v = 97;

        // Use a temp_B to allow pointer swapping for B
        Element temp_B = new Element();
        temp_B.copy(B);
        while (deg_u != 0) {
//            System.out.println("deg_u="+deg_u+", deg_v="+deg_v);
            j = deg_u - deg_v;
            if (j < 0) {
                trans.copy(u);
                u.copy(v);
                v.copy(trans);

                trans.copy(temp_B);
                temp_B.copy(g2);
                g2.copy(trans);

                t = deg_u;
                deg_u = deg_v;
                deg_v = t;

                j = 0 - j;
            }
//            System.out.println("after if (j<0):");
//            System.out.println("u:"+Main.output_Element(u));
//            System.out.println("v:"+Main.output_Element(v));
//            System.out.println("trans:"+Main.output_Element(trans));
//            System.out.println("temp_B:"+Main.output_Element(temp_B));
//            System.out.println("g2:"+Main.output_Element(g2));

            if ((((u.hi[deg_u/8] & 0xFF) >>> (deg_u%8)) & ((v.lo[deg_v/8] & 0xFF) >>> (deg_v%8)) & 0x01) != 0)
                equal = true;
            else if ((((u.lo[deg_u/8]& 0xFF) >>>(deg_u%8)) & ((v.hi[deg_v/8]& 0xFF) >>>(deg_v%8)) & 0x01) != 0)
                equal = true;
            else
                equal = false;

            if (!equal) {
                base_element_neg(v, v);
                base_element_neg(g2, g2);
            }

            private_element_shift(v.hi, v.lo, trans.hi, trans.lo, Constants.ELEMENT_LEN, j);
            base_element_add(u, trans, u);
            private_element_shift(g2.hi, g2.lo, trans.hi, trans.lo, Constants.ELEMENT_LEN, j);
            base_element_add(temp_B, trans, temp_B);

//            System.out.println("j="+j);
//            System.out.println("u:"+Main.output_Element(u));
//            System.out.println("v:"+Main.output_Element(v));
//            System.out.println("g2:"+Main.output_Element(g2));
//            System.out.println("trans:"+Main.output_Element(trans));
//            System.out.println("temp_B:"+Main.output_Element(temp_B));

            deg_u = private_element_deg(u);
            deg_v = private_element_deg(v);
        }

        B.copy(temp_B);

        if(u.hi[0] == 1){
  	  base_element_neg(B, B);
        }
    }

    /**
     * compute opposite value of A
     * B = (-A)
     *
     * Note: A and B can be the same
     */
    public static void base_element_neg(Element A, Element B) {
        byte [] temp = new byte [Constants.ELEMENT_LEN];
        private_memcpy(temp, A.hi, 0, 0, Constants.ELEMENT_LEN);
        private_memcpy(B.hi, A.lo, 0, 0, Constants.ELEMENT_LEN);
        private_memcpy(B.lo, temp, 0, 0, Constants.ELEMENT_LEN);
    }

    /**
     * Base Conversion function
     * Compress an Element, 20% space saved
     * For speed concerns, here we use a byte to represent every 5 digits in radix 3
     */
    public static void base_elmt2cpelmt(Element E, CpElement cpElement) {
        int i, k;
        byte head, tail;
        byte hi, lo;

        for (i=0; i<Constants.CPELEMENT_LEN; i++) {
            k = i*5;
            tail = (byte) (k % Constants.UINT_LEN);
            head = (byte) ((k + 4) % Constants.UINT_LEN);

            k /= 8;

            // contain in one byte
            if (head > tail) {
                hi = (byte) (((E.hi[k] & 0xFF) >>> tail) & 0x1F);
                lo = (byte) (((E.lo[k] & 0xFF) >>> tail) & 0x1F);
            }

            // contain in two bytes
            else {
                hi = (byte) ((((E.hi[k] & 0xFF) >>> tail) | (E.hi[k + 1] << (Constants.UINT_LEN - tail) & Constants.DIGIT_MASK)) & 0x1F);
                lo = (byte) ((((E.lo[k] & 0xFF) >>> tail) | (E.lo[k + 1] << (Constants.UINT_LEN - tail) & Constants.DIGIT_MASK)) & 0x1F);
            }

            private_byte_elmt2int(hi, lo, cpElement.value, i);
        }
    }

    /**
     * Convert in inverse order
     * decompress an element
     */
    public static void base_cpelmt2elmt(CpElement cpElement, Element E) {
        int i, k;
        byte head, tail;
        byte hi=0, lo=0;
        byte [] hi_lo = new byte [2];

        E.set_zero();

        for (i=0; i<Constants.CPELEMENT_LEN; i++) {
            hi_lo[0] = hi;
            hi_lo[1] = lo;
            private_byte_int2elmt(cpElement.value[i], hi_lo);
            hi = hi_lo[0];
            lo = hi_lo[1];
            k = i*5;
            tail = (byte) (k % Constants.UINT_LEN);
            head = (byte) ((k + 4) % Constants.UINT_LEN);

            k /= 8;
            if (head > tail) {
                E.hi[k] |= hi<<tail & Constants.DIGIT_MASK;
                E.lo[k] |= lo<<tail & Constants.DIGIT_MASK;
            }
            else {
                E.hi[k] |= hi<<tail & Constants.DIGIT_MASK;
                E.lo[k] |= lo<<tail & Constants.DIGIT_MASK;
                E.hi[k+1] |= (hi& 0xFF) >>>(Constants.UINT_LEN-tail);
                E.lo[k+1] |= (lo& 0xFF) >>>(Constants.UINT_LEN-tail);
            }
        }
    }
}
