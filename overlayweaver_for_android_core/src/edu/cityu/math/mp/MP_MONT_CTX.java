/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.math.mp;

/**
 * Date structure for supporting MP operations
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class MP_MONT_CTX {

    public short n;
    public MP_INT [] R_m;
    public MP_INT [] R2_m;
    public MP_INT [] m;
    public short mi;

}
