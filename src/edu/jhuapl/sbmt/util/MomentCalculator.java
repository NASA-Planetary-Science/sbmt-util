package edu.jhuapl.sbmt.util;

public class MomentCalculator
{
    private double mean;
    private double variance;
    private double skewness;
    private double kurtosis;

    public MomentCalculator(double[] data)
    {
        mean=0;
        for (int i=0; i<data.length; i++)
            mean+=data[i];
        mean/=data.length;
        //
        variance=0;
        for (int i=0; i<data.length; i++)
            variance+=Math.pow(data[i]-mean, 2);
        variance/=data.length;
        //
        skewness=0;
        for (int i=0; i<data.length; i++)
            skewness+=Math.pow(data[i]-mean, 3);
        skewness/=data.length*Math.pow(variance, 3./2.);
        //
        kurtosis=0;
        for (int i=0; i<data.length; i++)
            kurtosis+=Math.pow(data[i]-mean, 4);
        kurtosis/=data.length*Math.pow(variance, 2);
    }

    public double getMean()
    {
        return mean;
    }

    public double getVariance()
    {
        return variance;
    }

    public double getSkewness()
    {
        return skewness;
    }

    public double getKurtosis()
    {
        return kurtosis;
    }



}
