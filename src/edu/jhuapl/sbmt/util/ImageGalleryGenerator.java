package edu.jhuapl.sbmt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.ConvertResourceToFile;
import edu.jhuapl.saavtk.util.DownloadableFileManager.StateListener;
import edu.jhuapl.saavtk.util.DownloadableFileState;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.NoInternetAccessException;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.core.image.IImagingInstrument;
import edu.jhuapl.sbmt.image.gui.controllers.images.ImageResultsTableController;
//import edu.jhuapl.sbmt.image.gui.controllers.images.ImageResultsTableController;
import edu.jhuapl.sbmt.query.IQueryBase;

/**
 * Class for managing access to image galleries. A gallery is a collection of
 * reduced images that may be used to browse image search results quickly in a
 * browser using a web page that is generated on-the-fly for each collection of
 * search results.
 * <p>
 * The current implementation is something of a compromise to offer a couple
 * options for how to manage downloads within the legacy implementations of
 * {@link IImagingIntrument}, {@link IQueryBase}, and most of all
 * {@link ImageResultsTableController}.
 * <p>
 * The compromise is needed because the factory method
 * {@link ImageGalleryGenerator#of(IImagingInstrument)} may be called multiple
 * times for what turns out to be a single gallery path (for example, if
 * multiple models access the same images). In order to avoid race conditions
 * and duplicate downloads, this class keeps a map of its instances and returns
 * only one for each gallery path, rather than instantiating repeatedly.
 * <p>
 * This class would ideally be package-private and final, so treat it as if it
 * were. It is the way it is because of the compromise described above. It is
 * not deprecated, but its use should not be expanded. It can't be private
 * because the nested {@link ImageGalleryEntry} class is public. It can't be
 * final because it was designed to have two distinct implementations before the
 * problem was detected that led to the compromise.
 *
 * @author Philip Twu, overhauled and augmented by James Peachey in 2021
 *
 */
public abstract class ImageGalleryGenerator
{
    /**
     * Only construct one instance per gallery.
     */
    private static final Map<String, ImageGalleryGenerator> GalleryMap = new HashMap<>();

    /**
     * A single gallery entry associated with a gallery image, a
     * preview/thumbnail image, and a caption
     */
    public static class ImageGalleryEntry
    {
        private final String caption;
        private final String imageFilename;
        private final String previewFilename;

        public ImageGalleryEntry(String caption, String imageFilename, String previewFilename)
        {
            this.caption = caption;
            this.imageFilename = imageFilename;
            this.previewFilename = previewFilename;
        }

        @Override
        public String toString()
        {
            return caption + " (" + previewFilename + " -> " + imageFilename + ")";
        }
    }

    protected static final SafeURLPaths SAFE_URL_PATHS = SafeURLPaths.instance();

    /**
     * Return a valid {@link ImageGalleryGenerator} for the specified
     * {@link IImagingInstrument}, or null if a gallery generator cannot be set
     * up for the instrument. See {@link #of(IImagingInstrument, StateListener)}
     * for more detail; also that overload is preferable to this one because it
     * allows the caller to be notified when the gallery has been downloaded and
     * unpacked.
     *
     * @param instrument the instrument
     * @return the gallery generator
     */
    public static synchronized ImageGalleryGenerator of(IImagingInstrument instrument)
    {
        return of(instrument, null);
    }

    /**
     * Return a valid {@link ImageGalleryGenerator} for the specified
     * {@link IImagingInstrument}, or null if a gallery generator cannot be set
     * up for the instrument. This can happen if the instrument does not include
     * a gallery (returning null for the path to the gallery), or if an
     * exception prevents the gallery from being set up completely.
     * <p>
     * The location of the associated gallery (if present) is obtained from the
     * {@link IQueryBase#getGalleryPath()} method for the query object returned
     * by the specified instrument's {@link IImagingInstrument#getSearchQuery()}
     * method.
     * <p>
     * This method attempts to download asynchronously an optional file named
     * "gallery-list.txt", which, if present, is expected to be a 3 column CSV
     * file associating the name of each image with the name of the preview
     * (thumbnail) image and finally the name of the gallery image. In the
     * absence of such a file, the gallery generator assumes the preview image
     * has the base name from the (full-size) image file but with the suffix
     * "-small.jpeg". Similarly, the gallery image is assumed to have an
     * identical basename but extension .jpeg.
     * <p>
     * The code also tries to download and unpack an optional file named
     * "gallery.zip", which, if present, is expected to contain the preview
     * thumbnail images (but not the gallery images themselves). This is so that
     * any proprietary thumbnail images may be unpacked in bulk prior to
     * actually displaying the gallery in the web browser.
     * <p>
     * Successfully-initialized instances of {@link ImageGalleryGenerator} for a
     * specific instrument are cached and looked up based on the gallery path
     * (if a gallery path is non-null). If this method encounters an exception
     * while trying to initialize an instance for the given instrument, it will
     * return null, but subsequent calls will keep attempting to set up the
     * gallery. This is in case a transient problem is responsible for the
     * exception.
     * <p>
     * The
     * {@link StateListener#respond(edu.jhuapl.saavtk.util.DownloadableFileState)}
     * method of the listener will FOR SURE be called once and only once if a
     * gallery is available for this instrument, whether or not an optional
     * "gallery.zip" file exists for the gallery. However, the
     * {@link DownloadableFileState} object passed to the listener is the state
     * of the "gallery.zip" file. But if the listener is called, it means a
     * gallery exists. The listener is used only by the file downloader, so
     * there is no need to remove the listener explicitly; the gallery
     * generator's reference to it will go out of scope and be garbage collected
     * after the listener is called.
     *
     * @param instrument the instrument for which to find the gallery
     * @param listener that responds to file state changes (may be null)
     * @return a generator for the instrument's gallery, or null if there is no
     *         gallery for this instrument.
     */
    public static synchronized ImageGalleryGenerator of(IImagingInstrument instrument, StateListener listener)
    {
        if (instrument == null)
        {
            return null;
        }

        IQueryBase query = instrument.getSearchQuery();

        if (query == null)
        {
            return null;
        }

        // Make this final to prevent accidentally changing it before using it
        // to add a map entry.
        final String galleryPath = query.getGalleryPath();

        if (galleryPath == null)
        {
            return null;
        }

        if (GalleryMap.containsKey(galleryPath))
        {
            // This method completed successfully before, just return the
            // result. Note it could be null.
            return GalleryMap.get(galleryPath);
        }

        String galleryParent = galleryPath.replaceFirst("[/\\\\]+[^/\\\\]+$", "");

        AtomicReference<String> galleryTopReference = new AtomicReference<>();

        String galleryListFile = SAFE_URL_PATHS.getString(galleryParent, "gallery-list.txt");

        File file;
        try
        {
            file = FileCache.getFileFromServer(galleryListFile);
        }
        catch (NoInternetAccessException e)
        {
            // Transient problem. Return here -- don't add to the map so this
            // gets tried again later.
            return null;
        }
        catch (Exception e)
        {
            // Ignore this -- this file is a newer resource, not present
            // in legacy models. It was added when DART models were added.
            file = null;
        }

        // Ensure the map will have an entry associated with this key. Make it
        // null for now, but below if all goes well it will be replaced with a
        // real gallery generator.
        GalleryMap.put(galleryPath, null);

        String dataPath = query.getDataPath();

        final ImageGalleryGenerator galleryGenerator;

        if (file == null || !file.isFile())
        {
            // Legacy behavior.
            galleryGenerator = new ImageGalleryGenerator() {

                @Override
                protected String getPreviewImageFile(String imageFileName)
                {
                    return "/" + imageFileName.replace(dataPath, galleryPath) + "-small.jpeg";
                }

                @Override
                protected String getGalleryImageFile(String imageFileName)
                {
                    return "/" + imageFileName.replace(dataPath, galleryPath) + ".jpeg";
                }

                @Override
                protected String getPreviewTopUrl()
                {
                    galleryTopReference.compareAndSet(null, Configuration.getDataRootURL().toString());

                    return galleryTopReference.get();
                }

                @Override
                protected void setPreviewTopUrl(String previewTopUrl)
                {
                    galleryTopReference.set(previewTopUrl != null ? previewTopUrl : Configuration.getDataRootURL().toString());
                }
            };
        }
        else
        {
            // Read the gallery-list.txt to associate each image with its
            // thumbnail and gallery image.
            try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {

                LinkedHashMap<String, String> previewImages = new LinkedHashMap<>();
                LinkedHashMap<String, String> galleryImages = new LinkedHashMap<>();

                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null)
                {
                    ++lineNumber;

                    String[] files = line.split("\\s+");

                    if (files.length != 3)
                    {
                        throw new IOException("Expected three fields in gallery list file " + file + " on line " + lineNumber + ": " + line);
                    }

                    String imageFilePath = SAFE_URL_PATHS.getString(dataPath, files[0]);
                    previewImages.put(imageFilePath, SAFE_URL_PATHS.getString(galleryPath, files[1]));
                    galleryImages.put(imageFilePath, SAFE_URL_PATHS.getString(galleryPath, files[2]));
                }

                galleryGenerator = new ImageGalleryGenerator() {

                    @Override
                    protected String getPreviewImageFile(String imageFileName)
                    {
                        return previewImages.get(imageFileName);
                    }

                    @Override
                    protected String getGalleryImageFile(String imageFileName)
                    {
                        return galleryImages.get(imageFileName);
                    }

                    @Override
                    protected String getPreviewTopUrl()
                    {
                        galleryTopReference.compareAndSet(null, Configuration.getDataRootURL().toString());

                        return galleryTopReference.get();
                    }

                    @Override
                    protected void setPreviewTopUrl(String previewTopUrl)
                    {
                        galleryTopReference.set(previewTopUrl != null ? previewTopUrl : Configuration.getDataRootURL().toString());
                    }

                };
            }
            catch (Exception e)
            {
                // This exception was thrown by code trying to parse the gallery
                // list file. If that failed once, it will probably always fail,
                // so continue -- this result should be cached.

                // This is probably already true; adding explicit "set" here for
                // completeness and to future-proof in case the code above
                // changes.
                System.err.println(e);
                return null;
            }
        }

        // Store the gallery generator.
        GalleryMap.put(galleryPath, galleryGenerator);

        // Finally, try to download and unzip the file that contains all the
        // preview/thumbnail images. This may take a while, so kick off a
        // background thread to do this.
        String galleryZipFile = SAFE_URL_PATHS.getString(galleryParent, "gallery.zip");

        FileCache.getFileFromServerAsync(galleryZipFile, false, true, state -> {
            if (state.isLocalFileAvailable())
            {
                galleryGenerator.setPreviewTopUrl(".");
            }

            if (listener != null)
            {
                listener.respond(state);
            }
        });

        return galleryGenerator;
    }

    /**
     * This constructor is private so that this class completely controls
     * instantiation.
     */
    private ImageGalleryGenerator()
    {
        super();
    }

    // Generates all the required image gallery files and returns the location
    // of the HTML file
    public String generateGallery(List<ImageGalleryEntry> entries)
    {
        // Define location and name of gallery file
        String galleryURL = SAFE_URL_PATHS.getString(Configuration.getCacheDir(), "gallery.html");

//        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
//
//            @Override
//            protected Void doInBackground() throws Exception
//            {
//                for (ImageGalleryEntry entry : entries) {
//                    try
//                    {
//                        FileCache.getFileFromServer(entry.previewFilename);
//                    }
//                    catch (Exception e)
//                    {
//
//                    }
//                    try
//                    {
//                        FileCache.getFileFromServer(entry.imageFilename);
//                    }
//                    catch (Exception e)
//                    {
//
//                    }
//                }
//                return null;
//            }
//
//        };
//        worker.execute();

        // Generate the image gallery
        try
        {
            String galleryName = "Search Results Image Gallery (Auto-Generated on " +
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + ")";
            generateGalleryHTML(galleryURL, galleryName, entries);
        }
        catch (Exception e)
        {
            System.err.println(e);
            return null;
        }

        // Copy over required javascript files
        ConvertResourceToFile.convertResourceToRealFile(
                galleryURL.getClass(),
                "/edu/jhuapl/sbmt/data/main.js",
                Configuration.getCustomGalleriesDir());
        ConvertResourceToFile.convertResourceToRealFile(
                galleryURL.getClass(),
                "/edu/jhuapl/sbmt/data/jquery.js",
                Configuration.getCustomGalleriesDir());

        // Return to user to be opened
        return galleryURL;
    }

    public ImageGalleryEntry getEntry(String imageFileName)
    {
        String imageFileUrl = SAFE_URL_PATHS.getString(Configuration.getDataRootURL().toString(), getGalleryImageFile(imageFileName));
        String previewFileUrl = locateGalleryFile(getPreviewImageFile(imageFileName));

        return new ImageGalleryEntry(imageFileName.substring(imageFileName.lastIndexOf("/") + 1), imageFileUrl, previewFileUrl);
    }

    protected abstract String getPreviewImageFile(String imageFileName);

    protected abstract String getGalleryImageFile(String imageFileName);

    protected abstract String getPreviewTopUrl();

    protected abstract void setPreviewTopUrl(String previewToUrl);

    protected String locateGalleryFile(String fileName)
    {
        return SAFE_URL_PATHS.getString(getPreviewTopUrl(), fileName);
    }

    // Creates equivalent of gallery.html at specified location and containing
    // entries in argument
    private void generateGalleryHTML(String galleryURL, String galleryName, List<ImageGalleryEntry> entries) throws FileNotFoundException
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
        writer.println("    margin-bottom:10px;");
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

        for (ImageGalleryEntry entry : entries)
        {
            writer.println("<li><a href=\"" +
                    entry.imageFilename +
                    "\" class=\"preview\" title=\"" + entry.caption + "\"><img src=\"" +
                    entry.previewFilename +
                    "\" alt=\"" + entry.caption + "\" /></a></li>");
        }
        writer.println("</ul>");
        writer.println("</body>");

        // End of document
        writer.println("</html>");

        // Close writer
        writer.close();
    }
}
