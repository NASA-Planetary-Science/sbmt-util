package edu.jhuapl.sbmt.util;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import edu.jhuapl.saavtk.gui.FileDownloadSwingWorker;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;

public class MapmakerRemoteSwingWorker extends FileDownloadSwingWorker
{

    public static void main(String[] args)
    {
        MapmakerRemoteSwingWorker remote = new MapmakerRemoteSwingWorker(null, "Test", "TestFile.fits");
        remote.setName("MyTestFile");
        remote.setHalfSize(512);
        remote.setPixelScale(4);
        remote.setLatitude(32);
        remote.setLongitude(-230);
        remote.setDatadir("DATA");
        remote.setMapoutdir("MAPFILES");
        remote.setCacheDir("/Users/steelrj1/Desktop/");
        remote.run();
    }

    boolean regionSpecifiedWithLatLonScale = true;
    private String name;
    private double[] centerPoint;
    private double radius;
    private File outputFolder;
    private File mapletFile;
    private int halfSize;
    private double pixelScale;
    private double latitude;
    private double longitude;
    private String datadir;
    private String mapoutdir;
    private String cacheDir;
    private String lowResModelPath;
    private double density;
    private double rotationRate;
    private double referencePotential;
    private String bodyLowestResModelName;

    public MapmakerRemoteSwingWorker(Component c, String title, String filename)
    {
        super(filename, c, title, "<html>Running Mapmaker<br> </html>", true);
		try
		{
			System.out.println("MapMakerRemoteSwingWorker: runMapmaker: jar URI " + SBMTDistributedGravity.getJarURI());
			System.out.println("MapmakerRemoteSwingWorker: MapmakerRemoteSwingWorker: " + (new File(SBMTDistributedGravity.getJarURI().getPath()).getParent() + File.separator + "near.jar"));
		} catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }


    public void setName(String name)
    {
        this.name = name;
    }


    public void setCenterPoint(double[] centerPoint)
    {
        this.centerPoint = centerPoint;
    }


    public void setRadius(double radius)
    {
        this.radius = radius;
    }

    public void setHalfSize(int halfSize)
    {
        this.halfSize = halfSize;
    }


    public void setOutputFolder(File outputFolder)
    {
        this.outputFolder = outputFolder;
    }

    public void setPixelScale(double pixelScale)
    {
        this.pixelScale = pixelScale;
    }


    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }


    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
    }


    public File getMapletFile()
    {
        return mapletFile;
    }


    public void setDatadir(String datadir)
    {
        this.datadir = datadir;
    }


    public void setMapoutdir(String mapoutdir)
    {
        this.mapoutdir = mapoutdir;
    }


    public void setCacheDir(String cacheDir)
    {
        this.cacheDir = cacheDir;
    }

    public void setLowResModelPath(String path)
    {
        this.lowResModelPath = path;
    }


    public void setRegionSpecifiedWithLatLonScale(
            boolean regionSpecifiedWithLatLonScale)
    {
        this.regionSpecifiedWithLatLonScale = regionSpecifiedWithLatLonScale;
    }

    @Override
    public boolean getIfNeedToDownload()
    {
        return false;
    }

    @Override
    protected Void doInBackground() throws InterruptedException
    {
        checkNotCanceled("About to run mapmaker");

        setProgress(1);

        try
        {
//            File file = FileCache.getFileFromServer(this.getFileDownloaded());
//            String mapmakerRootDir = file.getParent() + File.separator + "mapmaker";

            MapMakerRemote mapmaker = new MapMakerRemote();
            mapmaker.setRotationRate(rotationRate);
            mapmaker.setReferencePotential(referencePotential);
            mapmaker.setDensity(density);
            mapmaker.setName(name);
            mapmaker.setBodyLowestResModelName(bodyLowestResModelName);
            if (regionSpecifiedWithLatLonScale)
            {
                mapmaker.setLatitude(latitude);
                mapmaker.setLongitude(longitude);
                mapmaker.setPixelSize(pixelScale);
            }
            else
            {

                LatLon ll = MathUtil.reclat(centerPoint).toDegrees();
                mapmaker.setLatitude(ll.lat);
                mapmaker.setLongitude(ll.lon);
                mapmaker.setPixelSize(1000.0 * 1.5 * radius / (double)halfSize);
            }
            mapmaker.setHalfSize(halfSize);
            mapmaker.setOutputFolder(outputFolder);
            mapmaker.setDatadir(datadir);
            mapmaker.setMapoutdir(mapoutdir);
            mapmaker.setCacheDir(cacheDir);
            mapmaker.setLowResModelPath(lowResModelPath);
            mapmaker.runMapmaker(this);
            mapletFile = mapmaker.getMapletFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setProgress(100);

        return null;
    }




    public void setDensity(double density)
    {
        this.density = density;
    }


    public void setReferencePotential(double referencePotential)
    {
        this.referencePotential = referencePotential;
    }


    public double getDensity()
    {
        return density;
    }


    public void setRotationRate(double rotationRate)
    {
        this.rotationRate = rotationRate;
    }


    public void setBodyLowestResModelName(String bodyLowestResModelName)
    {
        this.bodyLowestResModelName = bodyLowestResModelName;
    }
}
