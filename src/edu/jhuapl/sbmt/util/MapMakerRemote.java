package edu.jhuapl.sbmt.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Map;

public class MapMakerRemote
{

    public static void main(String[] args) throws IOException
    {
        new MapMakerRemote();
    }

    public MapMakerRemote() throws IOException
    {
//        Authenticator.setDefault (new Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication ("sbmtAdmin", "$mallBodies18!".toCharArray());
//            }
//        });

        URL url = new URL("http://sbmt.jhuapl.edu/admin/joshtest/index01.php");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        String userpass = "sbmtAdmin:$mallBodies18!";
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        con.setRequestProperty ("Authorization", basicAuth);


        con.setRequestMethod("GET");

//        Map<String, String> parameters = new HashMap<>();
//        parameters.put("param1", "val");

//        con.setDoOutput(true);
//        DataOutputStream out = new DataOutputStream(con.getOutputStream());
//        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
//        out.flush();
//        out.close();
//
//        con.setRequestProperty("Content-Type", "application/json");

        String contentType = con.getHeaderField("Content-Type");

        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        int status = con.getResponseCode();
        if( status == HttpURLConnection.HTTP_OK ){
            InputStream is = con.getInputStream();
            System.out.println("MapMakerRemote: MapMakerRemote: ok");
            // do something with the data here
            int len;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (-1 != (len = is.read(buffer))) {
              bos.write(buffer, 0, len);
            }
            System.out.println("MapMakerRemote: MapMakerRemote: response is: " + "RAW response is " + new String(bos.toByteArray()));
        }else{
            InputStream err = con.getErrorStream();
            // err may have useful information.. but could be null see javadocs for more information
        }

//        System.out.println("MapMakerRemote: MapMakerRemote: status is " + status);
//        BufferedReader in = new BufferedReader(
//          new InputStreamReader(con.getInputStream()));
//        String inputLine;
//        StringBuffer content = new StringBuffer();
//        while ((inputLine = in.readLine()) != null) {
//            content.append(inputLine);
//        }
//        in.close();

        con.disconnect();
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
