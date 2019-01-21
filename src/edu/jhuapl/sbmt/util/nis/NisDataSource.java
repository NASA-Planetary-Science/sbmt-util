package edu.jhuapl.sbmt.util.nis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.gui.eros.NISSearchPanel;
import edu.jhuapl.sbmt.model.eros.NISSpectrum;

public class NisDataSource
{
    Path basePath;
    BiMap<NisTime, String> timeToFileMap=HashBiMap.create();
    String[] directories;
    List<String>[] directoryToFileLists;

    SmallBodyModel erosModel;

    static
    {
        NativeLibraryLoader.loadVtkLibraries();
        SmallBodyViewConfig.initialize();
    };

    public NisDataSource()
    {
        this(Paths.get("/Users/zimmemi1/sbmt/NIS/2000/"));
    }

    public NisDataSource(Path basePath)
    {
        this.basePath=basePath;
        System.out.print("Preparing data source... ");
        readNisTimes();
        gatherNisDirectories();
        SmallBodyViewConfig config=SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EROS, ShapeModelType.GASKELL);
        erosModel = SbmtModelFactory.createSmallBodyModel(config);
        System.out.println("Done.");
    }

    public void setResolutionLevel(int lev)
    {
        try
        {
            erosModel.setModelResolution(lev);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int getResolutionLevel()
    {
        return erosModel.getModelResolution();
    }

    public ISmallBodyModel getSmallBodyModel()
    {
        return erosModel;
    }

    public Set<NisTime> getAllObservationTimes()
    {
        return timeToFileMap.keySet();
    }

    public NISSpectrum getSpectrum(NisTime time)
    {
        Path filePath=basePath.resolve(timeToFileMap.get(time));
        NISSpectrum spectrum=null;
        try
        {
            spectrum=new LocalNISSpectrum(filePath.toFile(), erosModel);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return spectrum;
    }

    public class SpectralInfo
    {
        Vector3D toSun;
        Vector3D toSpacecraft;
        double irradianceFactor;
        double weight;
    }

/*    public Map<Integer, SpectralInfo> getFaceCoverage(NISSpectrum spectrum)
    {
        Map<Integer, Double> areaFractions=Maps.newHashMap();
        Map<Integer, Vector3D> toSpacecraft=Maps.newHashMap();
        Map<Integer, Double> irradianceFactors=Maps.newHashMap();
        spectrum.generateFootprint();
        vtkPolyData footprint=spectrum.getUnshiftedFootprint();
        //
        for (int c=0; c<footprint.GetNumberOfCells(); c++)
        {
            vtkIdTypeArray originalIds=(vtkIdTypeArray)footprint.GetCellData().GetArray(GenericPolyhedralModel.cellIdsArrayName);
            int originalId=originalIds.GetValue(c);
            vtkTriangle tri=(vtkTriangle)erosModel.getSmallBodyPolyData().GetCell(originalId);  // tri on original body model
            vtkTriangle ftri=(vtkTriangle)footprint.GetCell(c); // tri on footprint
            Vector3D spacecraftPosition=new Vector3D(spectrum.getSpacecraftPosition());
            double[] ftriCenter=new double[3];
            tri.TriangleCenter(ftri.GetPoints().GetPoint(0), ftri.GetPoints().GetPoint(1), ftri.GetPoints().GetPoint(2), ftriCenter);
            if (areaFractions.containsKey(originalId))
            {
                double oldFraction=areaFractions.get(originalId);
                areaFractions.put(originalId, ftri.ComputeArea()/tri.ComputeArea()+oldFraction);

            }
            else
            {
                areaFractions.put(originalId, ftri.ComputeArea()/tri.ComputeArea());
                toSpacecraft.put(originalId,spacecraftPosition.subtract(new Vector3D(ftriCenter)));
            }
        }
    }*/

    public Vector3D getToSunVector(NisTime time)
    {
        return NISSearchPanel.getToSunUnitVector(timeToFileMap.get(time));
    }

    public String[] getAllDirectories()
    {
        return directories;
    }

    public List<String> getAllFilesInDirectory(String dir)
    {
        int idx=-1;
        for (int i=0; i<directories.length && idx==-1; i++)
            if (directories[i].equals(dir))
                idx=i;
        return directoryToFileLists[idx];
    }

    public Set<NisTime> getAllTimesInDirectory(String dir)
    {
        int idx=-1;
        for (int i=0; i<directories.length && idx==-1; i++)
            if (directories[i].equals(dir))
                idx=i;
        Set<NisTime> times=Sets.newHashSet();
        for (NisTime time : timeToFileMap.keySet())
        {
            if (timeToFileMap.get(time).startsWith(dir))
                times.add(time);
        }
        return times;
    }

    private Path getNisTimesFile()
    {
        return basePath.resolve("nisTimes.txt");
    }

    private void readNisTimes()
    {
        try
        {
            Scanner scanner = new Scanner(getNisTimesFile().toFile());
            while (scanner.hasNext())
            {
                String imageString=scanner.next();
                String timeString=scanner.next();
                timeToFileMap.put(new NisTime(timeString), imageString);
            }
            scanner.close();
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void gatherNisDirectories()
    {
        directories=basePath.toFile().list(new FilenameFilter()
        {

            @Override
            public boolean accept(File dir, String name)
            {
                return name.length()==3;
            }
        });
        directoryToFileLists=new List[directories.length];
        for (int i=0; i<directories.length; i++)
            directoryToFileLists[i]=Lists.newArrayList();
        for (int i=0; i<directories.length; i++)
        {
            for (String file : timeToFileMap.values())
                if (file.substring(0, 3).equals(directories[i]))
                    directoryToFileLists[i].add(file);
        }
    }


}
