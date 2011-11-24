/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

/**
 * Data structure to represent extend element
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class ExtElement {

    /**
     * ExtElement is constructed by 2 MidElement
     */
    public MidElement [] ext = new MidElement [2];

    /**
     *
     */
    public ExtElement() {
        ext[0] = new MidElement();
        ext[1] = new MidElement();
    }

    /**
     * Test if two ExtElements are the same
     * @param a the ExtElement to be tested
     * @return true if equal, false if different
     */
    public boolean equal(ExtElement a) {
        for (int i=0; i<2; i++) {
            if (!this.ext[i].equal(a.ext[i]))
                return false;
        }
        return true;
    }

    /**
     *
     * @return
     */
    public byte[] getBytes() {
        int len = 2*3*2*Constants.ELEMENT_LEN;
        byte [] result = new byte[len];

        int index = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<Constants.ELEMENT_LEN; k++) {
                    result[index++] = (byte) this.ext[i].mid[j].hi[k];
                    result[index++] = (byte) this.ext[i].mid[j].lo[k];
                }
            }
        }

        return result;
    }
    
}
