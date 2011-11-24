/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.rand;

import java.util.Date;
import edu.cityu.aes.AES_Encrypt;
import edu.cityu.hash.SHA256;

/**
 * Fortuna psuedorandom number generator
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class Generator {

    /**
     * Ctr is the counter to be encrypt
     * ctr is big-endian, i.e., MSB is ctr[0]
     */
    private static byte [] ctr = new byte [16];
    private byte [] key = new byte [32];
    private int counter = 0;
    private byte [] newKeyBuff = new byte [16];

    /**
     *
     */
    public Generator() {
        reseed();
        incCtr();
        counter = 0;
    }

    /**
     * Generate a random block of data (16 bytes)
     * @return 16 bytes of random data
     */
    public byte[] genRand() {
        byte [] newRand = AES_Encrypt.Encrypt(ctr, key);    // newRand is 16 bytes
        counter++;
        incCtr();

        // the key is changed for every 10 blocks of random data
        if (counter == 9)
            for (int i=0; i<16; i++)
                newKeyBuff[i] = newRand[i];
        if (counter == 10) {
            for (int i=0; i<16; i++) {
                key[i] = newKeyBuff[i];
                key[16+i] = newRand[i];
            }
            counter = 0;
        }

        return newRand;
    }

    /**
     * reseed using the current time
     */
    private void reseed() {
        Date d = new Date();
//        System.out.println(d.getTime());
        key = SHA256.SHA256(Long.toString(d.getTime()));  // keyInt is 8 ints

//        int index=0;
//        for (int i=0; i<8; i++) {
//            byte [] tempBytes = int2Bytes(keyInt[i]);
//            key[index++] = tempBytes[0];
//            key[index++] = tempBytes[1];
//            key[index++] = tempBytes[2];
//            key[index++] = tempBytes[3];
//        }
    }

//    private byte[] int2Bytes(int a) {
//        byte [] b = new byte [4];
//
//        b[0] = (byte) (a >> 24 & 0xff);
//        b[1] = (byte) (a >> 16 & 0xff);
//        b[2] = (byte) (a >> 8 & 0xff);
//        b[3] = (byte) (a & 0xff);
//
//        return b;
//    }

    /**
     * Increase ctr by 1
     * no need for checking overflow, since 2^128 bits can generate
     * 2^128*16 bytes random date
     */
    private static void incCtr() {
        int h1 = 0;
        int h2 = 0;
        int c = 0;

        ctr[15] = (byte) (ctr[15] + 1);

        if (ctr[15]==0)
            c = 1;
        else
            c = 0;

        for (int i=14; i>=0; i--) {
            if (c==0) return;

            ctr[i] = (byte) (ctr[i] + c);
            
            if (ctr[i]==0)
                c = 1;
            else
                c = 0;
        }
    }

}
