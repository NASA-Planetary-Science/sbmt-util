package edu.jhuapl.sbmt.util.nis;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.LinearSpace;

public class NisPhaseCurveHistogram extends RegularHistogram2D
{

    public NisPhaseCurveHistogram(int nPhaseBins, int nIntensityBins, double minIntensity, double maxIntensity)
    {
        super(LinearSpace.create(0, 180, nPhaseBins),LinearSpace.create(minIntensity, maxIntensity, nIntensityBins));
    }

    public NisPhaseCurveHistogram(int nPhaseBins, int nIntensityBins, double maxIntensity)
    {
        this(nPhaseBins,nIntensityBins,0,maxIntensity);
    }

    public void add(List<NisSample> samples)
    {
        for (int i=0; i<samples.size(); i++)
            add(samples.get(i));
    }

    public void add(NisSample sample)
    {
        try
        {
            double phaseAngle=Math.toDegrees(Vector3D.angle(sample.toSpacecraft, sample.toSun));
            double intensity=0;
            for (int m=0; m<sample.spectrumLength; m++)
                intensity+=sample.spectrum[m];
            incrementCount(phaseAngle,intensity);
        } catch (HistogramValueOutOfBoundsException e)
        {
            e.printStackTrace();
        }

    }

    public static void main(String[] args)
    {
        int resolutionLevel=0;
        NisDataSource data=new NisDataSource();
        data.setResolutionLevel(resolutionLevel);

        String[] subDirs=new String[]{"031","032"};
        NisPhaseCurveHistogram histogram=new NisPhaseCurveHistogram(360, 180,10);
        for (int i=0; i<subDirs.length; i++)
        {
            Path filePath=data.basePath.resolve(subDirs[i]).resolve("faceSamples."+resolutionLevel+".dat");
            System.out.print("Processing "+filePath+"... ");
            NisSampleFile file=new NisSampleFile(filePath);
            Iterator<NisSample> iterator=file.iterator();
            while (iterator.hasNext())
                histogram.add(iterator.next());
            System.out.println("Done.");
        }

        Path histogramFile=Paths.get("/Users/zimmemi1/Desktop/histogram.dat");
        System.out.print("Writing to "+histogramFile+"... ");
        histogram.write(histogramFile);
        System.out.println("Done.");
    }

}
