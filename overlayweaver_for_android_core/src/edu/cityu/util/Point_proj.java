/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.cityu.util;

/**
 * Data structure to represent a point projection
 * @author BAO Yiyang <yiyang.bao@gmail.com>
 */
public class Point_proj {

    /**
     * Coordinate x of a point projection
     */
    public Element x;
    /**
     * Coordinate y of a point projection
     */
    public Element y;
    /**
     * Coordinate z of a point projection
     */
    public Element z;

    /**
     *
     */
    public Point_proj() {
        this.x = new Element();
        this.y = new Element();
        this.z = new Element();
    }

    /**
     * Copy another Point_proj into this object
     * @param a the Point_proj object to be copied
     */
    public void copy(Point_proj a) {
        this.x.copy(a.x);
        this.y.copy(a.y);
        this.z.copy(a.z);
    }
}
