/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.aes;

/**
 * Provides methods for AES Encryption
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class AES_Encrypt {

    /**
     * @param text (arbitrary length)
     * @param key (32 bytes)
     * @return cipher (length of text taking ceiling to multiples of 16)
     */
    public static byte[] Encrypt(byte[] text, byte[] key) {
        byte [] tempText = new byte [16];
        byte [] tempCipher = new byte [16];
        byte [] cipher = new byte [((text.length%16==0)?text.length:((text.length/16+1)*16))];

        byte [] expKey = new byte [240];
        Utils.KeyExpansion(key, expKey);

        int i=0;
        int tempTexti = 0;
        int cipherIndex = 0;
        while (i<text.length) {
            tempTexti = 0;
            tempText = new byte [16];
            // get 16 bytes or remaining text (if less than 16) into tempText
            if ((text.length-i) < 16) {
                for (; i<text.length; i++)
                    tempText[tempTexti++] = text[i];
            }
            else {
                for (int j=0; j<16; j++) {
                    tempText[tempTexti++] = text[i++];
                }
            }

            // encrypt tempText
            tempCipher = encryptBlock(tempText, expKey);

            for (int j=0; j<16; j++) {
                cipher[cipherIndex++] = tempCipher[j];
            }
        }

        return cipher;
    }

    /**
     * @param text (16 bytes), expKey (240 bytes)
     * @return cipher (16 bytes)
     */
    private static byte[] encryptBlock(byte[] text, byte[] expKey) {
        byte [] textCopy = new byte [16];
        byte [] rKey = new byte [16];
//        byte [] expKey = new byte [240];
//        Utils.KeyExpansion(key, expKey);
        
        for (int i=0; i<16; i++)
            textCopy[i] = text[i];

        // initial Add Round Key
        for (int i=0; i<16; i++)
            rKey[i] = expKey[i];
        Utils.AddRoundKey(textCopy, rKey);

        // 13 rounds
        for (int i=0; i<13; i++) {
            SubBytes(textCopy);
            ShiftRow(textCopy);
            MixColumns(textCopy);
            for (int j=0; j<16; j++)
                rKey[j] = expKey[16*(i+1)+j];
            Utils.AddRoundKey(textCopy, rKey);
        }

        // final round
        SubBytes(textCopy);
        ShiftRow(textCopy);
        for (int j=0; j<16; j++)
            rKey[j] = expKey[224+j];
        Utils.AddRoundKey(textCopy, rKey);

        return textCopy;
    }

    /**
     * state is 128 bits (16 bytes)
     */
    private static void SubBytes(byte[] state) {
        for (int i=0; i<16; i++)
            state[i] = Utils.SubOneByte(state[i]);
    }

    /**
     * state is 128 bits (16 bytes)
     */
    private static void ShiftRow(byte[] state) {
        Utils.swap(state, 1, 5);
        Utils.swap(state, 5, 9);
        Utils.swap(state, 9, 13);
        Utils.swap(state, 2, 10);
        Utils.swap(state, 6, 14);
        Utils.swap(state, 11, 15);
        Utils.swap(state, 7, 11);
        Utils.swap(state, 3, 7);
    }

    /**
     * state is 128 bits (16 bytes)
     */
    private static void MixColumns(byte[] state) {
        byte [] col = new byte [4];

        for (int i=0; i<4; i++) {
            for (int j=0; j<4; j++)
                col[j] = state[4*i+j];
            MixOneColumn(col);
            for (int j=0; j<4; j++)
                state[4*i+j] = col[j];
        }
    }

    private static void MixOneColumn(byte[] col) {
        byte [] a = new byte [4];
        byte [] b = new byte [4];
        byte h;

        for (int i=0; i<4; i++) {
            a[i] = col[i];
            h = (byte) (col[i] >> 7);
            b[i] = (byte) (col[i] << 1);
            if ((h & 0x01) == 1)
                b[i] ^= 0x1b;
        }
        col[0] = (byte) (b[0] ^ a[3] ^ a[2] ^ b[1] ^ a[1]); /* 2 * a0 + a3 + a2 + 3 * a1 */
	col[1] = (byte) (b[1] ^ a[0] ^ a[3] ^ b[2] ^ a[2]); /* 2 * a1 + a0 + a3 + 3 * a2 */
	col[2] = (byte) (b[2] ^ a[1] ^ a[0] ^ b[3] ^ a[3]); /* 2 * a2 + a1 + a0 + 3 * a3 */
	col[3] = (byte) (b[3] ^ a[2] ^ a[1] ^ b[0] ^ a[0]); /* 2 * a3 + a2 + a1 + 3 * a0 */
    }
}
