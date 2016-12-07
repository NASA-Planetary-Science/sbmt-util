package edu.jhuapl.sbmt.util.nis;

import edu.jhuapl.sbmt.util.TimeUtil;

public class NisTime
{
    String timeString;
    double timeEt;

    public NisTime(String timeString)
    {
        this.timeString=timeString;
        timeEt=TimeUtil.str2et(timeString);
    }

    @Override
    public String toString()
    {
        return timeString;
    }

    public double toEt()
    {
        return timeEt;
    }
}
