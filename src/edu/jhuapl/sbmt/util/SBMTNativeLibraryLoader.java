package edu.jhuapl.sbmt.util;

import static ch.unibas.cs.gravis.gdaljavanativelibs.GDALNativeLibraries.MAJOR_VERSION;
import static ch.unibas.cs.gravis.gdaljavanativelibs.GDALNativeLibraries.MINOR_VERSION;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import ch.unibas.cs.gravis.gdaljavanativelibs.GDALJavaNativeLibraryException;
import ch.unibas.cs.gravis.gdaljavanativelibs.GDALNativeLibraries;
import ch.unibas.cs.gravis.gdaljavanativelibs.GDALNativeLibrariesImpl;
import ch.unibas.cs.gravis.gdaljavanativelibs.Platform;
import ch.unibas.cs.gravis.gdaljavanativelibs.Util;
import ch.unibas.cs.gravis.vtkjavanativelibs.VtkJavaNativeLibraryException;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;

public class SBMTNativeLibraryLoader extends NativeLibraryLoader {

	private static File nativeGDALLibraryDir;

	public static void initialize(File nativeLibraryBaseDirectory)
			throws VtkJavaNativeLibraryException {

		GDALNativeLibrariesImpl gdalImpl;
		try {
			gdalImpl = GDALNativeLibraries.detectPlatform();
			SBMTNativeLibraryLoader.initialize(nativeLibraryBaseDirectory, gdalImpl);
		} catch (GDALJavaNativeLibraryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		NativeLibraryLoader.initialize(nativeLibraryBaseDirectory);
	}

	public static void initialize(File nativeLibraryBaseDirectory,
			GDALNativeLibrariesImpl gdalImpl) throws GDALJavaNativeLibraryException {

		// Create the target directory if it does not exist
		nativeGDALLibraryDir = createNativeGDALDirectory(nativeLibraryBaseDirectory);

		if (debug)
			System.out.println("Extract GDAL to " + nativeGDALLibraryDir);

		// Need section for GDAL her eventually
		for (URL libraryUrl : gdalImpl.getGDALLibraries()) {
			String nativeName = libraryUrl.getFile();

			File file = new File(nativeGDALLibraryDir,
					nativeName.substring(nativeName.lastIndexOf('/') + 1, nativeName.length()));

			try {
				Util.copyUrlToFile(libraryUrl, file);
			} catch (IOException e) {
				throw new GDALJavaNativeLibraryException("Error while copying library " + nativeName, e);
			}
		}

		for (URL libraryUrl : gdalImpl.getGDALLibraries()) {
			String nativeName = libraryUrl.getFile();
			File file = new File(nativeGDALLibraryDir,
					nativeName.substring(nativeName.lastIndexOf('/') + 1, nativeName.length()));
			Runtime.getRuntime().load(file.getAbsolutePath());
		}

//		NativeLibraryLoader.initialize(nativeLibraryBaseDirectory, impl);
	}

	public static File createNativeGDALDirectory(File nativeLibraryBaseDirectory) throws GDALJavaNativeLibraryException {

		File nativeLibraryDirectory = new File(nativeLibraryBaseDirectory,
				"gdaljavanatives-" + MAJOR_VERSION + "." + MINOR_VERSION);
		try {
			if (!nativeLibraryDirectory.exists()) {
				nativeLibraryDirectory.mkdirs();
			}
		} catch (Throwable t) {
			throw new GDALJavaNativeLibraryException("Unable to create directory for native libs", t);
		}
		return nativeLibraryDirectory;
	}

	private enum gdalNativeLibrary {
//		libcfitsio("libcfitsio.9"),
//		libhdf5_hl("libhdf5_hl.200"),
//		libhdf200("libhdf5.200"),
//		libjpeg("libjpeg.9"),
//		libnetcdf("libnetcdf.19"),
//		libpng16("libpng16.16"),
		libopenjp2("libopenjp2.7"),
		libproj("libproj.25"),
//		libsz("libsz.2"),
//		libtiff("libtiff.5"),
//		libtiledb("libtiledb"),
//		libwebp("libwebp.7"),
//		libzstd("libzstd.1.5.4"),
		libgdal("libgdal.33"),
		libgdalalljni("libgdalalljni");

		private String filename;

		private gdalNativeLibrary(String filename) {
			this.filename = filename;
		}

		public String getFilename() {
			return filename;
		}
	}

    private static void unpackNatives()
    {
    	if (debug)
    	{
	    	System.out.println("GDAL: gdal-native version: " + MAJOR_VERSION + "." + MINOR_VERSION);
	        System.out.println("GDAL: Java version: " + System.getProperty("java.version"));
	        System.out.println("GDAL: Current platform: " + Platform.getPlatform());
    	}
        if (Platform.isUnknown()) {
        	System.err.println("Cannot determine the platform you are running on.");
        	System.exit(1);
        }

        File nativeDir = new File(System.getProperty("user.home") + File.separator +".nativelibs");

        if (debug)
        	System.out.println("GDAL: Will unpack to : " + nativeDir);

        try {
            SBMTNativeLibraryLoader.initialize(nativeDir);
            System.out.println("GDAL: Initialization done, ");
        } catch (Throwable t) {
            System.err.println("Initialization failed with " + t.getClass().getSimpleName() + ", stacktrace follows.");
            t.printStackTrace(System.err);
            System.err.println("stacktrace above.");
            System.exit(1);
        }
    }

	public static void loadGDALLibraries() {

    	try {
    		File nativeDir = new File(System.getProperty("user.home") + File.separator +".nativelibs");
			GDALNativeLibraries.initialize(nativeDir);
		} catch (GDALJavaNativeLibraryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	unpackNatives();

        if (!EventQueue.isDispatchThread())
        {
            try
            {
                EventQueue.invokeAndWait(() -> {});
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

		boolean caughtLinkError = false;

		if (caughtLinkError) {
			throw new UnsatisfiedLinkError("One or more GDAL libraries failed to load");
		}
	}
}
