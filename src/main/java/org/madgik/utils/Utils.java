/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.madgik.utils;

import static cc.mallet.types.MatrixOps.dotProduct;
import static cc.mallet.types.MatrixOps.twoNorm;

import org.apache.commons.lang.ArrayUtils;
import org.knowceans.util.Vectors;

import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author omiros
 */
public class Utils {

    public static double cosineSimilarity(double[] m1, double[] m2) {

        return dotProduct(m1, m2) / (twoNorm(m1) * twoNorm(m2));

    }

    double[] softmax(double[] xs) {

        double a = Double.POSITIVE_INFINITY; //-1000000000.0;
        for (int i = 0; i < xs.length; ++i) {
            if (xs[i] > a) {
                a = xs[i];
            }
        }

        double Z = 0.0;
        for (int i = 0; i < xs.length; ++i) {
            Z += Math.exp(xs[i] - a);
        }

        double[] ps = new double[xs.length];
        for (int i = 0; i < xs.length; ++i) {
            ps[i] = Math.exp(xs[i] - a) / Z;
        }

        return ps;
    }


    public static Double [][] toDouble2DObject(double [][] in){
        if (in.length ==0 ) return null;
        Double [][]out = new Double[in.length][in[0].length];
        for (int i=0;i<in.length;++i) out[i] = ArrayUtils.toObject(in[i]);
        return out;
    }


    static Stack<Long> timestack = new Stack<>();
    public static void tic(){
        Utils.timestack.push(System.currentTimeMillis());
    }
    public static void toc(String msg){
        long millis = System.currentTimeMillis() - Utils.timestack.pop();
        String outmsg =  String.format("[%s] took: ", msg);
        if (TimeUnit.MILLISECONDS.toDays(millis) > 0) outmsg += String.format(" %d days", TimeUnit.MILLISECONDS.toDays(millis));
        if (TimeUnit.MILLISECONDS.toHours(millis) > 0) outmsg += String.format(" %d hours", TimeUnit.MILLISECONDS.toHours(millis));
        if (TimeUnit.MILLISECONDS.toMinutes(millis) > 0) outmsg += String.format(" %d minutes", TimeUnit.MILLISECONDS.toMinutes(millis));
        outmsg += String.format(" %d seconds", TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        System.out.println(outmsg);
    }

}
