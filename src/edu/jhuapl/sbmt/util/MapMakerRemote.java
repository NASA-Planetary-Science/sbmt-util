package edu.jhuapl.sbmt.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;


public class MapMakerRemote
{

    private String name;
    private double latitude;
    private double longitude;
    private int halfSize = 512;
    private double pixelSize;
    private File outputFolder;
    private File mapletFitsFile;
    private String datadir;
    private String cacheDir;

    public void setDatadir(String datadir)
    {
        this.datadir = datadir;
    }

    public void setMapoutdir(String mapoutdir)
    {
        this.mapoutdir = mapoutdir;
    }


    private String mapoutdir;

    public static void main(String[] args) throws IOException
    {
        MapMakerRemote remote = new MapMakerRemote();

        try
        {


            remote.runMapmaker();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public MapMakerRemote() throws IOException
    {


    }


    public void runMapmaker() throws Exception
    {
        System.out.println("MapMakerRemote: runMapmaker: running mapmaker");

        Object[] options = {"Just run Mapmaker",
                "Run Distributed Gravity"};
        int n = JOptionPane.showOptionDialog(null,
        "This will run mapmaker remotely and return a FITS file; do you want to also run Distributed Gravity locally?",
        "Run Distributed Gravity?",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        options,
        options[0]);


        HashMap<String, String> args = new HashMap<>();
        args.put("mapfilename", name);
        args.put("halfsize", String.valueOf(halfSize));
        args.put("horizontalscale", String.valueOf(pixelSize));
        args.put("latitude", String.valueOf(latitude));
        args.put("westlongitude", String.valueOf(longitude));
        args.put("datadir", datadir);
        args.put("mapoutdir", mapoutdir);

        String arguments = constructUrlArguments(args);
        System.out.println("MapMakerRemote: runMapmaker: doing query");
        doQuery("http://sbmt.jhuapl.edu/admin/joshtest/index01.php", arguments);

        System.out.println("MapMakerRemote: runMapmaker: returned from running query");
        if (n == 1)
        {
            System.out.println("MapMakerRemote: runMapmaker: running Distributed Gravity");
            // Assemble options for calling DistributedGravity
            File tempFolder = new File("/Users/steelrj1/Desktop/");
            File objShapeFile = new File("/Users/steelrj1/Desktop/shape0.obj");
            File mapmakerToFitsFile = new File(outputFolder + File.separator + name + ".fits");
            File dgFitsFile = new File(outputFolder + File.separator + name + "_FINAL.FIT");
            List<String> dgOptionList = new LinkedList<String>();
            dgOptionList.add("-d");
            dgOptionList.add("2.67");   //density
            dgOptionList.add("-r");
            dgOptionList.add("0.000331165761670640");   //rotation rate
            dgOptionList.add("--werner");
            dgOptionList.add("--centers");
            dgOptionList.add("--batch-type");
            dgOptionList.add("local");
            dgOptionList.add("--fits-local");
            dgOptionList.add(mapmakerToFitsFile.getPath());
            dgOptionList.add("--ref-potential");
            dgOptionList.add("-53.765039959572114");    //reference potential
            dgOptionList.add("--output-folder");
            dgOptionList.add("/Users/steelrj1/Desktop");
            dgOptionList.add(objShapeFile.getPath()); // Global shape model file, Olivier suggests lowest res .OBJ **/
            dgOptionList.add(dgFitsFile.getPath()); // Path to output file that will contain all results
            dgOptionList.add("/Users/steelrj1/Desktop");
            dgOptionList.add(gravityExecutableName); // Version of gravity called differs by OS


            // Debug output
//            System.out.println("Calling Distributed Gravity with...");
//            System.out.println("  density = " + smallBodyModel.getDensity() + " g/cm^3");
//            System.out.println("  rotation rate = " + smallBodyModel.getRotationRate() + " rad/s");
//            System.out.println("  reference potential = " + smallBodyModel.getReferencePotential() + " J/kg");

            // Convert argument list to array and call DistributedGravity
            // Resulting FITS file has original Maplet2FITS planes plus the following:
            // Add gravity information by appending the following planes to the FITs file
            // 10. normal vector x component
            // 11. normal vector y component
            // 12. normal vector z component
            // 13. gravacc x component
            // 14. gravacc y component
            // 15. gravacc z component
            // 16. magnitude of grav acc
            // 17. grav potential
            // 18. elevation
            // 19. slope
            // 20. Tilt
            // 21. Tilt direction
            // 22. Tilt mean
            // 23. Tilt standard deviation
            // 24. Distance to plane
            // 25. Shaded relief
            String[] dgOptionArray = dgOptionList.toArray(new String[dgOptionList.size()]);
            for (String option : dgOptionArray)
            {
                System.out.println("MapMakerRemote: runMapmaker: option " + option);
            }
            SBMTDistributedGravity.main(dgOptionArray);
        }

//        String userpass = "sbmtAdmin:$mallBodies18!";
//        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
//        con.setRequestProperty ("Authorization", basicAuth);
//
//        con.setRequestMethod("GET");
//        String contentType = con.getHeaderField("Content-Type");
//
//        con.setConnectTimeout(5000);
//        con.setReadTimeout(5000);
//
//        int status = con.getResponseCode();
//        if( status == HttpURLConnection.HTTP_OK ){
//            InputStream is = con.getInputStream();
//            // do something with the data here
//            int len;
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            byte[] buffer = new byte[4096];
//            while (-1 != (len = is.read(buffer))) {
//              bos.write(buffer, 0, len);
//            }
//
//            try (OutputStream outputStream = new FileOutputStream("/Users/steelrj1/Desktop/test.fits"))
//            {
//               bos.writeTo(outputStream);
//               outputStream.close();
//            }
//
//        }else{
//            InputStream err = con.getErrorStream();
//            // err may have useful information.. but could be null see javadocs for more information
//        }

//        String execDir = dgCacheDir;
//
//        List<String> processCommand = new ArrayList<String>();
//
//        processBuilder = new ProcessBuilder(processCommand);
//
//        processBuilder.directory(new File(dgCacheDir));
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
//        String gravityExecutableName = "gravity" + executableExtension;
//        new File(execDir + File.separator + gravityExecutableName).setExecutable(true);
//
//     // Assemble options for calling BigmapDistributedGravity
//        String name = "test";
//        File tempFolder = new File("/Users/steelrj1/Desktop/");
//        File objShapeFile = new File("/Users/steelrj1/Desktop/shape0.obj");
//        File bigmapToFitsFile = new File(tempFolder + File.separator + name + ".fits");
//        File dgFitsFile = new File(tempFolder + File.separator + name + "_FINAL.FIT");
//        List<String> dgOptionList = new LinkedList<String>();
//        dgOptionList.add("-d");
//        dgOptionList.add("2.67");
////        dgOptionList.add(Double.toString(smallBodyModel.getDensity()));
//        dgOptionList.add("-r");
//        dgOptionList.add("0.000331165761670640");
////        dgOptionList.add(Double.toString(smallBodyModel.getRotationRate()));
//        dgOptionList.add("--werner");
//        dgOptionList.add("--centers");
//        dgOptionList.add("--batch-type");
//        dgOptionList.add("local");
//        dgOptionList.add("--fits-local");
//        dgOptionList.add(bigmapToFitsFile.getPath());
//        dgOptionList.add("--ref-potential");
//        dgOptionList.add("-53.765039959572114");
////        dgOptionList.add(Double.toString(smallBodyModel.getReferencePotential()));
//        dgOptionList.add("--output-folder");
//        dgOptionList.add("/Users/steelrj1/Desktop");
//        dgOptionList.add(objShapeFile.getPath()); // Global shape model file, Olivier suggests lowest res .OBJ **/
////        dgOptionList.add(tempFolder.getPath());
//        dgOptionList.add(dgFitsFile.getPath()); // Path to output file that will contain all results
//        dgOptionList.add("/Users/steelrj1/Desktop");
//        dgOptionList.add(gravityExecutableName); // Version of gravity called differs by OS
//
//
//        // Debug output
////        System.out.println("Calling Distributed Gravity with...");
////        System.out.println("  density = " + smallBodyModel.getDensity() + " g/cm^3");
////        System.out.println("  rotation rate = " + smallBodyModel.getRotationRate() + " rad/s");
////        System.out.println("  reference potential = " + smallBodyModel.getReferencePotential() + " J/kg");
//
//        // Convert argument list to array and call DistributedGravity
//        // Resulting FITS file has original Maplet2FITS planes plus the following:
//        // Add gravity information by appending the following planes to the FITs file
//        // 10. normal vector x component
//        // 11. normal vector y component
//        // 12. normal vector z component
//        // 13. gravacc x component
//        // 14. gravacc y component
//        // 15. gravacc z component
//        // 16. magnitude of grav acc
//        // 17. grav potential
//        // 18. elevation
//        // 19. slope
//        // 20. Tilt
//        // 21. Tilt direction
//        // 22. Tilt mean
//        // 23. Tilt standard deviation
//        // 24. Distance to plane
//        // 25. Shaded relief
//        String[] dgOptionArray = dgOptionList.toArray(new String[dgOptionList.size()]);
//        for (String option : dgOptionArray)
//        {
//            System.out.println("MapMakerRemote: runMapmaker: option " + option);
//        }
//        SBMTDistributedGravity.main(dgOptionArray);
//        con.disconnect();
    }

//    public void setSmallBodyModel(PolyhedralModel smallBodyModel)
//    {
//        this.smallBodyModel = smallBodyModel;
//    }

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


    public void setCacheDir(String cacheDir)
    {
        this.cacheDir = cacheDir;
    }

    protected OutputStream doQuery(String phpScript, String data) throws IOException
    {
        OutputStream outputStream = null;

        URL url = new URL(phpScript);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Java client");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        System.out.println("MapMakerRemote: doQuery: setting up auth");
        String userpass = "sbmtAdmin:$mallBodies18!";
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        con.setRequestProperty ("Authorization", basicAuth);

        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write(postData);
        }

        int status = con.getResponseCode();
        if( status == HttpURLConnection.HTTP_OK )
        {
            InputStream is = con.getInputStream();
            // do something with the data here
            int len;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (-1 != (len = is.read(buffer))) {
              bos.write(buffer, 0, len);
            }
            outputStream = new FileOutputStream(cacheDir + File.separator + name + ".fits");
            try
            {
               bos.writeTo(outputStream);
               outputStream.close();
               con.disconnect();
               mapletFitsFile = new File(cacheDir + File.separator + name + ".fits");
               return outputStream;
            }
            catch (Exception e)
            {
                System.out.println("MapMakerRemote: doQuery: " + e);
                con.disconnect();
                return null;
            }

        }
        else
        {
            System.out.println("MapMakerRemote: doQuery: got no status");
            InputStream err = con.getErrorStream();
            con.disconnect();
            System.exit(0);
            return null;
            // err may have useful information.. but could be null see javadocs for more information
        }
    }


    protected String constructUrlArguments(HashMap<String, String> args)
    {
        String str = "";

        boolean firstKey = true;
        for (String key : args.keySet())
        {
            if (firstKey == true)
                firstKey = false;
            else
                str += "&";

            str += key + "=" + args.get(key);
        }

        return str;
    }


}

class ParameterStringBuilder {
    public static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
    }
}
