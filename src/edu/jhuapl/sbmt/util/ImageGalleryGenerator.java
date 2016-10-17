package edu.jhuapl.sbmt.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.ConvertResourceToFile;

public class ImageGalleryGenerator
{
    public static class ImageGalleryEntry
    {
        private String caption;
        private String imageFilename;
        private String previewFilename;

        public ImageGalleryEntry(String caption, String imageFilename, String previewFilename)
        {
            this.caption = caption;
            this.imageFilename = imageFilename;
            this.previewFilename = previewFilename;
        }
    }

    // Generates all the required image gallery files and returns the location of the HTML file
    public static String generateGallery(List<ImageGalleryEntry> entries)
    {
        // Define location and name of gallery file
        String galleryURL = Configuration.getCustomGalleriesDir() + File.separator + "gallery.html";

        // Generate the image gallery
        try
        {
            String galleryName = "Search Result Image Gallery (Auto-Generated on " +
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + ")";
            generateGalleryHTML(galleryURL,galleryName,entries);
        }
        catch(Exception e)
        {
            System.err.println(e);
            return null;
        }

        // Copy over required javascript files
        ConvertResourceToFile.convertResourceToRealFile(
                galleryURL,
                "/edu/jhuapl/sbmt/data/main.js",
                Configuration.getCustomGalleriesDir());
        ConvertResourceToFile.convertResourceToRealFile(
                galleryURL,
                "/edu/jhuapl/sbmt/data/jquery.js",
                Configuration.getCustomGalleriesDir());

        // Return to user to be opened
        return galleryURL;
    }

    // Creates equivalent of gallery.html at specified location and containing entries in argument
    private static void generateGalleryHTML(String galleryURL, String galleryName, List<ImageGalleryEntry> entries) throws FileNotFoundException
    {
        // Setup writer
        PrintWriter writer = new PrintWriter(galleryURL);

        // Write header
        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        writer.println("<head>");
        writer.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
        writer.println("<title>" + galleryName + "</title>");
        writer.println("<meta name=\"description\" content=\"Easiest jQuery Tooltip Ever\">");
        writer.println("<script src=\"jquery.js\" type=\"text/javascript\"></script>");
        writer.println("<script src=\"main.js\" type=\"text/javascript\"></script>");
        writer.println("</meta>");

        // Write style
        writer.println("<style>");
        writer.println("body {");
        writer.println("    margin:0;");
        writer.println("    padding:40px;");
        writer.println("    background:#fff;");
        writer.println("    font:80% Arial, Helvetica, sans-serif;");
        writer.println("    color:#555;");
        writer.println("    line-height:180%;");
        writer.println("}");
        writer.println("h1{");
        writer.println("    font-size:180%;");
        writer.println("    font-weight:normal;");
        writer.println("    color:#555;");
        writer.println("}");
        writer.println("h2{");
        writer.println("    clear:both;");
        writer.println("    font-size:160%;");
        writer.println("    font-weight:normal;");
        writer.println("    color:#555;");
        writer.println("    margin:0;");
        writer.println("    padding:.5em 0;");
        writer.println("}");
        writer.println("a{");
        writer.println("    text-decoration:none;");
        writer.println("    color:#f30;");
        writer.println("}");
        writer.println("p{");
        writer.println("    clear:both;");
        writer.println("    margin:0;");
        writer.println("    padding:.5em 0;");
        writer.println("}");
        writer.println("pre{");
        writer.println("    display:block;");
        writer.println("    font:100% \"Courier New\", Courier, monospace;");
        writer.println("    padding:10px;");
        writer.println("    border:1px solid #bae2f0;");
        writer.println("    background:#e3f4f9;");
        writer.println("    margin:.5em 0;");
        writer.println("    overflow:auto;");
        writer.println("    width:800px;");
        writer.println("}");
        writer.println("img{border:none;}");
        writer.println("ul,li{");
        writer.println("    margin:0;");
        writer.println("    padding:0;");
        writer.println("}");
        writer.println("li{");
        writer.println("    list-style:none;");
        writer.println("    float:left;");
        writer.println("    display:inline;");
        writer.println("    margin-right:10px;");
        writer.println("}");
        writer.println("#preview{");
        writer.println("    position:absolute;");
        writer.println("    border:1px solid #ccc;");
        writer.println("    background:#333;");
        writer.println("    padding:5px;");
        writer.println("    display:none;");
        writer.println("    color:#fff;");
        writer.println("    }");
        writer.println("</style>");
        writer.println("</head>");

        // Write entries
        writer.println("<body>");
        writer.println("<h1>" + galleryName + "</h1>");
        writer.println("<ul>");
        for(ImageGalleryEntry entry : entries)
        {
            writer.println("<li><a href=\"" +
                Configuration.getDataRootURL() + "/" + entry.imageFilename +
                "\" class=\"preview\" title=\"" + entry.caption + "\"><img src=\"" +
                Configuration.getDataRootURL() + "/" + entry.previewFilename +
                "\" alt=\"gallery thumbnail\" /></a></li>");
        }
        writer.println("</ul>");
        writer.println("</body>");

        // End of document
        writer.println("</html>");

        // Close writer
        writer.close();
    }
}
