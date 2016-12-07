package edu.jhuapl.sbmt.util.nis;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface Histogram2D
{
    public int getNx();
    public int getNy();
    public int getCount(int i, int j);
    public void incrementCount(double xVal, double yVal) throws HistogramValueOutOfBoundsException;
    public Vector2D getBinCenter(int i, int j);
    public Vector2D getBinLowerCorner(int i, int j);
    public Vector2D getBinUpperCorner(int i, int j);
    public void add(Histogram2D h) throws HistogramDimensionMismatchException;

    public static class HistogramValueOutOfBoundsException extends Exception
    {
        double xval,yval;
        int i,j,nx,ny;

        public HistogramValueOutOfBoundsException(double xval, double yval, int i, int j, int nx, int ny)
        {
            this.xval=xval;
            this.yval=yval;
            this.i=i;
            this.j=j;
            this.nx=nx;
            this.ny=ny;
        }

        @Override
        public String getMessage()
        {
            return super.getMessage()+": (xVal,yVal)=("+xval+","+yval+") (i,j)=("+i+","+j+")"+" (nx,ny)=("+nx+","+ny+")";
        }
    };

    public static class HistogramDimensionMismatchException extends Exception
    {
        int iThis,jThis;
        int iThat,jThat;

        public HistogramDimensionMismatchException(int iThis, int jThis, int iThat, int jThat)
        {
            this.iThis=iThis;
            this.jThis=jThis;
            this.iThat=iThat;
            this.jThat=jThat;
        }

         @Override
        public String getMessage()
        {
            return super.getMessage()+": (i,j)=("+iThis+","+jThis+") (u,v)=("+iThat+","+jThat+")";
        }
    }
}
