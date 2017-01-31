package edu.jhuapl.sbmt.util.nis;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.base.Stopwatch;

import vtk.vtkDoubleArray;
import vtk.vtkIntArray;
import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

import edu.jhuapl.saavtk.util.LinearSpace;

public class NisFaceStatistics
{

/*    static Path outputDirectory;

    public static Path getFaceFile(int i)
    {
        return outputDirectory.resolve("face_"+i+".dat");
    }*/





    public static void main(String[] args) throws IOException
    {
        int resolutionLevel=0;
        NisDataSource data=new NisDataSource(Paths.get("/Volumes/freeman1.8TB/sbmt/NIS/2000"));
        data.setResolutionLevel(resolutionLevel);
        String[] subDirs=new String[]{"011","019","020","025","029","031","032","036","037","038","041","042","043","044","045","046","047","048","049","050","051","052","053","054","055","056","057","058","059","063","065","066","070","074","075","076","077","079","080","081","086","088","089","090","091","092","093","095","096","097","098","099","100","101","102","103","104","105","106","107","108","111","112","113","114","115","116","117","118","119","120","121","122","123","124","125","126","127","128","129","130","131","132","133","134"};

/*        vtkPolyData polyData=data.getSmallBodyModel().getSmallBodyPolyData();
        outputDirectory=data.basePath.resolve("faceData/res"+resolutionLevel);
        outputDirectory.toFile().mkdirs();
        for (int i=0; i<polyData.GetNumberOfCells(); i++)
        {
            File file=getFaceFile(i).toFile();
            if (file.exists())
                file.delete();
            DataOutputStream stream=new DataOutputStream(new FileOutputStream(getFaceFile(i).toFile()));
            stream.writeInt(0); // this will need to be replaced with # of tuples later
            stream.writeInt(NisSample.spectrumLength);
            stream.close();
        }*/

        vtkPolyData polyData=data.getSmallBodyModel().getSmallBodyPolyData();

        int nBins=18;
        double[] phaseAngleBinEdges=LinearSpace.create(0, 180, nBins+1);
        int[][] counts=new int[polyData.GetNumberOfCells()][nBins];
        double[][] spectra=new double[polyData.GetNumberOfCells()][nBins];

        int spectrumComponent=1;

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
                //histogram.add(iterator.next());
                NisSample sample=iterator.next();
                double phaseAngle=Math.toDegrees(Vector3D.angle(sample.toSpacecraft, sample.toSun));
                double planeAngle=Math.toDegrees(Vector3D.angle(new Vector3D(0.5,sample.toSpacecraft,0.5,sample.toSun), sample.normal));
/*                DataOutputStream stream=new DataOutputStream(new FileOutputStream(getFaceFile(sample.faceId).toFile(),true));
                stream.writeDouble(sample.time.toEt());
                stream.writeDouble(phaseAngle);
                stream.writeDouble(planeAngle);
                for (int j=0; j<NisSample.spectrumLength; j++)
                    stream.writeDouble(sample.spectrum[j]);
                stream.close();*/
                //
                int faceId=sample.faceId;
                int phaseBin=(int)((phaseAngle-phaseAngleBinEdges[0])/(phaseAngleBinEdges[nBins]-phaseAngleBinEdges[0])*(double)nBins);
                counts[faceId][phaseBin]++;
                spectra[faceId][phaseBin]+=sample.spectrum[spectrumComponent];
                cnt++;
            }
            System.out.println("Done.");
        }

        for (int i=0; i<polyData.GetNumberOfCells(); i++)
        {
            for (int j=0; j<nBins; j++)
            {
                double norm=Math.max(1, counts[i][j]);
                spectra[i][j]/=norm;
            }
        }


        while (polyData.GetCellData().GetNumberOfArrays()>0)
            polyData.GetCellData().RemoveArray(0);
        while (polyData.GetPointData().GetNumberOfArrays()>0)
            polyData.GetPointData().RemoveArray(0);

        vtkDoubleArray spectrumArray=new vtkDoubleArray();
        spectrumArray.SetName("Spectrum "+spectrumComponent);
        spectrumArray.SetNumberOfComponents(nBins);
        spectrumArray.SetNumberOfTuples(polyData.GetNumberOfCells());
        for (int i=0; i<polyData.GetNumberOfCells(); i++)
            for (int j=0; j<nBins; j++)
                spectrumArray.SetComponent(i, j, spectra[i][j]);

        vtkIntArray countArray=new vtkIntArray();
        countArray.SetName("Counts "+spectrumComponent);
        countArray.SetNumberOfComponents(nBins);
        countArray.SetNumberOfTuples(polyData.GetNumberOfCells());
        for (int i=0; i<polyData.GetNumberOfCells(); i++)
            for (int j=0; j<nBins; j++)
                countArray.SetComponent(i, j, counts[i][j]);

        polyData.GetCellData().AddArray(spectrumArray);
        polyData.GetCellData().AddArray(countArray);

        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName("/Users/zimmemi1/Desktop/eros_nis_channel_"+spectrumComponent+".vtk");
        writer.SetFileTypeToBinary();
        writer.SetInputData(polyData);
        writer.Write();
    }
}
