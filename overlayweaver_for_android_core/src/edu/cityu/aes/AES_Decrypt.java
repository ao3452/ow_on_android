/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.aes;

/**
 * Provides methods for AES Decryption
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class AES_Decrypt {

    /**
     * @param cipher (arbitrary length)
     * @param key (32 bytes)
     * @return text (cipher.length)
     */
    public static byte[] Decrypt(byte[] cipher, byte[] key) {
        byte [] tempCipher = new byte [16];
        byte [] tempText = new byte [16];
        byte [] text = new byte [cipher.length];

        byte [] expKey = new byte [240];
        Utils.KeyExpansion(key, expKey);

        int i=0;
        int tempCipheri = 0;
        int textIndex = 0;
        while (i<cipher.length) {
            tempCipheri = 0;
            tempCipher = new byte [16];
            // get 16 bytes or remaining text (if less than 16) into tempText
            for (int j=0; j<16; j++) {
                tempCipher[tempCipheri++] = cipher[i++];
            }

            // decrypt tempText
            tempText = decryptBlock(tempCipher, expKey);

            for (int j=0; j<16; j++) {
                text[textIndex++] = tempText[j];
            }
        }

        return text;
    }

    /**
     * @param cipher (16 bytes), key (32 bytes)
     * @return text (16 bytes)
     */
    private static byte[] decryptBlock(byte[] cipher, byte[] expKey) {
        byte [] cipherCopy = new byte [16];
        byte [] rKey = new byte [16];
//        byte [] expKey = new byte [240];
//        Utils.KeyExpansion(key, expKey);

        for (int i=0; i<16; i++)
            cipherCopy[i] = cipher[i];

        // initial Add Round Key
        for (int j=0; j<16; j++)
            rKey[j] = expKey[224+j];
        Utils.AddRoundKey(cipherCopy, rKey);

        // 13 rounds
        for (int i=0; i<13; i++) {
            InverShiftRow(cipherCopy);
            InverSubBytes(cipherCopy);

            for (int j=0; j<16; j++)
                rKey[j] = expKey[208-16*i+j];
            Utils.AddRoundKey(cipherCopy, rKey);

            InverMixColumns(cipherCopy);
        }

        // final round
        InverShiftRow(cipherCopy);
        InverSubBytes(cipherCopy);
        for (int i=0; i<16; i++)
            rKey[i] = expKey[i];
        Utils.AddRoundKey(cipherCopy, rKey);

        return cipherCopy;
    }

    /**
     * state is 128 bits (16 bytes)
     */
    private static void InverSubBytes(byte[] state) {
        for (int i=0; i<16; i++)
            state[i] = Utils.InverSubOneByte(state[i]);
    }

    /**
     * State is 16 bytes
     */
    private static void InverShiftRow(byte[] state) {
        Utils.swap(state, 9, 13);
        Utils.swap(state, 5, 9);
        Utils.swap(state, 1, 5);
        Utils.swap(state, 2, 10);
        Utils.swap(state, 6, 14);
        Utils.swap(state, 3, 7);
        Utils.swap(state, 7, 11);
        Utils.swap(state, 11, 15);
    }

    /**
     * state is 128 bits (16 bytes)
     */
    private static void InverMixColumns(byte[] state) {
        byte [] col = new byte [4];

        for (int i=0; i<4; i++) {
            for (int j=0; j<4; j++)
                col[j] = state[4*i+j];
            InverMixOneColumn(col);
            for (int j=0; j<4; j++)
                state[4*i+j] = col[j];
        }
    }

    private static void InverMixOneColumn(byte[] col) {
        byte [] a = new byte [4];   // a[] = col[]
        byte e = 14;
        byte b = 11;
        byte d = 13;
        byte nine = 9;
        byte h;

        for (int i=0; i<4; i++)
            a[i] = col[i];
        
        col[0] = (byte) (Field_mul(a[0], e) ^ Field_mul(a[1], b) ^ Field_mul(a[2], d) ^ Field_mul(a[3], nine)); /* 14 * a0 + 11 * a1 + 13 * a2 + 9 * a3 */
	col[1] = (byte) (Field_mul(a[0], nine) ^ Field_mul(a[1], e) ^ Field_mul(a[2], b) ^ Field_mul(a[3], d)); /* 9 * a0 + 14 * a1 + 11 * a2 + 13 * a3 */
	col[2] = (byte) (Field_mul(a[0], d) ^ Field_mul(a[1], nine) ^ Field_mul(a[2], e) ^ Field_mul(a[3], b)); /* 13 * a0 + 9 * a1 + 14 * a2 + 11 * a3 */
	col[3] = (byte) (Field_mul(a[0], b) ^ Field_mul(a[1], d) ^ Field_mul(a[2], nine) ^ Field_mul(a[3], e)); /* 11 * a0 + 13 * a1 + 9 * a2 + 14 * a3 */
    }

    /**
     * Multiplication in GF(2^8)
     * Ref: http://www.progressive-coding.com/tutorial.php#aes_decryption
     */
    private static byte Field_mul(byte a, byte b) {
        byte p = 0x00;
        byte h;
	for(int i=0; i<8; i++) {
            if ((b&0x01)==1)
                p ^= a;

            h = (byte) (a >> 7);
            a <<= 1;
            if ((h & 0x01) == 1)
                a ^= 0x1b;
            b >>= 1;
	}
	return p;
    }




}
