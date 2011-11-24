/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp;

import edu.cityu.util.Constants;

/**
 * Data structure for MP operation
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class MP_CTX {

    public short tos;
    public MP_INT [] t;

    public MP_CTX() {
        t = new MP_INT [Constants.MP_CTX_NUM+1];
        for (int i=0; i<Constants.MP_CTX_NUM+1; i++)
            t[i] = new MP_INT();
        tos = 0;
    }

    public void clear() {
        for (int i=0; i<t.length; i++)
            t[i].clear();
    }

}
