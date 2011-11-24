/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

/**
 * Data structure for mid element
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class MidElement {

    /**
     * MidElement is constructed by 3 Elements
     */
    public Element [] mid = new Element [3];

    /**
     *
     */
    public MidElement() {
        mid[0] = new Element();
        mid[1] = new Element();
        mid[2] = new Element();
    }

    /**
     * Test if two MidElements are the same
     * @param a the MidElement object to be tested
     * @return true if equal, false if different
     */
    public boolean equal(MidElement a) {
        for (int i=0; i<3; i++) {
            if (!this.mid[i].equal(a.mid[i]))
                return false;
        }
        return true;
    }
    
}
