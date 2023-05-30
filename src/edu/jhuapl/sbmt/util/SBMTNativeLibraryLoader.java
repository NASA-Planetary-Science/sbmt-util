package edu.jhuapl.sbmt.util;

import static ch.unibas.cs.gravis.vtkjavanativelibs.VtkNativeLibraries.MAJOR_VERSION;
import static ch.unibas.cs.gravis.vtkjavanativelibs.VtkNativeLibraries.MINOR_VERSION;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import edu.jhuapl.saavtk.util.NativeLibraryLoader;

import ch.unibas.cs.gravis.gdaljavanativelibs.GDALJavaNativeLibraryException;
import ch.unibas.cs.gravis.gdaljavanativelibs.GDALNativeLibraries;
import ch.unibas.cs.gravis.gdaljavanativelibs.GDALNativeLibrariesImpl;
import ch.unibas.cs.gravis.gdaljavanativelibs.Platform;
import ch.unibas.cs.gravis.gdaljavanativelibs.Util;
import ch.unibas.cs.gravis.vtkjavanativelibs.VtkJavaNativeLibraryException;

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
		System.out.println("native gdal is " + nativeGDALLibraryDir);

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
//	          System.out.println("file is " + file.getAbsolutePath());
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
    	System.out.println("gdal-native version: " + MAJOR_VERSION + "." + MINOR_VERSION);
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Current platform: " + Platform.getPlatform());
        if (Platform.isUnknown()) {
        	System.err.println("Cannot determine the platform you are running on.");
        	System.exit(1);
        }

//        File nativeDir = new File(System.getProperty("java.io.tmpdir"));
        File nativeDir = new File(System.getProperty("user.home") + File.separator +".nativelibs");

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

//        try {
//            System.out.println(new vtk.vtkVersion().GetVTKVersion());
////            new vtkJoglPanelComponent();
//        } catch (Throwable t) {
//            System.out.println("Could not invoke vtk Methode" +t.getMessage());
//            t.printStackTrace();
//        }
    }

	public static void loadGDALLibraries() {


//		try {
//		Runtime.getRuntime().loadLibrary("libgdal");
//		}  catch (UnsatisfiedLinkError e) {
//			e.printStackTrace();
//		}

		System.out.println("SBMTNativeLibraryLoader: loadGDALLibraries: loading all GDAL libs");
    	try {
    		File nativeDir = new File(System.getProperty("user.home") + File.separator +".nativelibs");
			GDALNativeLibraries.initialize(nativeDir);
		} catch (GDALJavaNativeLibraryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.println("SBMTNativeLibraryLoader: loadAllVtkLibraries: unpacking natives");
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
//		System.out.println("SBMTNativeLibraryLoader: loadGDALLibraries: native before loop " + nativeGDALLibraryDir);
//		for (gdalNativeLibrary lib : gdalNativeLibrary.values()) {
//			try {
////                  if (!lib.IsLoaded())
//				{
//					System.out.println("SBMTNativeLibraryLoader: loadGDALLibraries: trying to load "
//							+ new File(nativeGDALLibraryDir, lib.getFilename() + ".dylib").getAbsolutePath());
//					Runtime.getRuntime()
//							.load(new File(nativeGDALLibraryDir, lib.getFilename() + ".dylib").getAbsolutePath());
////                  	System.load(new File(nativeGDALLibraryDir, lib.getFilename() + ".dylib").getAbsolutePath());
//
//				}
//			} catch (UnsatisfiedLinkError e) {
//				caughtLinkError = true;
//				e.printStackTrace();
//			}
//		}

		if (caughtLinkError) {
			throw new UnsatisfiedLinkError("One or more GDAL libraries failed to load");
		}
	}

}
