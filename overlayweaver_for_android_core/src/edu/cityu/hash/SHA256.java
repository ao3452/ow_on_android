/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.hash;

/**
 * Provides methods for SHA-256 hashing
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public final class SHA256 {

    /**
     * Hash a byte array into a 256 bits value
     *
     * @param data source data to be hashed
     * @return The hashed value, in array of 32 bytes
     */
    public static byte[] SHA256(byte[] data) {
        int len = data.length;
        int [] msg = new int [(len>>2)+((len&0x03)==0?0:1)];

        int index = 0;
        int temp = 0;
        for (int i=0; i<msg.length; i++) {
            temp = 0;
            if (i == msg.length-1) {
                while (index < data.length) {
                    temp += data[index++];
                    temp <<= 8;
                }
            }
            else {
                for (int j=0; j<4; j++) {
                    temp += data[index++];
                    temp <<= 8;
                }
            }
            msg[i] = temp;
        }

        int dataSize = data.length*8;
        return SHA256(msg, dataSize);
    }

    /**
     * Hash a string into a 256 bits value
     *
     * @param str The string to be hashed
     * @return The hashed value, in array of 32 bytes
     */
    public static byte[] SHA256(String str) {
        int[] msg = Str2bin(UTF8Encode(str));
        int len = str.length()*8;
        return SHA256(msg, len);
    }

    private static byte[] SHA256(int[] msg, int len) {
        int [] K = {0x428A2F98, 0x71374491, 0xB5C0FBCF, 0xE9B5DBA5, 0x3956C25B, 0x59F111F1, 0x923F82A4, 0xAB1C5ED5, 0xD807AA98,
        0x12835B01, 0x243185BE, 0x550C7DC3, 0x72BE5D74, 0x80DEB1FE, 0x9BDC06A7, 0xC19BF174, 0xE49B69C1, 0xEFBE4786, 0x0FC19DC6,
        0x240CA1CC, 0x2DE92C6F, 0x4A7484AA, 0x5CB0A9DC, 0x76F988DA, 0x983E5152, 0xA831C66D, 0xB00327C8, 0xBF597FC7, 0xC6E00BF3,
        0xD5A79147, 0x06CA6351, 0x14292967, 0x27B70A85, 0x2E1B2138, 0x4D2C6DFC, 0x53380D13, 0x650A7354, 0x766A0ABB, 0x81C2C92E,
        0x92722C85, 0xA2BFE8A1, 0xA81A664B, 0xC24B8B70, 0xC76C51A3, 0xD192E819, 0xD6990624, 0xF40E3585, 0x106AA070, 0x19A4C116,
        0x1E376C08, 0x2748774C, 0x34B0BCB5, 0x391C0CB3, 0x4ED8AA4A, 0x5B9CCA4F, 0x682E6FF3, 0x748F82EE, 0x78A5636F, 0x84C87814,
        0x8CC70208, 0x90BEFFFA, 0xA4506CEB, 0xBEF9A3F7, 0xC67178F2};

        byte [] hashInByte = new byte [32];
        int [] HASH = {0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A, 0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19};
        int [] W = new int [64];
        int a, b, c, d, e, f, g, h;
        int T1, T2;

        msg = PreProcess(msg, len);

        for (int i=0; i<msg.length; i+=16) {
            a = HASH[0];
            b = HASH[1];
            c = HASH[2];
            d = HASH[3];
            e = HASH[4];
            f = HASH[5];
            g = HASH[6];
            h = HASH[7];

            for (int j=0; j<64; j++) {
                if (j < 16)
                    W[j] = msg[j+i];
                else
                    W[j] = Gamma1256(W[j-2]) + W[j-7] + Gamma0256(W[j-15]) + W[j-16];

                T1 = h + Sigma1256(e) + Ch(e, f, g) + K[j] + W[j];
                T2 = Sigma0256(a) + Maj(a, b, c);

                h = g;
                g = f;
                f = e;
                e = d + T1;
                d = c;
                c = b;
                b = a;
                a = T1 + T2;
            }

            HASH[0] = a + HASH[0];
            HASH[1] = b + HASH[1];
            HASH[2] = c + HASH[2];
            HASH[3] = d + HASH[3];
            HASH[4] = e + HASH[4];
            HASH[5] = f + HASH[5];
            HASH[6] = g + HASH[6];
            HASH[7] = h + HASH[7];
        }

        int index = 0;
        for (int i=0; i<8; i++) {
            byte [] temp = Int2Bytes(HASH[i]);
            hashInByte[index++] = temp[0];
            hashInByte[index++] = temp[1];
            hashInByte[index++] = temp[2];
            hashInByte[index++] = temp[3];
        }

        return hashInByte;
    }

    /**
     * pre-processing
     */
    private static int[] PreProcess(int[] msg, int len) {
//        System.out.println(msg.length+" "+len);
        int[] newMsg;
        if (((len+65) & 0x1FF) == 0)
            newMsg = new int [(len+65)/32];
        else
            newMsg = new int [((len+65)/512+1)*16];

        for (int i=0; i<msg.length; i++)
            newMsg[i] = msg[i];

        
        newMsg[(len>>5)] |= 0x80 << (24 - len % 32);
        newMsg[((len+64>>9)<<4)+15] = len;

        return newMsg;
    }

    /**
     * Right Rotate
     *
     * @param x The int to be swapped
     * @param n Obviously n < 32
     */
    private static int RRotate(int x, int n) {
        return ((x>>>n) | (x<<(32-n)));
    }

    private static int Ch(int x, int y, int z) {
        return ((x&y) ^ ((~x)&z));
    }

    private static int Maj(int x, int y, int z) {
        return ((x&y) ^ (x&z) ^ (y&z));
    }

    private static int Sigma0256(int x) {
        return (RRotate(x, 2) ^ RRotate(x, 13) ^ RRotate(x, 22));
    }

    private static int Sigma1256(int x) {
        return (RRotate(x, 6) ^ RRotate(x, 11) ^ RRotate(x, 25));
    }

    private static int Gamma0256(int x) {
        return (RRotate(x, 7) ^ RRotate(x, 18) ^ (x >>> 3));
    }

    private static int Gamma1256(int x) {
        return (RRotate(x, 17) ^ RRotate(x, 19) ^ (x >>> 10));
    }

    private static String UTF8Encode(String str) {
        String newStr = "";

        for (int i=0; i<str.length(); i++) {
            int c = str.charAt(i);

            if (c < 128) {
                newStr += (char)c;
            }
            else if (c < 2048) {
                newStr += (char)((c >> 6) | 192);
                newStr += (char)((c & 63) | 128);
            }
            else {
                newStr += (char)((c >> 12) | 224);
                newStr += (char)(((c >> 6) & 63) | 128);
                newStr += (char)((c & 63) | 128);
            }
        }
        return newStr;
    }

    private static int[] Str2bin(String str) {
        int len = str.length();
        int[] bin = new int [(len>>2) + ((len&0x3)==0?0:1)];
        for (int i=0; i<(len*8); i+=8) {
            bin[i>>5] |= (str.charAt(i/8)) << (24-i%32);
        }
        
        return bin;
    }

    private static byte[] Int2Bytes(int a) {
        byte [] b = new byte [4];

        b[0] = (byte) (a >> 24 & 0xff);
        b[1] = (byte) (a >> 16 & 0xff);
        b[2] = (byte) (a >> 8 & 0xff);
        b[3] = (byte) (a & 0xff);

        return b;
    }
}
