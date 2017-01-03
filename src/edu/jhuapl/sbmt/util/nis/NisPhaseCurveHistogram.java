package edu.jhuapl.sbmt.util.nis;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.base.Stopwatch;

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
        NisDataSource data=new NisDataSource(Paths.get("/Volumes/freeman1.8TB/sbmt/NIS/2000"));
        data.setResolutionLevel(resolutionLevel);

        String[] subDirs=new String[]{"011","019","020","025","029","031","032","036","037","038","041","042","043","044","045","046","047","048","049","050","051","052","053","054","055","056","057","058","059","063","065","066","070","074","075","076","077","079","080","081","086","088","089","090","091","092","093","095","096","097","098","099","100","101","102","103","104","105","106","107","108","111","112","113","114","115","116","117","118","119","120","121","122","123","124","125","126","127","128","129","130","131","132","133","134"};
        NisPhaseCurveHistogram histogram=new NisPhaseCurveHistogram(720, 400, 50);
        for (int i=0; i<subDirs.length; i++)
        {
            Path filePath=data.basePath.resolve(subDirs[i]).resolve("faceSamples."+resolutionLevel+".dat");
            System.out.print("Processing "+filePath+"... ");
            NisSampleFile file=new NisSampleFile(filePath);
            Iterator<NisSample> iterator=file.iterator();
            Stopwatch sw=new Stopwatch();
            sw.start();
            int cnt=0;
            while (iterator.hasNext())
            {
                if (sw.elapsedMillis()>4000)
                {
                    System.out.println(" "+(cnt+1)+"/"+file.nSamples);
                    sw.reset();
                    sw.start();
                }
                histogram.add(iterator.next());
                cnt++;
            }
            System.out.println("Done.");
        }

        Path histogramFile=Paths.get("/Users/zimmemi1/Desktop/phase_angle_histogram.dat");
        System.out.print("Writing to "+histogramFile+"... ");
        histogram.write(histogramFile);
        System.out.println("Done.");
    }

}
