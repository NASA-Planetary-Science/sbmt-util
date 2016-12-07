package edu.jhuapl.sbmt.util.nis;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class RegularHistogram2D implements Histogram2D
{

    int nx,ny;
    int[][] counts;
    double[] xEdges;
    double[] yEdges;

    public RegularHistogram2D(double[] xEdges, double[] yEdges)
    {
        this.nx=xEdges.length-1;
        this.ny=yEdges.length-1;
        this.xEdges=xEdges;
        this.yEdges=yEdges;
        counts=new int[nx][ny];
    }

    @Override
    public int getNx()
    {
        return nx;
    }

    @Override
    public int getNy()
    {
        return ny;
    }

    @Override
    public int getCount(int i, int j)
    {
        return counts[i][j];
    }

    @Override
    public void incrementCount(double xVal, double yVal) throws HistogramValueOutOfBoundsException
    {
        int i=(int)((xVal-xEdges[0])/(xEdges[nx]-xEdges[0])*(double)nx);
        int j=(int)((yVal-yEdges[0])/(yEdges[ny]-yEdges[0])*(double)ny);
        if (i>=nx || j>=ny)
            throw new HistogramValueOutOfBoundsException(xVal,yVal,i,j,nx,ny);
        counts[i][j]++;
    }

    @Override
    public Vector2D getBinCenter(int i, int j)
    {
        return new Vector2D((xEdges[i]+xEdges[i+1])/2,(yEdges[j]+yEdges[j+1])/2);
    }

    @Override
    public Vector2D getBinLowerCorner(int i, int j)
    {
        return new Vector2D(xEdges[i],yEdges[j]);
    }

    @Override
    public Vector2D getBinUpperCorner(int i, int j)
    {
        return new Vector2D(xEdges[i+1],yEdges[j+1]);
    }

    @Override
    public void add(Histogram2D h) throws HistogramDimensionMismatchException
    {
        if (h.getNx()!=nx || h.getNy()!=ny)
            throw new HistogramDimensionMismatchException(nx, ny, h.getNx(), h.getNy());
        for (int i=0; i<nx; i++)
            for (int j=0; j<ny; j++)
                counts[i][j]+=h.getCount(i, j);
    }

    public void write(Path file)
    {
        try
        {
            DataOutputStream stream=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile())));
            stream.writeInt(nx);
            stream.writeInt(ny);
            for (int i=0; i<nx+1; i++)
                stream.writeDouble(xEdges[i]);
            for (int j=0; j<ny+1; j++)
                stream.writeDouble(yEdges[j]);
            for (int i=0; i<nx; i++)
                for (int j=0; j<ny; j++)
                    stream.writeInt(counts[i][j]);
            stream.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


}
