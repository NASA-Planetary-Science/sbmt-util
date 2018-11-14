//package edu.jhuapl.sbmt.util;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
//import nom.tam.fits.FitsException;
//import altwg.tools.Maplet2FITS;
//
//import com.google.common.io.Files;
//
//import edu.jhuapl.saavtk.model.PolyhedralModel;
//import edu.jhuapl.saavtk.util.Configuration;
//import edu.jhuapl.sbmt.tools.BigmapDistributedGravity;
//
//public class Bigmap
//{
//    public static final int MAX_WIDTH = 1027;
//    public static final int MAX_HEIGHT = 1027;
//    public static final int MAX_PLANES = 6;
//
//    private ProcessBuilder processBuilder;
//    private String bigmapRootDir;
//    private String name;
//    private double latitude;
//    private double longitude;
//    private int halfSize = 512;
//    private double pixelSize;
//    private File outputFolder;
//    private File tempFolder;
//    private File mapletFitsFile;
//    private String gravityExecutableName;
//    private PolyhedralModel smallBodyModel;
//
//    public Bigmap(String bigmapRootDir, boolean grotesque) throws IOException
//    {
//        this.bigmapRootDir = bigmapRootDir;
//
//        String execDir = bigmapRootDir;
//
//        List<String> processCommand = new ArrayList<String>();
//
//        processBuilder = new ProcessBuilder(processCommand);
//
//        processBuilder.directory(new File(bigmapRootDir));
//
//        Map<String, String> env = processBuilder.environment();
//
//        String executableExtension = "";
//        if (Configuration.isLinux())
//        {
//            if (System.getProperty("sun.arch.data.model").equals("64"))
//            {
//                executableExtension = ".linux64";
//            }
//            else
//            {
//                executableExtension = ".linux32";
//            }
//
//            env.put("LD_LIBRARY_PATH", execDir);
//        }
//        else if (Configuration.isMac())
//        {
//            executableExtension = ".macosx";
//
//            env.put("DYLD_LIBRARY_PATH", execDir);
//        }
//        else
//        {
//            executableExtension = ".win32.exe";
//            //throw new IOException("Operating system not supported");
//        }
//
//        // Build up name for bigmap executable
//        String processName;
//        if(grotesque)
//        {
//            processName = execDir + File.separator + "BIGMAPOF" + executableExtension;
//        }
//        else
//        {
//            processName = execDir + File.separator + "BIGMAPO" + executableExtension;
//        }
//        new File(processName).setExecutable(true);
//        processCommand.add(processName);
//        System.out.println("Running process: " + processName);
//
//        // Build up name for gravity executable
//        gravityExecutableName = "gravity" + executableExtension;
//        new File(execDir + File.separator + gravityExecutableName).setExecutable(true);
//    }
//
//    public Process runBigmap() throws IOException, InterruptedException
//    {
//        Process process = processBuilder.start();
//        OutputStream stdin = process.getOutputStream();
//
//        /** Hardcoded values for now, check with Olivier B. on these **/
//        int seed = 1234; // (value from example in BIGMAPO.F comments)
//        double maxMapletRes = 0.2; // km (value recommended by Ray Espiritu)
//        double fractionHeights = .005; // fraction of heights for conditioning (value from example in BIGMAPO.F comments)
//        double conditioningWeight = .025; // conditioning weight (value from example in BIGMAPO.F comments)
//        int slopeIntegration = 1; // (value from example in BIGMAPO.F comments)
//        int numIterations = 6; // Not sure how many iterations is appropriate
//
//        // Also, should we be using bigmap.f, bigmapo.f, or bigmapof.f (grotesque) ???
//
//        // Create arguments for bigmap, see header comment of BIGMAPO.F for example
//        String arguments = "l" + "\n"               // specify by lat/lon
//                + latitude + ", " + longitude + "\n" // lat/lon values
//                + pixelSize/1000 + ", " + halfSize + ", " + seed + ", " + maxMapletRes + "\n" // scale, qsx, seed, max maplet res
//                + name + "\n"                       // bigmap name
//                + slopeIntegration + "\n"           // choose slope integration
//                + fractionHeights + "\n"            // fraction of heights for conditioning
//                + conditioningWeight + "\n";        // conditioning weight
//        for(int i=0; i<numIterations; i++)
//        {
//            arguments += "1" + "\n";
//        }
//        arguments += "0" + "\n"                     // stop iteration
//                + "0" + "\n";                       // no template
//
//        System.out.println(arguments);
//
//        stdin.write(arguments.getBytes());
//        stdin.flush();
//        stdin.close();
//
//        return process;
//    }
//
//    public void convertMapletToFitsAndSaveInOutputFolder(boolean deleteMaplet)
//    {
//        try
//        {
//            // Convert the maplet produced by bigmap to FITs format with 10 planes:
//            // 0. lat
//            // 1. lon
//            // 2. radius
//            // 3. x position
//            // 4. y position
//            // 5. z position
//            // 6. height
//            // 7. albedo
//            // 8. sigma
//            // 9. quality
//            File origMapletFile = new File(bigmapRootDir + File.separator + "BIGMAP" + File.separator + name + ".MAP");
//            File bigmapToFitsFile = new File(tempFolder + File.separator + name + "_m2f" + ".FIT");
//            Maplet2FITS.main(new String[] {origMapletFile.getPath(), bigmapToFitsFile.getPath()});
//
//            // Assemble options for calling BigmapDistributedGravity
//            File dgFitsFile = new File(tempFolder + File.separator + name + ".FIT");
//            File objShapeFile = new File(bigmapRootDir + File.separator + "SHAPEFILES" + File.separator + "SHAPE_LOWEST_RES.OBJ");
//            List<String> dgOptionList = new LinkedList<String>();
//            dgOptionList.add("-d");
//            dgOptionList.add(Double.toString(smallBodyModel.getDensity()));
//            dgOptionList.add("-r");
//            dgOptionList.add(Double.toString(smallBodyModel.getRotationRate()));
//            dgOptionList.add("--fits-local");
//            dgOptionList.add(bigmapToFitsFile.getPath());
//            dgOptionList.add("--ref-potential");
//            dgOptionList.add(Double.toString(smallBodyModel.getReferencePotential()));
//            dgOptionList.add("--output-folder");
//            dgOptionList.add(tempFolder.getPath());
//            dgOptionList.add("--werner");
//            dgOptionList.add(objShapeFile.getPath()); // Global shape model file, Olivier suggests lowest res .OBJ **/
//            dgOptionList.add(dgFitsFile.getPath()); // Path to output file that will contain all results
//            dgOptionList.add(bigmapRootDir);
//            dgOptionList.add(gravityExecutableName); // Version of gravity called differs by OS
//
//            // Debug output
//            System.out.println("Calling Distributed Gravity with...");
//            System.out.println("  density = " + smallBodyModel.getDensity() + " g/cm^3");
//            System.out.println("  rotation rate = " + smallBodyModel.getRotationRate() + " rad/s");
//            System.out.println("  reference potential = " + smallBodyModel.getReferencePotential() + " J/kg");
//
//            // Convert argument list to array and call BigmapDistributedGravity
//            // Resulting FITS file has original Maplet2FITS planes plus the following:
//            // Add gravity information by appending the following planes to the FITs file
//            // 10. normal vector x component
//            // 11. normal vector y component
//            // 12. normal vector z component
//            // 13. gravacc x component
//            // 14. gravacc y component
//            // 15. gravacc z component
//            // 16. magnitude of grav acc
//            // 17. grav potential
//            // 18. elevation
//            // 19. slope
//            // 20. Tilt
//            // 21. Tilt direction
//            // 22. Tilt mean
//            // 23. Tilt standard deviation
//            // 24. Distance to plane
//            // 25. Shaded relief
//            String[] dgOptionArray = dgOptionList.toArray(new String[dgOptionList.size()]);
//            BigmapDistributedGravity.main(dgOptionArray);
//            System.out.println("DistributedGravity finished processing bigmap outputs");
//
//            // Write the output FITS file
//            mapletFitsFile = new File(outputFolder + File.separator + name + ".FIT");
//            Files.copy(dgFitsFile, mapletFitsFile);
//
//            // Delete the files that are no longer needed
//            bigmapToFitsFile.delete();
//            dgFitsFile.delete();
//            if(deleteMaplet)
//            {
//                origMapletFile.delete();
//            }
//        }
//        catch(FitsException e)
//        {
//            e.printStackTrace();
//        }
//        catch(IOException e)
//        {
//            e.printStackTrace();
//        }
//        catch(Exception e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    public String getName()
//    {
//        return name;
//    }
//
//    public void setName(String name)
//    {
//        this.name = name;
//    }
//
//    public double getLatitude()
//    {
//        return latitude;
//    }
//
//    /**
//     * set the latitude in degrees
//     * @param latitude
//     */
//    public void setLatitude(double latitude)
//    {
//        this.latitude = latitude;
//    }
//
//    public double getLongitude()
//    {
//        return longitude;
//    }
//
//    /**
//     * set the longitude in degrees and as West Longitude (not east as is shown in the status bar)
//     * @param longitude
//     */
//    public void setLongitude(double longitude)
//    {
//        this.longitude = longitude;
//        this.longitude = 360.0 - this.longitude;
//        if (this.longitude < 0.0)
//            this.longitude += 360.0;
//    }
//
//    public int getHalfSize()
//    {
//        return halfSize;
//    }
//
//    public void setHalfSize(int halfSize)
//    {
//        this.halfSize = halfSize;
//    }
//
//    public double getPixelSize()
//    {
//        return pixelSize;
//    }
//
//    public void setPixelSize(double pixelSize)
//    {
//        this.pixelSize = pixelSize;
//    }
//
//    public File getMapletFile()
//    {
//        return mapletFitsFile;
//    }
//
//    public File getOutputFolder()
//    {
//        return outputFolder;
//    }
//
//    public void setOutputFolder(File outputFolder)
//    {
//        this.outputFolder = outputFolder;
//    }
//
//    public void setTempFolder(File tempFolder)
//    {
//        this.tempFolder = tempFolder;
//    }
//
//    public void setSmallBodyModel(PolyhedralModel smallBodyModel)
//    {
//        this.smallBodyModel = smallBodyModel;
//    }
//
//    public PolyhedralModel getSmallBodyModel()
//    {
//        return smallBodyModel;
//    }
//}
