/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

/**
 * Data structure for supporting Element compress
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class CpElement {

    /**
     * CpElement has 20 bytes
     */
    public byte [] value = new byte [Constants.CPELEMENT_LEN];

    /**
     *
     */
    public CpElement() {};

    /**
     * Constructor with initial value
     * @param value the initial value
     */
    public CpElement(byte [] value) {
        if (value.length==Constants.CPELEMENT_LEN) {
            for (int i=0; i<Constants.CPELEMENT_LEN; i++) {
                this.value[i] = value[i];
            }
        }
    }

    /**
     * Copy another CpElement into this object
     * @param c the CpElement to be copied
     */
    public void copy(CpElement c) {
        for (int i=0; i<Constants.CPELEMENT_LEN; i++) {
            this.value[i] = c.value[i];
        }
    }

    /**
     * Generate a ECORDER
     * @return ECORDER
     */
    public static CpElement get_ECORDER() {
        byte [] value = {(byte)0xC7, (byte)0x13, (byte)0x79, (byte)0xC8, (byte)0xBD,
        (byte)0x3A, (byte)0x72, (byte)0x1A, (byte)0x19, (byte)0xB7, (byte)0xDC, (byte)0x0C,
        (byte)0x4F, (byte)0x20, (byte)0xDC, (byte)0xF0, (byte)0x23, (byte)0xF0, (byte)0x57, (byte)0x03};

        CpElement ECORDER = new CpElement(value);
        return ECORDER;
    }
}
