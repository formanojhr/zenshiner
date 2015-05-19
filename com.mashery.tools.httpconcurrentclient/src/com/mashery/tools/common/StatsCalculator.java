package com.mashery.tools.common;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * Created with IntelliJ IDEA.
 * User: mramakr1
 * Date: 11/18/13
 * Time: 4:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatsCalculator {

    private final DescriptiveStatistics stats;

    public StatsCalculator() {
        // Get a DescriptiveStatistics instance
        stats = new DescriptiveStatistics();
    }

    public synchronized void  addValue(long value){
        stats.addValue(value);
    }

    public double getMedian(){
        return  stats.getGeometricMean();

    }

    public double getMean(){
        return stats.getMean();

    }

    public double getMax(){
        return stats.getMax();

    }
    public double getMin(){
        return stats.getMin();

    }
    public double getTotal(){
        return stats.getN();

    }
}
