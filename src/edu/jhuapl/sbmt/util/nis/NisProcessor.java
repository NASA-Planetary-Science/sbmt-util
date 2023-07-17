package edu.jhuapl.sbmt.util.nis;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import vtk.vtkIdList;
import vtk.vtkIdTypeArray;
import vtk.vtkOBBTree;
import vtk.vtkObject;
import vtk.vtkPolyData;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.sbmt.core.body.ISmallBodyModel;
import edu.jhuapl.sbmt.model.eros.nis.NISSpectrum;
import edu.jhuapl.sbmt.spectrum.rendering.BasicSpectrumRenderer;

public class NisProcessor implements Runnable
{
    NisDataSource data;
    int spectraToProcess;
    List<NisSample>[] faceSamples;
    vtkOBBTree tree;
    String directory;
    ISmallBodyModel smallBodyModel;

    public NisProcessor(NisDataSource data, String directory, ISmallBodyModel model)
    {
        this.data=data;
        this.smallBodyModel = model;
        faceSamples=new List[(int)data.erosModel.getSmallBodyPolyData().GetNumberOfCells()];
        for (int i=0; i<faceSamples.length; i++)
            faceSamples[i]=Lists.newArrayList();
        //
        tree=new vtkOBBTree();
        tree.SetDataSet(data.erosModel.getSmallBodyPolyData());
        tree.SetTolerance(1e-12);
        tree.Update();
        //
        this.directory=directory;
        spectraToProcess=data.getAllTimesInDirectory(directory).size();
    }

    public void limitNumberOfSpectraToProcess(int i)
    {
        spectraToProcess=i;
    }

    @Override
    public void run()
    {
        int spectrumCount=0;
        Iterator<NisTime> timeIterator=data.getAllTimesInDirectory(directory).iterator();
        //
        Stopwatch sw = Stopwatch.createUnstarted();
        sw.start();
        while (timeIterator.hasNext() && spectrumCount<spectraToProcess)
        {
            NisTime time=timeIterator.next();
            NISSpectrum spectrum=data.getSpectrum(time);
            BasicSpectrumRenderer spectrumRenderer = new BasicSpectrumRenderer(spectrum, smallBodyModel, false);
            spectrumRenderer.generateFootprint();
            vtkPolyData footprint=spectrumRenderer.getUnshiftedFootprint();
            //
            Vector3D toSun=data.getToSunVector(time).normalize();               // normalized
            Vector3D spacecraftPosition=new Vector3D(spectrum.getSpacecraftPosition());
            //
            double[] spectralIntensity=Arrays.copyOf(spectrum.getSpectrum(),spectrum.getSpectrum().length);
            for (int c=0; c<footprint.GetNumberOfCells(); c++)
            {
                if (sw.elapsed(TimeUnit.MILLISECONDS)>20000)
                {
                    System.out.println("Spectrum "+spectrumCount+"/"+spectraToProcess+"  Face "+(c+1)+"/"+footprint.GetNumberOfCells());
             //       printBins(360, Paths.get("/Users/zimmemi1/Desktop/test.dat"));
                    vtkObject.JAVA_OBJECT_MANAGER.gc(false); // this is necessary to clear VTK objects
                    System.gc();
                    sw.reset();
                    sw.start();
                }
                //
                vtkIdTypeArray originalIds=(vtkIdTypeArray)footprint.GetCellData().GetArray(GenericPolyhedralModel.cellIdsArrayName);
                int originalId=(int)originalIds.GetValue(c);
                vtkTriangle tri=(vtkTriangle)data.erosModel.getSmallBodyPolyData().GetCell(originalId);  // tri on original body model
                vtkTriangle ftri=(vtkTriangle)footprint.GetCell(c); // tri on footprint
                double[] ftriCenter=new double[3];
                double[] ftriNormal=new double[3];
                tri.TriangleCenter(ftri.GetPoints().GetPoint(0), ftri.GetPoints().GetPoint(1), ftri.GetPoints().GetPoint(2), ftriCenter);
                tri.ComputeNormal(ftri.GetPoints().GetPoint(0), ftri.GetPoints().GetPoint(1), ftri.GetPoints().GetPoint(2), ftriNormal);
                //
                Vector3D normal=new Vector3D(ftriNormal).normalize();   // normalized
                if (normal.dotProduct(toSun)<0)
                    continue;
                //
                Vector3D ftriCenterVector=new Vector3D(ftriCenter);
                double raylength=data.erosModel.getBoundingBoxDiagonalLength();
                vtkIdList ids=new vtkIdList();
                tree.IntersectWithLine(ftriCenter, toSun.scalarMultiply(raylength).toArray(), null, ids);
                boolean go=true;
                for (int i=0; i<ids.GetNumberOfIds() && go; i++)
                    if (ids.GetId(i)!=originalId)
                        go=false;
                if (!go)
                    continue;
                Vector3D toSpacecraft=spacecraftPosition.subtract(ftriCenterVector).normalize();    // normalized
                faceSamples[originalId].add(new NisSample(originalId, time, normal, toSun, toSpacecraft, spectralIntensity, ftri.ComputeArea()/tri.ComputeArea()));
            }
            //
            spectrumCount++;
        }
    }

/*    public void writeToVtk(Path vtkFile)
    {
        vtkPolyData polyData=new vtkPolyData();
        polyData.DeepCopy(data.erosModel.getSmallBodyPolyData());
        vtkDoubleArray toSpacecraftCosineArray=new vtkDoubleArray();
        vtkDoubleArray toSunCosineArray=new vtkDoubleArray();
        vtkDoubleArray intensityArray=new vtkDoubleArray();
        toSpacecraftCosineArray.SetName("sc dot nml");
        toSunCosineArray.SetName("sun dot nml");
        intensityArray.SetName("intensity");
        polyData.GetCellData().AddArray(toSpacecraftCosineArray);
        polyData.GetCellData().AddArray(toSunCosineArray);
        polyData.GetCellData().AddArray(intensityArray);
        //
        for (int c=0; c<polyData.GetNumberOfCells(); c++)
        {
            double toSpacecraftCosine=0;
            double toSunCosine=0;
            double intensity=0;
            double totalWeight=0;
            for (int i=0; i<faceData[c].getNumberOfObservations(); i++)
            {
                Observation o=faceData[c].getObservation(i);
                toSpacecraftCosine+=o.weight*o.toSpacecraft.normalize().dotProduct(o.normal);
                toSunCosine+=o.weight*o.toSun.normalize().dotProduct(o.normal);
                intensity+=o.weight*o.intensity;
                totalWeight+=o.weight;
            }
            if (totalWeight>0)
            {
                toSpacecraftCosineArray.InsertNextValue(toSpacecraftCosine/totalWeight);
                toSunCosineArray.InsertNextValue(toSunCosine/totalWeight);
                intensityArray.InsertNextValue(intensity/totalWeight);
            }
            else
            {
                toSpacecraftCosineArray.InsertNextValue(0);
                toSunCosineArray.InsertNextValue(0);
                intensityArray.InsertNextValue(0);
            }
        }
        //
        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName(vtkFile.toString());
        writer.SetFileTypeToBinary();
        writer.SetInputData(polyData);
        writer.Write();
    }

    public void printBins(int nBins, Path file)
    {
        double[] phaseCenters=new double[nBins];
        double[] planeCenters=new double[nBins];
        double[] phaseEdges=LinearSpace.create(0, 180, nBins+1);
        double[] planeEdges=LinearSpace.create(0, 90, nBins+1);
        for (int i=0; i<nBins; i++)
        {
            phaseCenters[i]=(phaseEdges[i]+phaseEdges[i+1])/2.;
            planeCenters[i]=(planeEdges[i]+planeEdges[i+1])/2.;
        }
        //
        try
        {
            FileWriter writer=new FileWriter(file.toFile());
            writer.write(nBins+System.lineSeparator());
            for (int i=0; i<nBins; i++)
            {
                for (int j=0; j<nBins; j++)
                    writer.write(phaseCenters[i]+" ");
                writer.write(System.lineSeparator());
            }
            for (int i=0; i<nBins; i++)
            {
                for (int j=0; j<nBins; j++)
                    writer.write(planeCenters[j]+" ");
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void printBinnedData(int nBins, Path file)
    {
        System.out.println("Writing to "+file);

        vtkPolyData polyData=data.erosModel.getSmallBodyPolyData();
        double[][] counts=new double[nBins][nBins];
        double[][] intensity=new double[nBins][nBins];
        double[] phaseCenters=new double[nBins];
        double[] planeCenters=new double[nBins];
        double[] phaseEdges=LinearSpace.create(0, 180, nBins+1);
        double[] planeEdges=LinearSpace.create(0, 90, nBins+1);
        for (int i=0; i<nBins; i++)
        {
            phaseCenters[i]=(phaseEdges[i]+phaseEdges[i+1])/2.;
            planeCenters[i]=(planeEdges[i]+planeEdges[i+1])/2.;
        }
        for (int c=0; c<polyData.GetNumberOfCells(); c++)
        {
            for (int i=0; i<faceData[c].getNumberOfObservations(); i++)
            {
                Observation o=faceData[c].getObservation(i);
                double phaseAngle=Math.toDegrees(Vector3D.angle(o.toSpacecraft, o.toSun));
                double planeAngle=Math.toDegrees(Vector3D.angle(new Vector3D(0.5,o.toSpacecraft,0.5,o.toSun), o.normal));
                int m=Arrays.binarySearch(phaseEdges, phaseAngle);  // -insertionPoint-1
                int n=Arrays.binarySearch(planeEdges, planeAngle);
                m=m<0?-m-1:m;
                n=n<0?-n-1:n;
                if (m>=nBins || n>=nBins)
                    continue;
                counts[m][n]+=o.weight;
                intensity[m][n]+=o.intensity*o.weight;
            }
        }
        for (int i=0; i<nBins; i++)
            for (int j=0; j<nBins; j++)
                intensity[i][j]/=counts[i][j]>0?counts[i][j]:1;
        try
        {
            FileWriter writer=new FileWriter(file.toFile());
            writer.write(nBins+System.lineSeparator());
            for (int i=0; i<nBins; i++)
            {
                for (int j=0; j<nBins; j++)
                    writer.write(counts[i][j]+" ");
                writer.write(System.lineSeparator());
            }
            for (int i=0; i<nBins; i++)
            {
                for (int j=0; j<nBins; j++)
                    writer.write(intensity[i][j]+" ");
                writer.write(System.lineSeparator());
            }
            writer.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
*/

    public void writeSamples(Path file)
    {
        NisSampleFile.write(file, faceSamples[0]);
        for (int i=1; i<faceSamples.length; i++)
            NisSampleFile.append(file, faceSamples[i]);
    }

}
