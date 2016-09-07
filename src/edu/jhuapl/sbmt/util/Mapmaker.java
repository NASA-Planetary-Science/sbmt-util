package edu.jhuapl.sbmt.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileUtil;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.util.BufferedFile;


public class Mapmaker
{
    public static final int MAX_WIDTH = 1027;
    public static final int MAX_HEIGHT = 1027;
    public static final int MAX_PLANES = 6;

    private ProcessBuilder processBuilder;
    private String mapmakerRootDir;
    private String name;
    private double latitude;
    private double longitude;
    private int halfSize = 512;
    private double pixelSize;
    private File outputFolder;
    private File mapletFitsFile;

    public Mapmaker(String mapmakerRootDir) throws IOException
    {
        this.mapmakerRootDir = mapmakerRootDir;

        String execDir = mapmakerRootDir + File.separator + "EXECUTABLES";

        ArrayList<String> processCommand = new ArrayList<String>();

        processBuilder = new ProcessBuilder(processCommand);

        processBuilder.directory(new File(mapmakerRootDir));

        Map<String, String> env = processBuilder.environment();

        String processName = null;
        if (Configuration.isLinux())
        {
            if (System.getProperty("sun.arch.data.model").equals("64"))
                processName = execDir + File.separator + "MAPMAKERO.linux64";
            else
                processName = execDir + File.separator + "MAPMAKERO.linux32";

            env.put("LD_LIBRARY_PATH", execDir);
        }
        else if (Configuration.isMac())
        {
            processName = execDir + File.separator + "MAPMAKERO.macosx";

            env.put("DYLD_LIBRARY_PATH", execDir);
        }
        else
        {
            processName = execDir + File.separator + "MAPMAKERO.win32.exe";
            //throw new IOException("Operating system not supported");
        }

        new File(processName).setExecutable(true);
        processCommand.add(processName);
    }

    public Process runMapmaker() throws IOException, InterruptedException
    {
        Process process = processBuilder.start();
        OutputStream stdin = process.getOutputStream();

        String arguments = name + "\n" + halfSize + " " + pixelSize + "\nL\n" + latitude + "," + longitude + "\nn\nn\nn\nn\nn\nn\n";
        stdin.write(arguments.getBytes());
        stdin.flush();
        stdin.close();

        return process;
    }

    public void convertCubeToFitsAndSaveInOutputFolder(boolean deleteCub)
    {
        try
        {
            File origCubeFile = new File(mapmakerRootDir + File.separator + "OUTPUT" + File.separator + name + ".cub");

            FileInputStream fs = new FileInputStream(origCubeFile);
            BufferedInputStream bs = new BufferedInputStream(fs);
            DataInputStream in = new DataInputStream(bs);

            int liveSize = 2 * halfSize + 1;
            int startPixel = (MAX_HEIGHT - liveSize) / 2;

            float[] indata = new float[MAX_WIDTH*MAX_HEIGHT*MAX_PLANES];
            for (int i=0;i<indata.length; ++i)
            {
                indata[i] = FileUtil.readFloatAndSwap(in);
            }

            float[][][] outdata = new float[MAX_PLANES][liveSize][liveSize];

            int endPixel = startPixel + liveSize - 1;
            for (int p=0; p<MAX_PLANES; ++p)
                for (int m=0; m<MAX_HEIGHT; ++m)
                    for (int n=0; n<MAX_WIDTH; ++n)
                    {
                        if (m >= startPixel && m <= endPixel && n >= startPixel && n <= endPixel)
                        {
                            outdata[p][m-startPixel][n-startPixel] = indata[index(n,m,p)];
                        }
                    }

            in.close();


            mapletFitsFile = new File(outputFolder + File.separator + name + ".FIT");

            Fits f = new Fits();
            BasicHDU hdu = FitsFactory.HDUFactory(outdata);

            hdu.getHeader().addValue("PLANE1", "Elevation Relative to Gravity (kilometers)", null);
            hdu.getHeader().addValue("PLANE2", "Elevation Relative to Normal Plane (kilometers)", null);
            hdu.getHeader().addValue("PLANE3", "Slope (radians)", null);
            hdu.getHeader().addValue("PLANE4", "X coordinate of maplet vertices (kilometers)", null);
            hdu.getHeader().addValue("PLANE5", "Y coordinate of maplet vertices (kilometers)", null);
            hdu.getHeader().addValue("PLANE6", "Z coordinate of maplet vertices (kilometers)", null);
            hdu.getHeader().addValue("HALFSIZE", halfSize, "Half Size (pixels)");
            hdu.getHeader().addValue("SCALE", pixelSize, "Horizontal Scale (meters per pixel)");
            hdu.getHeader().addValue("LATITUDE", latitude, "Latitude of Maplet Center (degrees)");
            hdu.getHeader().addValue("LONGTUDE", longitude, "Longitude of Maplet Center (degrees)");

            f.addHDU(hdu);
            BufferedFile bf = new BufferedFile(mapletFitsFile, "rw");
            f.write(bf);
            bf.close();

            if (deleteCub)
            {
                origCubeFile.delete();
                File origLblFile = new File(mapmakerRootDir + File.separator + "OUTPUT" + File.separator + name + ".lbl");
                origLblFile.delete();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (FitsException e)
        {
            e.printStackTrace();
        }
    }

    public static int index(int i, int j, int k)
    {
        return ((k * MAX_HEIGHT + j) * MAX_WIDTH + i);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public double getLatitude()
    {
        return latitude;
    }

    /**
     * set the latitude in degrees
     * @param latitude
     */
    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    /**
     * set the longitude in degrees and as West Longitude (not east as is shown in the status bar)
     * @param longitude
     */
    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
        this.longitude = 360.0 - this.longitude;
        if (this.longitude < 0.0)
            this.longitude += 360.0;
    }

    public int getHalfSize()
    {
        return halfSize;
    }

    public void setHalfSize(int halfSize)
    {
        this.halfSize = halfSize;
    }

    public double getPixelSize()
    {
        return pixelSize;
    }

    public void setPixelSize(double pixelSize)
    {
        this.pixelSize = pixelSize;
    }

    public File getMapletFile()
    {
        return mapletFitsFile;
    }

    public File getOutputFolder()
    {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder)
    {
        this.outputFolder = outputFolder;
    }

}
