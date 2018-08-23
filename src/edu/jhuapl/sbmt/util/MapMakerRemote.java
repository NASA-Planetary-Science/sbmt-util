package edu.jhuapl.sbmt.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.Configuration;


public class MapMakerRemote
{
    private ProcessBuilder processBuilder;
    private String gravityExecutableName;
    private PolyhedralModel smallBodyModel;
    private String dgCacheDir = "/Users/steelrj1/git/sbmt/misc/programs";

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
        URL url = new URL("http://sbmt.jhuapl.edu/admin/joshtest/index01.php");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        String userpass = "sbmtAdmin:$mallBodies18!";
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        con.setRequestProperty ("Authorization", basicAuth);

        con.setRequestMethod("GET");
        String contentType = con.getHeaderField("Content-Type");

        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        int status = con.getResponseCode();
        if( status == HttpURLConnection.HTTP_OK ){
            InputStream is = con.getInputStream();
            // do something with the data here
            int len;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (-1 != (len = is.read(buffer))) {
              bos.write(buffer, 0, len);
            }

            try (OutputStream outputStream = new FileOutputStream("/Users/steelrj1/Desktop/test.fits"))
            {
               bos.writeTo(outputStream);
               outputStream.close();
            }

        }else{
            InputStream err = con.getErrorStream();
            // err may have useful information.. but could be null see javadocs for more information
        }

        String execDir = dgCacheDir;

        List<String> processCommand = new ArrayList<String>();

        processBuilder = new ProcessBuilder(processCommand);

        processBuilder.directory(new File(dgCacheDir));

        Map<String, String> env = processBuilder.environment();

        String executableExtension = "";
        if (Configuration.isLinux())
        {
            if (System.getProperty("sun.arch.data.model").equals("64"))
            {
                executableExtension = ".linux64";
            }
            else
            {
                executableExtension = ".linux32";
            }

            env.put("LD_LIBRARY_PATH", execDir);
        }
        else if (Configuration.isMac())
        {
            executableExtension = ".macosx";

            env.put("DYLD_LIBRARY_PATH", execDir);
        }
        else
        {
            executableExtension = ".win32.exe";
            //throw new IOException("Operating system not supported");
        }

        String gravityExecutableName = "gravity" + executableExtension;
        new File(execDir + File.separator + gravityExecutableName).setExecutable(true);

     // Assemble options for calling BigmapDistributedGravity
        String name = "test";
        File tempFolder = new File("/Users/steelrj1/Desktop/");
        File objShapeFile = new File("/Users/steelrj1/Desktop/shape0.obj");
        File bigmapToFitsFile = new File(tempFolder + File.separator + name + ".fits");
        File dgFitsFile = new File(tempFolder + File.separator + name + "_FINAL.FIT");
        List<String> dgOptionList = new LinkedList<String>();
        dgOptionList.add("-d");
        dgOptionList.add("2.67");
//        dgOptionList.add(Double.toString(smallBodyModel.getDensity()));
        dgOptionList.add("-r");
        dgOptionList.add("0.000331165761670640");
//        dgOptionList.add(Double.toString(smallBodyModel.getRotationRate()));
        dgOptionList.add("--werner");
        dgOptionList.add("--centers");
        dgOptionList.add("--batch-type");
        dgOptionList.add("local");
        dgOptionList.add("--fits-local");
        dgOptionList.add(bigmapToFitsFile.getPath());
        dgOptionList.add("--ref-potential");
        dgOptionList.add("-53.765039959572114");
//        dgOptionList.add(Double.toString(smallBodyModel.getReferencePotential()));
        dgOptionList.add("--output-folder");
        dgOptionList.add("/Users/steelrj1/Desktop");
        dgOptionList.add(objShapeFile.getPath()); // Global shape model file, Olivier suggests lowest res .OBJ **/
//        dgOptionList.add(tempFolder.getPath());
        dgOptionList.add(dgFitsFile.getPath()); // Path to output file that will contain all results
        dgOptionList.add("/Users/steelrj1/Desktop");
        dgOptionList.add(gravityExecutableName); // Version of gravity called differs by OS

        // Debug output
//        System.out.println("Calling Distributed Gravity with...");
//        System.out.println("  density = " + smallBodyModel.getDensity() + " g/cm^3");
//        System.out.println("  rotation rate = " + smallBodyModel.getRotationRate() + " rad/s");
//        System.out.println("  reference potential = " + smallBodyModel.getReferencePotential() + " J/kg");

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
//        SBMTDistributedGravity.main(dgOptionArray);
        con.disconnect();
    }

    public void setSmallBodyModel(PolyhedralModel smallBodyModel)
    {
        this.smallBodyModel = smallBodyModel;
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
