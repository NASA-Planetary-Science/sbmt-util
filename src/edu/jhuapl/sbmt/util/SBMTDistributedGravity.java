package edu.jhuapl.sbmt.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

//import com.beust.jcommander.JCommander;
//import com.beust.jcommander.Parameter;
//import com.beust.jcommander.ParameterException;

import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkOctreePointLocator;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;

import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.ProgressStatusListener;

import altwg.Fits.FitsHeaderType;
import altwg.Fits.HeaderTag;
import altwg.Fits.PlaneInfo;
import altwg.tools.ALTWGTool;
import altwg.tools.ToolsVersion;
import altwg.util.AltwgDataType;
import altwg.util.AltwgFits;
import altwg.util.BatchType;
import altwg.util.CellInfo;
import altwg.util.FitsData;
import altwg.util.FitsData.FitsDataBuilder;
import altwg.util.FitsHdr;
import altwg.util.FitsHdr.FitsHdrBuilder;
import altwg.util.FitsUtil;
import altwg.util.GridType;
import altwg.util.JCommanderUsage;
import altwg.util.SigmaFileType;
import altwg.util.StringUtil;
import altwg.util.TiltUtil;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.utilities.Main;
import spice.basic.Matrix33;
import spice.basic.Vector3;

/**
 * DistributedGravity program. See the usage string for more information about this program.
 *
 * @author Eli Kahn
 * @version 1.0
 *
 */
public class SBMTDistributedGravity implements ALTWGTool {

	 private static String rootDir;
	 private static String gravityExecutableName;

	 static ArrayList<GravityValues> results = new ArrayList<GravityValues>();


	// used for shortDescription() and fullDescription()
	private final static SBMTDistributedGravity defaultObj = new SBMTDistributedGravity();


	 @Override
	    public String shortDescription() {
	        String o = "Compute gravitational acceleration and potential from a shape model.  "
	                + "Runs in parallel on multiple machines.";
	        return o;
	    }

	    @Override
	    public String fullDescription() {
	        StringBuilder builder = new StringBuilder();
	        Arguments arguments = new Arguments();
	        JCommander jcommander = new JCommander(arguments);
	        jcommander.setProgramName("DistributedGravity");

	        JCommanderUsage jcUsage = new JCommanderUsage(jcommander);
	        jcUsage.setColumnSize(100);
	        jcUsage.usage(builder, 4, arguments.commandDescription);

	        return builder.toString();
	    }

	    private SBMTDistributedGravity() {
	    }

	    public static enum GravityAlgorithmType {
	        WERNER, CHENG
	    };

	    public static enum HowToEvaluate {
	        EVALUATE_AT_CENTERS, EVALUATE_AT_POINTS_IN_FITS_FILE
	    };

	    private static enum GridIndex {
	        X(0), Y(1), Z(2), NX(3), NY(4), NZ(5), ACCX(6), ACCY(7), ACCZ(8), ACCMAG(9), POT(10), ELE(11), SLP(12),
	        AREA(13);

	        private int value;

	        private GridIndex(int value) {
	            this.value = value;
	        }

	        private int index() {
	            return this.value;
	        }
	    }

	    /**
	     * EnumSet containing all the indices that need to be regridded from center of facet to points in fits file (i.e.
	     * vertices).
	     */
	    private static EnumSet<GridIndex> regridInds = EnumSet.of(
	            GridIndex.NX, GridIndex.NY, GridIndex.NZ,
	            GridIndex.ACCX, GridIndex.ACCY, GridIndex.ACCZ,
	            GridIndex.ACCMAG, GridIndex.POT, GridIndex.ELE, GridIndex.SLP, GridIndex.AREA);

	    /**
	     * Container class for storing gravitational acceleration and potential read from a srcfile. Also stores the source
	     * acceleration and potential file names for debugging purposes.
	     *
	     * @author espirrc1
	     *
	     */
	    public static class GravityValues {
	        public String srcAccFile;
	        public String srcPotFile;
	        public double[] acc = new double[3];
	        public double potential;
	    }

	    private static vtkPolyData globalShapeModelPolyData;
	    private static String sigmaFile;
	    private static double density;
	    private static double rotationRate;
	    private static GravityAlgorithmType gravityType;
	    private static HowToEvaluate howToEvalute;
	    private static String fieldpointsfile;
	    private static double refPotential;
	    private static boolean refPotentialProvided;
	    private static boolean minRefPotential;
	    private static double massUncertainty2;
	    private static boolean saveRefPotential;
	    private static String refPotentialFile;
	    private static int numCores;
	    private static String objfile;
	    private static String outfile;
	    private static String outputFolder;
	    private static BatchType batchType;
	    private static double tiltRadius = Double.NaN;
	    private static BatchType gridBatchType;
	    private static String inputfitsfile;
	    private static String externalBody;
	    private static double sigmaScale;

	    private static class Arguments {

			private final String commandDescription = ToolsVersion.getVersionString()
					+ "\n\n"
					+ "This program computes the gravitational acceleration and potential of a\n"
					+ "shape model at specified points and saves the values to files. Unlike the\n"
					+ "gravity program which is a single threaded program, this one is designed\n"
					+ "run in distributed manner by dividing the computation into multiple jobs\n"
					+ "and running them in parallel on one or more machines.\n\n";

			@Parameter(names = "-help", help = true)
			private boolean help;

			@Parameter(names = "-d", order = 0, description = "Density of shape model in g/cm^3, default is 1.0", required = false)
			private double density = 1D;

			@Parameter(names = "-r", order = 1, description = "Rotation rate of shape model in radians/sec, default is 1.0", required = false)
			private double rotationRate = 1D;

			@Parameter(names = "--werner", order = 2, description = "Use the Werner algorithm for computing the gravity (this is the "
					+ "default if neither --werner or --cheng option provided)", required = false)
			private boolean werner = true;

			@Parameter(names = "--cheng", order = 3, description = "Use Andy Cheng's algorithm for computing the gravity (default is to "
					+ "use Werner method if neither --werner or --cheng option provided)", required = false)
			private boolean cheng = false;

			@Parameter(names = "--centers", order = 4, description = "Evaluate gravity directly at the centers of plates of <platemodelfile> "
					+ "and output as an ASCII file. ", required = false)
			private boolean centers = true;

			@Parameter(names = "--min-ref-potential", order = 5, description = "If this argument is present, the code will use the minimum reference potential as "
					+ "the reference potential. By default the code calculates an average gravitational "
					+ "potential as the reference potential. Note that the --fits-local option still "
					+ "requires the user to specify --ref-potential as the code cannot calculate a reference "
					+ "potential from the fits file.", required = false)
			private boolean minRefPotential = false;

			@Parameter(names = "--fits-local", order = 6, description = "<filename> Evaluate gravity at points specified in a FITS file and output as a fits file."
					+ "It is assumed the <filename> FITS file represents a local surface region "
					+ "(e.g. a maplet or mapola) and contains at least 6 planes with the first 6 "
					+ "planes being lat, lon, rad, x, y, and z. "
					+ "The output FITS file will try to follow the ALTWG fits header and file naming "
					+ "convention. It will try to preserve the input fits header tags which follow the ALTWG "
					+ "fits header convention, i.e. 'mphase', 'datasrc', 'datasrcv'."
					+ "The output file will contain the original planes of the input FITS file and append the"
					+ "gravity planes."
					+ "NOTE: the tool will always create an ascii table file that contains the gravity values at the"
					+ "facet centers of the shape model defined by the vertices of the fits file. The ascii table filename"
					+ " will be <out-file>.gravtab", required = false)
			private String localFitsFname = "";

			@Parameter(names = "--ref-potential", order = 7, description = "<value> If the --fits-local option is provided, then you must use this option to specify the "
					+ "reference potential (in J/kg) which is needed for calculating elevation. This option is "
					+ "ignored if --fits-local is not provided. Can be either a number or a path to a "
					+ "file containing the number (the number must be the only contents of the file)", required = false)
			private String refPotential = "";

			@Parameter(names = "--save-ref-potential", order = 8, description = "<path> Save the reference potential computed by this program to a file at <path>.", required = false)
			private String refPotSaveFname = "";

			@Parameter(names = "--output-folder", order = 9, description = "<folder> Path to folder in which to place output files. These are temporary files created by "
					+ "gravity executable (default is current directory).", required = false)
			private String outputFolder = "";

			@Parameter(names = "--num-jobs", order = 10, description = "<numJobs> Specify the maximum allowed number of simultaneous jobs for the "
					+ "local computer (for batch type local). This should be equal to "
					+ "or less than number of cores on the local computer. If num-jobs is less than actual number of cores "
					+ "on the local machine the program will respect it and run on the specified number of cores. "
					+ "Be careful! No error checking is done to determine whether <numJobs> is greater than the actual "
					+ "number of CPUs on the local machine!"
					+ " Defaults to 1.", required = false)
			private int numJobs = 1;

			@Parameter(names = "--tilt-radius", order = 11, description = "<value> Radius to use for computing basic tilts. Radius units in km. "
					+ "If this argument is not specified then tilt values will NOT be written to output fits file. "
					+ "NOTE: If output is NOT a fits file then no tilts will be written at all, regardless of this argument. "
					+ "At each point the tilt of all plates within the specified radius is used to "
					+ "compute the tilt and tilt direction.\n "
					+ "This argument is only kept to allow backwards compatibility with older versions of the software."
					+ "It is highly recommended that Shape2Tilt be used instead to calculate the tilts."
					+ "The tilt radius should be no bigger than about 2 to 3 times the mean spacing of "
					+ "the points where the gravity is being evaluated. A larger tilt radius will result "
					+ "in very long running times. Note also that all plates within the radius are included "
					+ "even if those plates are not connected via some path along the surface to the tilt "
					+ "point. This would only happen with highly irregular geometry in which the surface "
					+ "somehow almost folds over itself.", required = false)
			private double tiltRadius = Double.NaN;

			@Parameter(names = "--gravConst", order = 12, description = "<value> Allow user to change the value for the gravitational constant. Units are in "
					+ "m^3/kgs^2. If not passed then will use the default value of 6.67384e-11.", required = false)
			private double gravConst = 6.67384e-11;

			@Parameter(names = "--batch-type", order = 13, description = "<grid|local> The gravity program can take a very long time for large shape models;"
					+ " to mitigate it this program supports 2 forms of parallelization, grid or local. In either case,"
					+ " the program first divides the computation into chunks. For 'grid' the program computes the"
					+ " number of chunks needed. For 'local' (the default batch type), the program utilizes"
					+ " the number of CPUs as specified by <numJobs>. Be careful: the program does NOT check whether"
					+ " <numJobs> is greater than the number of CPUs on the local machine! This could result in "
					+ " slower processing time or out of memory errors! If <numJobs>"
					+ " is less than the number of CPUs on the local machine then program will run on only <numJobs> CPUs. ", required = false)
			private String batchType = "local";

			@Parameter(names = "--num-slots", order = 14, description = "<numSlots> Specify the number of slots "
					+ " to be taken by an individual job submitted to the grid engine. Allowed values are"
					+ " 2, 3, 4, 6, 8, 16, or 32. Choose a value which will limit the number of simultaneous jobs per node"
					+ " to four or less in order to minimize out-of-memory errors. Note that a job will not run on a node"
					+ " if the node has less than <numSlots> available. Thus, a cluster which has nodes that have a maximum"
					+ " of 4 slots each will not be able to run any jobs if <numSlots> is greater than 4."
					+ " This argument will only apply if --batch-type = grid."
					+ " Defaults to 4 slots per job.", required = false)
			private int numSlots = 4;

			@Parameter(names = "--altwgNaming", order = 15, description = "If the --fits-local option is provided, the output is saved to a fits file."
					+ " Enabling this option names the output fits file according to the ALTWG Naming convention. This"
					+ " supercedes the <out-file> specified. If <out-file> contains a path then the altwg named"
					+ " output file will be saved in that same path. Otherwise the altwg named output file will"
					+ " be written in the current working directory.", required = false)
			private boolean altwgNaming = false;

			@Parameter(names = "--keepGfiles", order = 16, description = " Keep the raw intermediate gravity files produced by the gravity c++ executable instead of deleting them. "
					+ "Useful for debugging purposes. Default is to delete them.", required = false)
			private boolean keepGFiles = false;

			@Parameter(names = "--configFile", order = 17, description = " Fits configuration file. Only used if creating an output DTM fits file, which only happens if user wants"
					+ " gravity evaluated at points in a local fits file. Supercedes keyword values in the input fits header.", required = false)
			private String configFile = "";

			// @Parameter(names = "--externalBody", order = 18, description = "mass and position of external body in kg and
			// km (body fixed coordinates). "
			// + "e.g. -externalBody 7.342e22,-2.8933e5,2.4802e5,-1.3891e5", required = false)
			// private String externalBody = "";

			@Parameter(names = "--massUncertainty", order = 18, description = " Fractional uncertainty in the total mass.  Default value is 0.01.", required = false)
			private double massUncertainty = 0.01;

			@Parameter(names = "--sigma-global", order = 19, description = "Shape model radial vertex errors read from <filename>.  "
					+ "Ignored if --fits-local is specified.  If not present, calculated tilt uncertainties will be zero. Format of sigma file "
					+ "is 4 column csv where the columns are: vertex x, y, z, and the sigma value.\n")
			private String sigmaFile = "";

			@Parameter(names = "--sigmaFileType", order = 20, description = "If present then will parse the sigma file based"
					+ " on the file format defined for that type. Supported types are 'spc' or 'errorfromsql'"
					+ " (case-insensitive). If not specified then the default format is 'spc'."
					+ " The SPC sigma file format is 4 column csv where the columns are:"
					+ " vertex x,y,z and the sigma value.")
			private String sigmaFileType = "";

			@Parameter(names = "--sigmaScale", order = 21, description = "scale sigma values by value.  Only used when read from file"
					+ " for the global case. Defaults to 1 if not set.")
			private double sigmaScale = 1D;

			@Parameter(description = "Usage: DistributedGravity [options] <platemodelfile> <out-file>\n\n"
					+ "Where:\n"
					+ "  <platemodelfile>       Path to global shape model file in OBJ format.\n"
					+ "  <out-file>             Path to output file which will contain all results.\n"
					+ "                         For --centers, <out-file> is an ascii file containing results at the\n"
					+ "                         facet centers.\n"
					+ "                         For --fits-local, <out-file> is a FITS DTM containing all the planes\n"
					+ "                         of the <fits-filename> plus gravity results appended as additional\n"
					+ "                         planes. The tool also creates a separate ascii file named\n"
					+ "                         <out-file>.gravtab that contains the results at the facet centers.\n"
					+ "                         ")

			private List<String> files = new ArrayList<>();

			@Parameter(names = "-shortDescription", hidden = true)
			private boolean shortDescription = false;
		}

	    // From
	    // https://stackoverflow.com/questions/523871/best-way-to-concatenate-list-of-string-objects
	    private static String concatStringsWSep(List<Double> strings, String format, String separator) {
	        StringBuilder sb = new StringBuilder();
	        String sep = "";
	        for (Double s : strings) {
	            if (s == null)
	                sb.append(sep).append("NA");
	            else {
	                // test conversion first to make sure we aren't trying to convert NaN values.
	                // disabling fix so I can see what is going on in gravity code
	                /*
	                 * String doubleAsString = String.format("%30.16f", s); double testD =
	                 * StringUtil.parseSafeD(doubleAsString); if (Double.isNaN(testD)) { sb.append(sep).append("NA"); } else
	                 * { sb.append(sep).append(doubleAsString); }
	                 */
	                // let the number be as long as it needs to be to capture 16 decimal points
	                sb.append(sep).append(String.format(format, s));
	            }
	            sep = separator;
	        }
	        return sb.toString();
	    }

	    private static String padString(String str, int maxLength) {
	        while (str.length() < maxLength) {
	            str += " ";
	        }
	        return str;
	    }

	    /**
	     * Save gravity at the plate centers of the given plate model to an ascii file.
	     *
	     * @param gravityfile
	     *            - output file containing gravity values
	     * @param polydata
	     *            - plate model loaded into a vtkPolyData object
	     * @param results
	     *            - results from the 'gravity' executable.
	     * @throws IOException
	     * @throws FitsException
	     */
	    private static void saveResultsAtCenters(String gravityfile, vtkPolyData polydata, vtkFloatArray sigmaData,
	            List<GravityValues> results)
	            throws IOException, FitsException {
	        FileWriter ofs = new FileWriter(gravityfile);
	        BufferedWriter out = new BufferedWriter(ofs);
//	        System.out.println("SBMTDistributedGravity: saveResultsAtCenters: saving results at centers");
	        List<String> cols = new ArrayList<String>();
	        cols.add("X (km)");
	        cols.add("Y (km)");
	        cols.add("Z (km)");
	        cols.add("Latitude (deg)");
	        cols.add("Longitude (deg)");
	        cols.add("Radius (km)");
	        cols.add(AltwgDataType.NORMAL_VECTOR_X.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.NORMAL_VECTOR_Y.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.NORMAL_VECTOR_Z.getHeaderValueWithUnits());

	        cols.add(AltwgDataType.AREA.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITY_VECTOR_X.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITY_VECTOR_Y.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITY_VECTOR_Z.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITATIONAL_MAGNITUDE.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITATIONAL_POTENTIAL.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.ELEVATION.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.SLOPE.getHeaderValueWithUnits());

	        cols.add(AltwgDataType.GRAVITY_VECTOR_X_UNCERTAINTY.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITY_VECTOR_Y_UNCERTAINTY.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITY_VECTOR_Z_UNCERTAINTY.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITATIONAL_MAGNITUDE_UNCERTAINTY.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.GRAVITATIONAL_POTENTIAL_UNCERTAINTY.getHeaderValueWithUnits());
	        cols.add(AltwgDataType.EL_UNCERTAINTY.getHeaderValueWithUnits());

	        String dl = ",";
	        StringJoiner formatPart = new StringJoiner(dl);
	        for (String colName : cols) {
	            formatPart.add("%30s");
	        }
	        String[] dataPart = cols.toArray(new String[0]);
	        String columnDescriptions = String.format(formatPart.toString(), (Object[]) dataPart);

	        // replace big columnDescriptions string with above list. Modifying the list will automatically
	        // change the number of string formatters to use.
	        out.write(padString(String.format("%-26s %-21s", "Target", "Unknown"), columnDescriptions.length()) + "\r\n");
	        out.write(padString(String.format("%-26s %-21s", "Density (kg/m^3)", String.valueOf(1000.0 * density)),
	                columnDescriptions.length()) + "\r\n");
	        out.write(padString(String.format("%-26s %-21s", "Rotation Rate (rad/sec)", String.valueOf(rotationRate)),
	                columnDescriptions.length()) + "\r\n");
	        out.write(padString(String.format("%-26s %-21s", "Reference Potential (J/kg)", String.valueOf(refPotential)),
	                columnDescriptions.length()) + "\r\n");
	        out.write(columnDescriptions + "\r\n");

	        vtkIdList idList = new vtkIdList();
	        vtkPolyData polyDataCenters = PolyDataUtil2.getPlateCenters(polydata);
	        vtkOctreePointLocator pointLocator = new vtkOctreePointLocator();
	        pointLocator.FreeSearchStructure();
	        pointLocator.SetDataSet(polyDataCenters);
	        pointLocator.BuildLocator();

	        int numCells = polydata.GetNumberOfCells();
	        double[] radialErrorAtPoint = new double[polydata.GetNumberOfPoints()];
	        if (sigmaData != null) {
	            for (int i = 0; i < radialErrorAtPoint.length; i++)
	                radialErrorAtPoint[i] = sigmaData.GetValue(i);
	        }

	        for (int i = 0; i < numCells; ++i) {
	            CellInfo ci = CellInfo.getCellInfo(polydata, i, idList);

	            // the radius uncertainty is dimensionless - (dr/r)^2 is used in the error formulas
	            double radiusUncertainty2 = 0;
	            for (int j = 0; j < 3; j++)
	                radiusUncertainty2 += radialErrorAtPoint[idList.GetId(j)] * radialErrorAtPoint[idList.GetId(j)];

	            double radius2 = MathUtil.vnorm(ci.center);
	            radius2 *= radius2;
	            radiusUncertainty2 /= radius2;

	            List<Double> row = new ArrayList<Double>();
	            row.add(ci.center[0]);
	            row.add(ci.center[1]);
	            row.add(ci.center[2]);
	            row.add(ci.latitude);
	            row.add(ci.longitude);
	            row.add(ci.radius);
	            row.add(ci.normal[0]);
	            row.add(ci.normal[1]);
	            row.add(ci.normal[2]);
	            row.add(ci.area);
	            row.add(results.get(i).acc[0]);
	            row.add(results.get(i).acc[1]);
	            row.add(results.get(i).acc[2]);
	            double slope = getSlope(results.get(i).acc, ci.normal);
	            double elevation = getElevation(refPotential, results.get(i).acc, results.get(i).potential);
	            double accMag = getAccelerationMagnitude(results.get(i).acc, slope);
	            row.add(accMag);
	            row.add(results.get(i).potential);
	            row.add(elevation);
	            row.add(slope);

	            // gravity uncertainties
	            // From Olivier's email on May 9
	            /*
	             * The uncertainty on each g measurement is to first order: g*sqrt((dM/M)^2 +(2*dr/R)^2).
	             *
	             * For potential the error will be: U*sqrt((dM/M)^2+(dr/R)^2) where U is the magnitude of the gravitational
	             * potential at each facet
	             *
	             */

	            double gravMagUncertainty = accMag * Math.sqrt(massUncertainty2 + 2 * radiusUncertainty2);
	            double gravPotentialUncertainty = results.get(i).potential
	                    * Math.sqrt(massUncertainty2 + radiusUncertainty2);
	            double elevationUncertainty = elevation * Math.sqrt(
	                    gravPotentialUncertainty * gravPotentialUncertainty + gravMagUncertainty * gravMagUncertainty);
	            row.add(gravMagUncertainty * ci.center[0] / ci.radius);
	            row.add(gravMagUncertainty * ci.center[1] / ci.radius);
	            row.add(gravMagUncertainty * ci.center[2] / ci.radius);
	            row.add(gravMagUncertainty);
	            row.add(gravPotentialUncertainty);
	            row.add(elevationUncertainty);

	            out.write(concatStringsWSep(row, "%.16f", ",") + "\r\n");
	        }

	        out.close();
	    }

	    /**
	     * Save results in output FITS file. IMPORTANT: The output FITS datacube contains all the planes in the input
	     * datacube PLUS the new planes added by this method.
	     *
	     * @param altwgName
	     * @param inputfitsfile
	     * @param fitspolydata
	     * @param outputfitsfile
	     * @param gravAtLocations
	     * @throws Exception
	     */
	    @Deprecated
	    private static void saveResultsAtPointsInFitsFile(boolean altwgName, String configFile,
	            String inputfitsfile, vtkPolyData fitspolydata, String outputfitsfile, List<GravityValues> gravAtLocations)
	            throws Exception {
//	    	System.out.println("SBMTDistributedGravity: saveResultsAtPointsInFitsFile: saving results at point in FITS file");
	        // Get the dimensions of the input fits file
	        int[] axes = new int[3];
	        double[][][] indata = FitsUtil.loadFits(inputfitsfile, axes);
	        int inputNumPlanes = axes[0];

	        int planesToAdd = 11;
	        if (!Double.isNaN(tiltRadius)) {

	            // add basic tilt and basic tilt direction
	            planesToAdd = planesToAdd + 2;
	        }

	        double[][][] outdata = new double[inputNumPlanes + planesToAdd][axes[1]][axes[2]];

	        double[] pointOnPlane = new double[3];
	        Rotation rot = altwg.util.PolyDataUtil2.fitPlaneToPolyData(fitspolydata, pointOnPlane);
	        double[][] mat = rot.getMatrix();
	        double[] ux = { mat[0][0], mat[1][0], mat[2][0] };
	        double[] uz = { mat[0][2], mat[1][2], mat[2][2] };
	        // Put the sun pointing in direction vector that bisects ux and uz
	        double[] sun = { pointOnPlane[0] + ux[0] + uz[0], pointOnPlane[1] + ux[1] + uz[1],
	                pointOnPlane[2] + ux[2] + uz[2] };
	        MathUtil.vhat(sun, sun);
	        // Put the eye pointing in direction vector that bisects -ux and uz
	        double[] eye = { pointOnPlane[0] - ux[0] + uz[0], pointOnPlane[1] - ux[1] + uz[1],
	                pointOnPlane[2] - ux[2] + uz[2] };
	        MathUtil.vhat(eye, eye);

	        double[] pt = new double[3];
	        double[] normal = new double[3];
	        // TiltUtil tiltClass = new TiltUtil(tiltRadius);
	        String line;
	        int i = 0;

	        // read gravity values from fieldpoints file
	        FileReader ifs = new FileReader(fieldpointsfile);
	        BufferedReader in = new BufferedReader(ifs);
	        // int counter = 0;
	        while ((line = in.readLine()) != null) {
	            // counter++;
	            // System.out.println("line:" + counter);
	            String[] tokens = line.trim().split("\\s+");
	            pt[0] = Double.parseDouble(tokens[0]);
	            pt[1] = Double.parseDouble(tokens[1]);
	            pt[2] = Double.parseDouble(tokens[2]);
	            normal[0] = Double.parseDouble(tokens[3]);
	            normal[1] = Double.parseDouble(tokens[4]);
	            normal[2] = Double.parseDouble(tokens[5]);
	            int m = Integer.parseInt(tokens[6]);
	            int n = Integer.parseInt(tokens[7]);
	            int k = 0;
	            for (; k < inputNumPlanes; ++k)
	                outdata[k][m][n] = indata[k][m][n];
	            outdata[k++][m][n] = normal[0];
	            outdata[k++][m][n] = normal[1];
	            outdata[k++][m][n] = normal[2];
	            outdata[k++][m][n] = gravAtLocations.get(i).acc[0];
	            outdata[k++][m][n] = gravAtLocations.get(i).acc[1];
	            outdata[k++][m][n] = gravAtLocations.get(i).acc[2];
	            double slope = getSlope(gravAtLocations.get(i).acc, normal);
	            double elevation = getElevation(refPotential, gravAtLocations.get(i).acc, gravAtLocations.get(i).potential);
	            double accMag = getAccelerationMagnitude(gravAtLocations.get(i).acc, slope);
	            outdata[k++][m][n] = accMag;
	            outdata[k++][m][n] = gravAtLocations.get(i).potential;
	            outdata[k++][m][n] = elevation;
	            outdata[k++][m][n] = slope;

	            // elevation normal - Not needed! This is same as Height!
	            // outdata[k++][m][n] = PolyDataUtil2.getDistanceToPlane(pt, pointOnPlane, rot);

	            // shaded relief
	            outdata[k++][m][n] = getShadedRelief(normal, eye, sun);

	            if (!Double.isNaN(tiltRadius)) {
	                // calculate angle between vector to point and normal. call this tilt
	                double tilt = TiltUtil.basicTiltDeg(pt, normal);
	                outdata[k++][m][n] = tilt;
	                // calculate tilt direction. Note: assume longitude is 2nd plane in input fits file!
	                // The following will fail if this is not true!
	                double lon = indata[1][m][n];
	                double tiltDir = TiltUtil.basicTiltDir(lon, normal);

	                // tilt direction
	                outdata[k++][m][n] = tiltDir;
	            }

	            ++i;
	        }

	        in.close();

	        // assume that evaluating to points in fits file means output is not global
	        boolean isGlobal = false;

	        saveToFits(isGlobal, configFile, altwgName, outdata, inputfitsfile, outfile);

	    }

	    /**
	     * Read the gravity results from a given acceleration and potential file created by the 'gravity' executable and
	     * return as a list of GravityValues.
	     *
	     * @param accFile
	     * @param potFile
	     * @return
	     * @throws IOException
	     */
	    private static List<GravityValues> readGravityResults(File accFile, File potFile) throws IOException {

//	    	System.out.println("SBMTDistributedGravity: readGravityResults: reading gravity results");
	        ArrayList<double[]> accelerationVector = new ArrayList<double[]>();
	        ArrayList<Double> potential = new ArrayList<Double>();

	        if (!accFile.exists()) {
	            System.out.println("ERROR! acceleration file:\n" + accFile.getAbsolutePath() + " does not exist!");
	        }
	        if (!potFile.exists()) {
	            System.out.println("ERROR! grav-pot file:\n" + potFile.getAbsolutePath() + " does not exist!");
	        }

	        accelerationVector.addAll(altwg.util.FileUtil.loadPointDataArray(accFile.getAbsolutePath(), 0));
	        potential.addAll(FileUtil.getFileLinesAsDoubleList(potFile.getAbsolutePath()));

	        ArrayList<GravityValues> results = new ArrayList<GravityValues>();

	        int numLines = potential.size();
	        for (int i = 0; i < numLines; ++i) {
	            GravityValues r = new GravityValues();
	            r.srcAccFile = accFile.getName();
	            r.srcPotFile = potFile.getName();
	            r.potential = potential.get(i);
	            r.acc[0] = accelerationVector.get(i)[0];
	            r.acc[1] = accelerationVector.get(i)[1];
	            r.acc[2] = accelerationVector.get(i)[2];
	            results.add(r);
	        }

	        return results;
	    }

	    /**
	     * Run the gravity executable to calculate gravity values for each plate in the plate model or at specific points in
	     * the fits file Run in distributed mode if possible then load all results files and compile into one list of
	     * GravityValues.
	     *
	     * @param keepGfiles
	     *            - flag true to keep temporary gravity files
	     * @param GridType
	     *            - enum to specify type of batch processing
	     * @param gravConstant
	     *            - gravitational constant to use
	     * @return
	     * @throws InterruptedException
	     * @throws ExecutionException
	     * @throws IOException
	     */
	    private static List<GravityValues> getGravityAtLocations(boolean keepGfiles, GridType gridType,
	            double gravConstant, ProgressStatusListener listener)
	            throws InterruptedException, ExecutionException, IOException {

	        ArrayList<String> commandList = new ArrayList<String>();
//	        System.out.println("SBMTDistributedGravity: getGravityAtLocations: get gravity at location");
	        boolean useExternalBody = externalBody.length() > 0;

	        long size = 0;
	        String howToEvaluateSwitch = "";
	        if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
	            howToEvaluateSwitch = "centers";
	            size = globalShapeModelPolyData.GetNumberOfCells();
//	            System.out.println("preparing to evaluate gravity for " + String.valueOf(size) + " centers");
	        } else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE) {
	            // if (useExternalBody) {
	            // howToEvaluateSwitch = "file " + fieldpointsfile;
	            // } else {
	            // howToEvaluateSwitch = "--file " + fieldpointsfile;
	            // }
	            howToEvaluateSwitch = "file " + "\"" + fieldpointsfile + "\"";
	            size = FileUtil.getNumberOfLinesInfile(fieldpointsfile);
//	            System.out.println("preparing to evaluate gravity for " + String.valueOf(size) + " records"
//	                    + " in fits file");
	        }

	        String outfilename = new File(outfile).getName();

	        // dynamically scale the number of cores used so that chunk size is no less than 1000 records. Smaller chunk
	        // sizes
	        // would not see an improvement in speed and chunk sizes < 1 would result in a core dump.

	        // coresToUse exists only in this method and can be dynamically sized.
	        int coresToUse = numCores;
	        if (batchType.equals(gridBatchType)) {
//	            System.out.println("Determining job allocation for grid engine");
	            coresToUse = 100;
	        }

	        long chunk = size / coresToUse;
	        boolean changedCores = false;

	        // initial tests show that chunksize of 10k yields optimum speed
	        if (chunk < 10000L) {
	            // determine new coresToUse
	            long tempCores = size / 10000L;
	            int newCores = (int) Math.floor(tempCores);
	            if (newCores < 1) {
	                newCores = 1;
	            }
	            // only use newCores if it is less than coresToUse
	            // do NOT want to adjust upwards.
	            if (newCores < coresToUse) {
	                coresToUse = newCores;
	                changedCores = true;
	            }
	        }
	        chunk = size / coresToUse;
	        if (changedCores) {
	            System.out.printf("changed coresToUse to %d to get chunksize of %d\n", coresToUse, chunk);
	        }

	        // create the list of commands which we will submit to the batch queuing system
	        URI gravityExe;
	        try
            {
	        	URI jarURI = getJarURI();
	        	String parentPath = new File(jarURI.getPath()).getParent() + File.separator + "near.jar";

	        	URI updatedURI = new URI(jarURI.getScheme(), jarURI.getUserInfo(), jarURI.getHost(), jarURI.getPort(), parentPath.replace('\\', '/'), jarURI.getQuery(), jarURI.getFragment());

                gravityExe = getFile(updatedURI, "/misc/programs/gravity/macos/gravity");
                System.out.println(
                        "SBMTDistributedGravity: getGravityAtLocations: gravityExe " + gravityExe);
            }
            catch (URISyntaxException | FileNotFoundException e)
            {
                String path = new File("").getAbsolutePath();
//                System.out.println(
//                        "SBMTDistributedGravity: getGravityAtLocations: path is " + path);
                gravityExe = getFile(URI.create("file://" + path), "/misc/programs/gravity/macos/gravity");
                // TODO Auto-generated catch block
//                e.printStackTrace();
            }
//	        System.out.println(
//                    "SBMTDistributedGravity: getGravityAtLocations: gravityExe " + gravityExe);
//	        System.out.println(
//                    "SBMTDistributedGravity: getGravityAtLocations: output folder " + outputFolder);
	        long stopId = 0;
	        for (int i = 0; i < coresToUse; i++) {
	            final long startId = i * chunk;
	            stopId = i < coresToUse - 1 ? (i + 1) * chunk : size;

	            String command;
	            if (useExternalBody) {
	                command = String
	                        .format("GravityFromShapeModel -density %.16e -rotation %.16e -algorithm %s -evaluate %s -startIndex %d -endIndex %d -suffix %s%d "
	                                + "-outputFolder %s -gravConst %.16e -plateModel %s -externalBody %s", rootDir + File.separator + gravityExecutableName,
	                                density, rotationRate, gravityType.name().toLowerCase(), howToEvaluateSwitch, startId,
	                                stopId, outfilename, i, outputFolder, gravConstant, objfile, externalBody);
	            } else {
	                command = String.format(
	                        "export DYLD_FALLBACK_LIBRARY_PATH=" + new File(gravityExe.getPath()).getParent() + ";" + gravityExe.getPath() + " -d %.16e -r %.16e --%s --%s --start-index %d --end-index %d --suffix %s%d "
	                                + "--output-folder \"%s\" --gravConst %.16e %s", //rootDir + File.separator + gravityExecutableName,
	                        density, rotationRate, gravityType.name().toLowerCase(), howToEvaluateSwitch, startId,
	                        stopId, outfilename,  i, outputFolder, gravConstant, objfile);
	            }
//	            System.out.println(
//                        "SBMTDistributedGravity: getGravityAtLocations: command is " + command);
	            commandList.add(command);
	        }

	        if (batchType.equals(gridBatchType)) {
	            System.out.println("Sending list of " + commandList.size() + " jobs to grid engine, using type:"
	                    + gridBatchType.toString());
	        }

	        // Submit the batches and wait till they're finished
//	        BatchSubmitI batchSubmit = BatchSubmitFactory.getBatchSubmit(commandList, batchType, gridType);
	        SubmitLocalGravityJob batchSubmit = new SubmitLocalGravityJob(commandList, batchType);

	        // for LOCAL_PARALLEL_MAKE allow one to specify fewer cores than actually exist.
	        // batchSubmit initializes with the actual number of cores on the machine, so
	        // if user asks for more than actual it is ignored.
	        if (batchType.equals(BatchType.LOCAL_PARALLEL_MAKE)) {
	            int actualCores = Runtime.getRuntime().availableProcessors();
	            if (coresToUse < actualCores) {
	                batchSubmit.limitCores(coresToUse);
	            }
	        }

	        /*
	         * Configure batch to run in specified output folder if using grid engine, otherwise otherwise set to null to
	         * allow local OS to write files in local temp folder. Explicitly setting batchDir for grid engine to get around
	         * permission problems where a node may not have permission to write in the local /tmp folder
	         */
	        // TODO investigate why this might be failing for some grid nodes
	        String batchDir = outputFolder;
	        if (gridType.equals(GridType.LOCAL)) {
	            batchDir = null;
	        }
//	        System.out.println("SBMTDistributedGravity: getGravityAtLocations: stopid is " + stopId);
	        batchSubmit.runBatchSubmitinDir(batchDir, listener, (int)stopId);

	        // Now read in all results
//	        System.out.println("Reading in the results");
	        for (int i = 0; i < coresToUse; i++) {
	            String basename = new File(objfile).getName();
	            File accFile = new File(outputFolder + File.separator + basename + "-acceleration.txt" + outfilename + i);
	            File potFile = new File(outputFolder + File.separator + basename + "-potential.txt" + outfilename + i);
	            results.addAll(readGravityResults(accFile, potFile));

	            if (!keepGfiles) {
	                // we don't need these files so delete them
	                accFile.delete();
	                potFile.delete();
	            }
	        }

	        if (howToEvalute != HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE) {
	            refPotential = getRefPotential(results, minRefPotential);
//	            System.out.println("Reference Potential = " + refPotential);
	            // save out reference potential to file so it can be loaded in again
	            if (saveRefPotential)
	                FileUtils.writeStringToFile(new File(refPotentialFile), String.valueOf(refPotential));
	        }

	        return results;
	    }



	    // This function reads the reference potential from a file. It is assumed
	    // the reference potential is the only word in the file.
	    private static double getRefPotential(String filename) throws IOException {
	        ArrayList<String> words = altwg.util.FileUtil.getFileWordsAsStringList(filename);
	        return Double.parseDouble(words.get(0));
	    }

	    private static double getRefPotential(List<GravityValues> results, boolean minRefPotential) {
	        int numFaces = globalShapeModelPolyData.GetNumberOfCells();
//	        System.out.println("SBMTDistributedGravity: getRefPotential: ");
	        if (minRefPotential) {
	            System.out.println("Using minimum Potential as gravity reference potential");
	        } else {
	            System.out.println("Using average Potential as gravity reference potential");
	        }
	        if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS && results.size() != numFaces) {
	            System.err.println("Error: Size of array not equal to number of plates");
	            System.exit(1);
	        }

	        vtkIdList idList = new vtkIdList();

	        double[] pt1 = new double[3];
	        double[] pt2 = new double[3];
	        double[] pt3 = new double[3];
	        double potTimesAreaSum = 0.0;
	        double totalArea = 0.0;
	        if (minRefPotential) {

	            double minRefPot = Double.NaN;
	            for (GravityValues thisGrav : results) {
	                if ((Double.isNaN(minRefPot)) || (minRefPot > thisGrav.potential)) {
	                    minRefPot = thisGrav.potential;
	                }
	            }

	            // stop with error if for some reason this is still NaN
	            if (Double.isNaN(minRefPot)) {
	                System.out.println(
	                        "ERROR! Could not find minimum reference potential in DistributedGravity.getRefPotential()!");
	                System.out.println("STOPPING WITH ERROR!");
	                System.exit(1);
	            }
	            return minRefPot;

	        } else {
	            for (int i = 0; i < numFaces; ++i) {
	                CellInfo.getCellPoints(globalShapeModelPolyData, i, idList, pt1, pt2, pt3);

	                double potential = 0.0;
	                if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
	                    potential = results.get(i).potential;
	                }

	                double area = MathUtil.triangleArea(pt1, pt2, pt3);

	                potTimesAreaSum += potential * area;
	                totalArea += area;
	            }
	            return potTimesAreaSum / totalArea;

	        }

	    }

	    private static double getSlope(double[] acc, double[] normal) {
	        double[] negativeAcc = new double[3];
	        negativeAcc[0] = -acc[0];
	        negativeAcc[1] = -acc[1];
	        negativeAcc[2] = -acc[2];
	        return Math.toDegrees(MathUtil.vsep(normal, negativeAcc));
	    }

	    private static double getElevation(double refPotential, double[] acc, double potential) {
	        double accMag = MathUtil.vnorm(acc);
	        return (potential - refPotential) / accMag;
	    }

	    private static double getAccelerationMagnitude(double[] acc, double slope) {
	        double accMag = MathUtil.vnorm(acc);
	        if (slope > 90.0)
	            accMag = -Math.abs(accMag);
	        else
	            accMag = Math.abs(accMag);
	        return accMag;
	    }

	    /**
	     * Evaluate shaded relief value based on incidence, emission, phase angle of "eye" with respect to "sun"
	     *
	     * @param normal
	     * @param eye
	     * @param sun
	     * @return
	     */
	    private static double getShadedRelief(double[] normal, double[] eye, double[] sun) {
	        double emissionAngle = MathUtil.vsep(eye, normal);
	        double incidenceAngle = MathUtil.vsep(sun, normal);
	        double phaseAngle = MathUtil.vsep(eye, sun);

	        double cose = Math.cos(emissionAngle);
	        double cosi = Math.cos(incidenceAngle);

	        double beta = Math.exp(-Math.toDegrees(phaseAngle) / 60.0);
	        double result = (1.0 - beta) * cosi + beta * cosi / (cosi + cose);

	        if (result > 1.0)
	            result = 1.0;
	        if (result < 0.0)
	            result = 0.0;

	        return result;
	    }

	    public static void main(String[] args, ProgressStatusListener listener) throws Exception {

	        long startTime = System.currentTimeMillis();

	        density = 1.0;
	        rotationRate = 0.0;
	        gravityType = GravityAlgorithmType.WERNER;
	        howToEvalute = HowToEvaluate.EVALUATE_AT_CENTERS;
	        fieldpointsfile = null;
	        minRefPotential = false;
	        refPotential = 0.0;
	        refPotentialProvided = false;
	        numCores = 2;
	        outputFolder = ".";
	        batchType = BatchType.LOCAL_PARALLEL_MAKE;
	        inputfitsfile = null;
	        SigmaFileType sigmaType = SigmaFileType.SPCSIGMA;

	        // default to Sun Open Grid Engine. Currently this is the only supported grid engine.
	        // re-enable the --gridType argument if support is added for other grid engines.
	        GridType gridType = GridType.SUNOPENGRID;

	        Arguments arg = new Arguments();
	        JCommander command = new JCommander(arg);

	        try {

	            // command = new JCommander(arg, args);
	            command.parse(args);
	        } catch (ParameterException ex) {
	            System.out.println(defaultObj.fullDescription());
	            String mesg = "Error parsing input arguments:" + ex.getMessage();
	            throw new RuntimeException(mesg);
	        }

	        if (arg.shortDescription) {
	            System.out.println(defaultObj.shortDescription());
	            System.exit(0);
	        }

	        if ((args.length < 1) || (arg.help)) {
	            System.out.println(defaultObj.fullDescription());
	            System.exit(0);
	        }

	        density = arg.density;
	        rotationRate = arg.rotationRate;
	        massUncertainty2 = arg.massUncertainty * arg.massUncertainty;
	        sigmaScale = arg.sigmaScale;

	        // gravity algorithm
	        if (arg.cheng) {
	            gravityType = GravityAlgorithmType.CHENG;
	        }
//	        System.out.println("SBMTDistributedGravity: main: arg local fits" + arg.localFitsFname);
	        if (arg.localFitsFname.length() > 0) {
	            howToEvalute = HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE;
	            inputfitsfile = arg.localFitsFname;
	        }

	        // set grid batch type.
	        int numSlotsPerJob = arg.numSlots;
	        gridBatchType = BatchType.getSlotsType(numSlotsPerJob);

	        // use minimum ref potential
	        minRefPotential = arg.minRefPotential;
	        if (minRefPotential) {
	            System.out.println("Will use minimum reference potential as gravity potential");
	        } else {
	            System.out.println("Will use averaged reference potential as gravity potential");
	        }

	        // define reference potential (required for --fits-local)
	        if (arg.refPotential.length() > 0) {
	            // The argument is either a double or a file that contains the
	            // double as a single value
	            String str = arg.refPotential;
	            try {
	                refPotential = Double.parseDouble(str);
	            } catch (NumberFormatException e) {
	                refPotential = getRefPotential(str);
	            }
	            refPotentialProvided = true;
	        }

	        // save reference potential to a file
	        if (arg.refPotSaveFname.length() > 0) {

	            refPotentialFile = arg.refPotSaveFname;
	            saveRefPotential = true;

	        }

	        // define output folder path
	        if (arg.outputFolder.length() > 0) {
	            outputFolder = arg.outputFolder;
	        }

	        // default is empty string
	        externalBody = "";

	        // resolve outputFolder to an absolute path so that it can be resolved if the
	        // gravity code is passed to a grid engine node.
	        File resolveFile = new File(outputFolder);
	        outputFolder = resolveFile.getAbsolutePath();

	        numCores = arg.numJobs;

	        // specify local or grid engine processing
	        String type = arg.batchType;
	        boolean throwError = true;
	        if (type.equals("local")) {
	            batchType = BatchType.LOCAL_PARALLEL_MAKE;
	            gridType = GridType.LOCAL;
	        } else if (type.equals("grid")) {
	            batchType = gridBatchType;
	            if (numCores > 1) {
	                System.out.println("Ignoring --num-jobs specified. Will dynamically determine number of jobs per "
	                        + "grid node");
	            }
	            System.out.println("Number of slots taken per job:" + BatchType.slotPerJob(gridBatchType));
	            // gridType = GridType.parseType(gridType, throwError);
	        } else {
	            System.out.println("Could not parse batchtype from string:" + arg.batchType);
	            System.out.println("defaulting to 'local'");
	            batchType = BatchType.LOCAL_PARALLEL_MAKE;
	            gridType = GridType.LOCAL;
	        }

	        // check for tilt_radius
	        tiltRadius = arg.tiltRadius;
	        if (!Double.isNaN(tiltRadius)) {
	            System.out.println("Tilt radius set to:" + Double.toString(tiltRadius));
	            System.out.println("WILL CREATE BASIC TILT AND BASIC TILT DIR PLANES IN OUTPUT FITS.");
	        } else {
	            System.out.println("tiltRadius not specified. Will not create tilt planes.");
	        }

	        // get gravitational constant
	        double gravConst = arg.gravConst;

	        boolean altwgName = arg.altwgNaming;
	        boolean keepGfiles = arg.keepGFiles;

	        // There must be numRequiredArgs arguments remaining after the options.
	        // Otherwise abort.
	        List<String> filenames = arg.files;
	        int numberRequiredArgs = 2;
	        if (filenames.size() != numberRequiredArgs) {
	            System.out.println(defaultObj.fullDescription());
	            StringBuilder sb = new StringBuilder();
	            sb.append("\nERROR: <platemodelfile> and <out-file> are required inputs.\n");
	            sb.append("Please check your command syntax.\n");
	            sb.append("List of files parsed from the arguments:\n");
	            for (String file : filenames) {
	                sb.append("file:" + file + "\n");
	            }

	            throw new RuntimeException(sb.toString());
	        }

	        if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE && refPotentialProvided == false) {
	            System.out.println("Error: When evaluating at points in a file, you must provide a value for the\n"
	                    + "reference potential with the --ref-potential option.");
	            System.exit(1);
	        }

	        objfile = filenames.get(0);

	        // resolve objfile to an absolute path so that it can be resolved if the gravity code
	        // is passed to a grid engine.
	        resolveFile = new File(objfile);

	        // throw error if objfile does not exist
	        if (!resolveFile.exists()) {
	            String errMesg = "ERROR! obj file:" + resolveFile.getAbsolutePath()
	                    + " does not exist!";
	            throw new RuntimeException(errMesg);
	        }

	        objfile = resolveFile.getAbsolutePath();

	        outfile = filenames.get(1);

	        // resolve outfile to an absolute path so that it can be resolved if the gravity code
	        // is passed to a grid engine.
	        resolveFile = new File(outfile);
	        outfile = resolveFile.getAbsolutePath();

//	        System.out.println(StringUtil.timenow() + ":starting DistributedGravity");

	        NativeLibraryLoader.loadVtkLibraries();
	        NativeLibraryLoader.loadSpiceLibraries();

	        globalShapeModelPolyData = getGlobalModel(objfile);
	        if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
	            if (arg.sigmaFile.length() > 0) {
	                // csv file
//	                String delimiter = ",";
	                List<float[]> sigmasRead = altwg.util.PolyDataUtil2.readSigmaFile(new File(arg.sigmaFile), sigmaType);

	                // sigmas are stored in 3rd column.
	                int sigmaColumn = 3;
	                // sigmas should have units of km (same units as shape model).
	                double unitConversion = 1;
	                altwg.util.PolyDataUtil2.addSigmaToPolydata(globalShapeModelPolyData, sigmasRead, sigmaColumn, sigmaScale, unitConversion);
	            } else {
	                // add sigma of 0 to all facets
	                System.out.println("No sigma file passed by user! Setting all sigmas to 0 for"
	                        + " error calculations.");
	                altwg.util.PolyDataUtil2.zeroSigmaToPolydata(globalShapeModelPolyData);
	            }
	        }

	        List<GravityValues> gravAtLocations = null;

	        if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
	            gravAtLocations = getGravityAtLocations(keepGfiles, gridType, gravConst, listener);
	            saveResultsAtCenters(outfile, globalShapeModelPolyData,
	            		altwg.util.PolyDataUtil2.getSigmasFromPolydata(globalShapeModelPolyData), gravAtLocations);
	        } else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE) {
	            if (!new File(inputfitsfile).exists()) {
	                System.out.println("Error: " + inputfitsfile + " does not exist.");
	                System.exit(1);
	            }
//	            System.out.println("SBMTDistributedGravity: main: running gravity for local fits");
	            gravityForLocalFits(inputfitsfile, arg.configFile, gravConst, gridType, keepGfiles, altwgName, listener);

	        }

	        long stopTime = System.currentTimeMillis();
//	        System.out.printf("Time taken %d minutes\n", TimeUnit.MILLISECONDS.toMinutes(stopTime - startTime));
//	        System.out.println(StringUtil.timenow() + ":done DistributedGravity");

	    }

	    private static vtkPolyData getGlobalModel(String objfile) throws Exception {
//	    	System.out.println("SBMTDistributedGravity: getGlobalModel: ");
	        vtkPolyData globalShapeModelPolyData = PolyDataUtil.loadOBJShapeModel(objfile);

	        if (globalShapeModelPolyData.GetPointData().GetNormals() == null
	                || globalShapeModelPolyData.GetCellData().GetNormals() == null) {
	            // Add normal vectors
	            vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
	            normalsFilter.SetInputData(globalShapeModelPolyData);
	            normalsFilter.SetComputeCellNormals(1);
	            normalsFilter.SetComputePointNormals(1);
	            normalsFilter.SplittingOff();
	            normalsFilter.ConsistencyOn();
	            normalsFilter.AutoOrientNormalsOff();
	            normalsFilter.Update();

	            vtkPolyData normalsOutput = normalsFilter.GetOutput();
	            globalShapeModelPolyData.DeepCopy(normalsOutput);

	            normalsFilter.Delete();
	        }

	        return globalShapeModelPolyData;
	    }

	    private static void gravityForLocalFits(String inputfitsfile, String configFile, double gravConst,
	            GridType gridType,
	            boolean keepGfiles, boolean altwgName, ProgressStatusListener listener) throws Exception {
//	    	System.out.println("SBMTDistributedGravity: gravityForLocalFits: ");
	        if (configFile.length() > 0) {
	            // check to see that config file exists.
	            if (!new File(configFile).exists()) {
	                String errMesg = "ERROR! Fits configfile:" + configFile + " not found!";
	                throw new RuntimeException(errMesg);
	            }
	        }

//	        System.out.println("Loading fits file to vtkpolydata");

	        if (!new File(inputfitsfile).exists()) {
	            System.out.println("Error: " + inputfitsfile + " does not exist.");
	            System.exit(1);
	        }

	        List<vtkFloatArray> ancillaryData = new ArrayList<>();
	        vtkPolyData fitspolydata = altwg.util.PolyDataUtil2.loadLocalFitsLLRModel(inputfitsfile, ancillaryData);

	        // check if HEIGHT_STDERR is one of the planes. If so, use it as the vertex error. Otherwise use SIGMA.
	        List<PlaneInfo> sourcePlanes = AltwgFits.planesFromFits(inputfitsfile);
	        vtkFloatArray heightErrors = null;
	        if (sourcePlanes.contains(PlaneInfo.HEIGHT_STDERR)) {
	            heightErrors = ancillaryData
	                    .get(sourcePlanes.indexOf(PlaneInfo.HEIGHT_STDERR) - PlaneInfo.first6HTags.size());
	        } else if (sourcePlanes.contains(PlaneInfo.SIGMA)) {
	            heightErrors = ancillaryData
	                    .get(sourcePlanes.indexOf(PlaneInfo.SIGMA) - PlaneInfo.first6HTags.size());
	        } else {
	            System.err.printf("FITS file %s must include either a HEIGHT_STDERR plane or a SIGMA plane\n",
	                    inputfitsfile);
	            System.exit(0);
	        }

	        // Convert the fits file to ASCII
//	        System.out.println("converting fits to ascii.");
	        fieldpointsfile = outfile + ".fits2ascii";

	        saveLocalFitsCenters(fitspolydata, fieldpointsfile);

	        List<GravityValues> gravAtLocations = null;

//	        System.out.println("getting gravity at centers of fits file.");
	        results.clear();
	        gravAtLocations = getGravityAtLocations(keepGfiles, gridType, gravConst, listener);
	        results.addAll(gravAtLocations);
//	        System.out.println("Saving gravity at centers of fits file");
	        String tableFile = outfile + ".gravtab";
	        saveResultsAtCenters(tableFile, fitspolydata, heightErrors, gravAtLocations);

//	        System.out.println("Regridding to go to fits points.");

	        // regrid the gravity values to go from values at facet center to values at points in the fits file
	        // Get the dimensions of the input fits file and load fits cube into 3D array
	        int[] axes = new int[3];
	        double[][][] indata = FitsUtil.loadFits(inputfitsfile, axes);
	        int nX = axes[1];
	        int nY = axes[2];

	        double[][][] regriddedGravity = regridToLocalFitsPoints(inputfitsfile, indata, nX, nY,
	                fitspolydata, gravAtLocations, listener);

//	        System.out.println(
//                    "SBMTDistributedGravity: gravityForLocalFits: done with regridding");
	        // load fits header from input fits file to get rotation and translation information, as
	        // well as gsd scaling.
	        Map<String, HeaderCard> headerMap = FitsUtil.getFitsHeaderAsMap(inputfitsfile);

	        double gsd = Double.valueOf(headerMap.get(HeaderTag.GSD.toString()).getValue());

	        // need to convert gsd to units of km for internal calculation
	        String gsdUnits = headerMap.get(HeaderTag.GSD.toString()).getComment();

	        // scale factor to go from km to mm
	        double scalFactor = 1.0e6;
	        if (gsdUnits.contains("mm")) {

	            // gsd value is in mm. Convert to km
	            gsd = gsd / scalFactor;
	        } else if (gsdUnits.contains("cm")) {

	            // gsd value is in cm. Convert to km
	            scalFactor = 1.0e5;
	            gsd = gsd / scalFactor;
	        }

//	        System.out.println(
//                    "SBMTDistributedGravity: gravityForLocalFits: generating sun and eye vectors");
	        // need to generate sun and eye vectors for calculating shaded relief
	        double[] pointOnPlane = new double[3];
	        Rotation rot = altwg.util.PolyDataUtil2.fitPlaneToPolyData(fitspolydata, pointOnPlane);

	        // Put the sun pointing in direction vector that bisects ux and uz
	        double[][] mat = rot.getMatrix();
	        double[] ux = { mat[0][0], mat[1][0], mat[2][0] };
	        double[] uz = { mat[0][2], mat[1][2], mat[2][2] };
	        double[] sun = { pointOnPlane[0] + ux[0] + uz[0], pointOnPlane[1] + ux[1] + uz[1],
	                pointOnPlane[2] + ux[2] + uz[2] };
	        MathUtil.vhat(sun, sun);

	        // Put the eye pointing in direction vector that bisects -ux and uz
	        double[] eye = { pointOnPlane[0] - ux[0] + uz[0], pointOnPlane[1] - ux[1] + uz[1],
	                pointOnPlane[2] - ux[2] + uz[2] };
	        MathUtil.vhat(eye, eye);

	        int inputNumPlanes = axes[0];

	        // compile final output array. Consists of all planes from input fits file plus gravity planes
	        // plus basic tilt planes (if tilt is specified)

	        // adding all the regridded values + shaded relief map, which is not gravity related, but added
	        // by this method.
	        int planesToAdd = regridInds.size() + 1;

	        if (!Double.isNaN(tiltRadius)) {
	            // add basic tilt and basic tilt direction if tiltRadius is defined.
	            // do this for backwards compatibility
	            planesToAdd = planesToAdd + 2;
	        }

	        int numInputPlanes = axes[0];
//	        System.out.println("number of input planes:" + numInputPlanes);
	        double[][][] outData = new double[numInputPlanes + planesToAdd][nX][nY];

	        // copy the planes from the input fits file to output planes first
	        for (int kk = 0; kk < numInputPlanes; kk++) {
	            for (int mm = 0; mm < nX; mm++) {
	                System.arraycopy(indata[kk][mm], 0, outData[kk][mm], 0, nY);
	            }
	        }

	        double[] pt = new double[3];
	        double[] normal = new double[3];
	        for (int mm = 0; mm < nX; mm++) {
	            for (int nn = 0; nn < nY; nn++) {

	                // reset index of additional planes to add
	                int kk = numInputPlanes;

	                // assume vertex x,y,z are always part of the first 6 planes, which are always
	                // lat,lon,rad,x,y,z
	                pt[0] = outData[3][mm][nn];
	                pt[1] = outData[4][mm][nn];
	                pt[2] = outData[5][mm][nn];

	                normal[0] = regriddedGravity[GridIndex.NX.index()][mm][nn];
	                normal[1] = regriddedGravity[GridIndex.NY.index()][mm][nn];
	                normal[2] = regriddedGravity[GridIndex.NZ.index()][mm][nn];

	                outData[kk++][mm][nn] = normal[0];
	                outData[kk++][mm][nn] = normal[1];
	                outData[kk++][mm][nn] = normal[2];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.ACCX.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.ACCY.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.ACCZ.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.ACCMAG.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.POT.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.ELE.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.SLP.index()][mm][nn];
	                outData[kk++][mm][nn] = regriddedGravity[GridIndex.AREA.index()][mm][nn];

	                // shaded relief - not an official gravity plane. Included here as a 'nice-to-have'
	                outData[kk++][mm][nn] = getShadedRelief(normal, eye, sun);

	                if (!Double.isNaN(tiltRadius)) {
	                    // calculate angle between vector to point and normal. call this tilt
	                    double tilt = TiltUtil.basicTiltDeg(pt, normal);
	                    outData[kk++][mm][nn] = tilt;

	                    // calculate tilt direction. Note: assume longitude is 2nd plane in input fits file!
	                    // The following will fail if this is not true!
	                    double lon = indata[1][mm][nn];
	                    double tiltDir = TiltUtil.basicTiltDir(lon, normal);

	                    // tilt direction
	                    outData[kk++][mm][nn] = tiltDir;
	                }

	            }
	        }
	        // assume that evaluating to points in fits file means output is not global
	        boolean isGlobal = false;
//	        System.out.println(
//                    "SBMTDistributedGravity: gravityForLocalFits: saving to fits");
	        saveToFits(isGlobal, configFile, altwgName, outData, inputfitsfile, outfile);

	    }

	    private static void saveToFits(boolean isGlobal, String configFile, boolean altwgName, double[][][] outData,
	            String inputfitsfile, String outfile) throws FitsException, IOException {
//	    	System.out.println("SBMTDistributedGravity: saveToFits: ");
	        // construct fitsData. Will contain data array plus information pertaining to data array
	        FitsDataBuilder dataBuilder = new FitsDataBuilder(outData, isGlobal);
	        dataBuilder.setAltProdType(AltwgDataType.DTM);
	        FitsData fitsData = dataBuilder.build();

	        // extract header from input fits file. Will save ALTWG keywords and use them in the header for output fits
	        // file.
	        FitsHdrBuilder hdrBuilder = FitsHdr.copyFitsHeader(new File(inputfitsfile));

	        // update keywords with values from fits config file
	        if (configFile.length() > 0) {
	            hdrBuilder = FitsHdr.configHdrBuilder(configFile, hdrBuilder);
	        }

	        // update keywords with gravity metadata

	        // multiply density by 1000 to convert to kg/m^3
	        hdrBuilder.setVCbyHeaderTag(HeaderTag.DENSITY, density * 1000D, HeaderTag.DENSITY.comment());
	        hdrBuilder.setVCbyHeaderTag(HeaderTag.ROT_RATE, rotationRate, HeaderTag.ROT_RATE.comment());
	        hdrBuilder.setVCbyHeaderTag(HeaderTag.REF_POT, refPotential, HeaderTag.REF_POT.comment());

	        if (!Double.isNaN(tiltRadius)) {

	            // convert from km to m
	            hdrBuilder.setVCbyHeaderTag(HeaderTag.TILT_RAD, tiltRadius * 1000, HeaderTag.TILT_RAD.comment());
	        }

	        // extract the planes in the original fits file as a list.
	        List<PlaneInfo> planeList = AltwgFits.planesFromFits(inputfitsfile);
//	        System.out.println("Number of planes based on planelist from input fits file:" + planeList.size());

	        // append list of planes that were added by this method.
	        for (AltwgDataType anciFitsType : AltwgDataType.gravityPlanes) {
	            PlaneInfo thisPlane = anciFitsType.getPlaneInfo();
	            if (thisPlane == null) {
	                // throw runtime exception. this should be a DTM plane defined in PlaneInfo
	                String errMesg = "ERROR! No PlaneInfo associated with AltwgProducType:" + anciFitsType.toString();
	                throw new RuntimeException(errMesg);
	            }
//	            System.out.println("SBMTDistributedGravity: saveToFits: adding plane " + thisPlane);
	            planeList.add(thisPlane);
	        }
//	      planeList.add(PlaneInfo.NORM_VECTOR_X);
//	      planeList.add(PlaneInfo.NORM_VECTOR_Y);
//	      planeList.add(PlaneInfo.NORM_VECTOR_Z);
//	      planeList.add(PlaneInfo.GRAV_VECTOR_X);
//	      planeList.add(PlaneInfo.GRAV_VECTOR_Y);
//	      planeList.add(PlaneInfo.GRAV_VECTOR_Z);
//	      planeList.add(PlaneInfo.GRAV_MAG);
//	      planeList.add(PlaneInfo.GRAV_POT);
//	      planeList.add(PlaneInfo.ELEV);
//	      planeList.add(PlaneInfo.SLOPE);

	        //Shaded Relief is not an official gravity plane but is included as a "nice to have". Note that Shaded Relief only appears in local DTMs!
	        planeList.add(PlaneInfo.SHADE);

	        if (!Double.isNaN(tiltRadius)) {
	            planeList.add(PlaneInfo.TILT);
	            planeList.add(PlaneInfo.TILT_DIRECTION);
	        }

//	        System.out.println("total number of planes, after adding gravity:" + planeList.size());

	        // AltwgProductType altwgProduct = AltwgProductType.DTM;
//	        System.out.println(StringUtil.timenow() + ":saving local fits to " + outfile);

	        // assume evaluation at points in fits file only happens for local fits
	        FitsHeaderType hdrType = FitsHeaderType.DTMLOCALALTWG;
	        AltwgFits.saveDataCubeFits(fitsData, planeList, outfile, hdrBuilder, hdrType, altwgName);
	    }

	    // regrid the gravity values from facet centers to vertices. Points in the output fits file
	    // are at the vertices.
	    private static double[][][] regridToLocalFitsPoints(String inputfitsfile, double[][][] indata,
	            int nX, int nY, vtkPolyData fitspolydata,
	            List<GravityValues> gravAtLocations, ProgressStatusListener listener) throws Exception {
//	    	System.out.println("SBMTDistributedGravity: regridToLocalFitsPoints: ");
	        // load fits header from input fits file to get rotation and translation information, as
	        // well as gsd scaling.
	        Map<String, HeaderCard> headerMap = FitsUtil.getFitsHeaderAsMap(inputfitsfile);

	        double gsd = Double.valueOf(headerMap.get(HeaderTag.GSD.toString()).getValue());

	        // need to convert gsd to units of km for internal calculation
	        String gsdUnits = headerMap.get(HeaderTag.GSD.toString()).getComment();

	        // scale factor to go from km to mm
	        double scalFactor = 1.0e6;
	        if (gsdUnits.contains("mm")) {

	            // gsd value is in mm. Convert to km
	            gsd = gsd / scalFactor;
	        } else if (gsdUnits.contains("cm")) {

	            // gsd value is in cm. Convert to km
	            scalFactor = 1.0e5;
	            gsd = gsd / scalFactor;
	        }

	        Vector3 V = new Vector3(Double.valueOf(headerMap.get("CNTR_V_X").getValue()),
	                Double.valueOf(headerMap.get("CNTR_V_Y").getValue()),
	                Double.valueOf(headerMap.get("CNTR_V_Z").getValue()));

	        Vector3 ux = new Vector3(Double.valueOf(headerMap.get("UX_X").getValue()),
	                Double.valueOf(headerMap.get("UX_Y").getValue()),
	                Double.valueOf(headerMap.get("UX_Z").getValue()));
	        Vector3 uy = new Vector3(Double.valueOf(headerMap.get("UY_X").getValue()),
	                Double.valueOf(headerMap.get("UY_Y").getValue()),
	                Double.valueOf(headerMap.get("UY_Z").getValue()));
	        Vector3 uz = new Vector3(Double.valueOf(headerMap.get("UZ_X").getValue()),
	                Double.valueOf(headerMap.get("UZ_Y").getValue()),
	                Double.valueOf(headerMap.get("UZ_Z").getValue()));

	        Matrix33 r = new Matrix33(ux, uy, uz);

	        double[] translation = V.toArray();
	        double[][] rotation = r.toArray();

	        // create GMTGridUtil to regrid the data
	        SBMTGMTGridUtil gmtUtil = new SBMTGMTGridUtil(nX, nY, gsd);

	        // compile center x,y,z and gravity values into a 2D array
	        double[][] centerArr = valuesToRegrid(gravAtLocations, fitspolydata);

	        // set X,Y,Z positions where data was originally calculated
	        gmtUtil.setXYZ(centerArr[GridIndex.X.index()], centerArr[GridIndex.Y.index()], centerArr[GridIndex.Z.index()]);

	        // set x,y,z points where we want the data to be evaluated. This is extracted from the FITS planes that contain
	        // the
	        // x, y, z positions at each pixel.
	        List<PlaneInfo> sourcePlanes = AltwgFits.planesFromFits(inputfitsfile);
	        gmtUtil = setEvalPoints(gmtUtil, indata, nX, nY, sourcePlanes, listener);

	        // set rotation and translation
	        gmtUtil.setRotation(rotation);
	        gmtUtil.setTranslation(translation);

	        // create regridded plane that will contain gravity values regridded to correspond to the fits x,y,z locations.
	        // this is the output array

	        // output array will consist of x,y,z of vertex + all planes that need to be regridded
	        // am doing this so the indices of the regridded planes are the same as the indices to the original array
	        int planesToAdd = 3 + regridInds.size();
	        double[][][] gravRegridded = new double[planesToAdd][nX][nY];

	        // loop through all the gravity indices to regrid
	        int outIndex = 0;
	        for (GridIndex thisIndex : regridInds) {
	        	listener.setProgressStatus("Regridding Index " + thisIndex.index() + " of " + (regridInds.size()+2), 99);
//	            System.out.println("working on index:" + thisIndex.index());
	            gmtUtil.setField(centerArr[thisIndex.index()]);

	            // regridded array always consists of 7 planes: lat, lon, radius, x, y, z, field value.
	            double[][][] regridded = gmtUtil.regridField();

	            // copy x,y,z from regridded array to output array by columns
	            if (outIndex == 0) {
	                for (int kk = 0; kk < nX; kk++) {
	                    System.arraycopy(regridded[3][kk], 0, gravRegridded[0][kk], 0, nY);
	                    System.arraycopy(regridded[4][kk], 0, gravRegridded[1][kk], 0, nY);
	                    System.arraycopy(regridded[5][kk], 0, gravRegridded[2][kk], 0, nY);
	                }
	                outIndex = outIndex + 3;
	            }

	            // copy from the regridded array of this field to the output array, by columns
	            for (int kk = 0; kk < nX; kk++) {
	                System.arraycopy(regridded[6][kk], 0, gravRegridded[outIndex][kk], 0, nY);
	            }
	            outIndex++;
	        }
//	        System.out.println(
//                    "SBMTDistributedGravity: regridToLocalFitsPoints: returning " + gravRegridded);
	        listener.setProgressStatus("Done!", 100);
	        return gravRegridded;
	    }

	    /**
	     * Save fits file centers to fieldpointsfile. Only need to save center x,y,z because we will use GMTGridUtil to
	     * regrid from centers to points in fits file.
	     *
	     * @param fitspolyData
	     * @param fieldpointsfile
	     * @throws IOException
	     */
	    private static void saveLocalFitsCenters(vtkPolyData fitspolyData, String fieldpointsfile) throws IOException {
//	    	System.out.println("SBMTDistributedGravity: saveLocalFitsCenters: ");
	        int numCells = fitspolyData.GetNumberOfCells();
	        vtkIdList cellPointIDS = new vtkIdList();

	        // loop over all the cells and write out center x,y,z, normal x,y,z to ascii file.
	        boolean failOnNAN = true;
	        String delimiter = " ";

	        FileWriter os = new FileWriter(fieldpointsfile);
	        BufferedWriter out = new BufferedWriter(os);

	        for (int ii = 0; ii < numCells; ii++) {
	            CellInfo ci = CellInfo.getCellInfo(fitspolyData, ii, cellPointIDS);
	            StringBuilder sb = new StringBuilder();

	            // need to check string conversion for each value to see if it is valid. Fail if unable to do string
	            // conversion.
	            String testConvert = String.format("%.16e", ci.center[0]);
	            double checkVal = StringUtil.parseSafeDException(testConvert, failOnNAN);
	            sb.append(testConvert);
	            sb.append(delimiter);

	            testConvert = String.format("%.16e", ci.center[1]);
	            checkVal = StringUtil.parseSafeDException(testConvert, failOnNAN);
	            sb.append(testConvert);
	            sb.append(delimiter);

	            testConvert = String.format("%.16e", ci.center[2]);
	            checkVal = StringUtil.parseSafeDException(testConvert, failOnNAN);
	            sb.append(testConvert);

	            out.write(sb.toString() + "\n");
	        }

	        out.close();
	    }

	    private static SBMTGMTGridUtil setEvalPoints(SBMTGMTGridUtil gmt, double[][][] data, int nX, int nY,
	            List<PlaneInfo> fitsPlanes, ProgressStatusListener listener) {
//	    	System.out.println("SBMTDistributedGravity: setEvalPoints: ");
	        int xIndex = fitsPlanes.indexOf(PlaneInfo.X);
	        int yIndex = fitsPlanes.indexOf(PlaneInfo.Y);
	        int zIndex = fitsPlanes.indexOf(PlaneInfo.Z);
	        if (xIndex == -1) {
	            String mesg = String.format("No X plane found in FITS file %s!\n", inputfitsfile);
	            throw new RuntimeException(mesg);
	        }
	        if (yIndex == -1) {
	            String mesg = String.format("No Y plane found in FITS file %s!\n", inputfitsfile);
	            throw new RuntimeException(mesg);
	        }
	        if (zIndex == -1) {
	            String mesg = String.format("No Z plane found in FITS file %s!\n", inputfitsfile);
	            throw new RuntimeException(mesg);
	        }

	        // extract x,y,z at each pixel
	        double[] x = new double[nX * nY];
	        double[] y = new double[nX * nY];
	        double[] z = new double[nX * nY];

	        int index = 0;
	        for (int m = 0; m < nX; m++) {
	            for (int n = 0; n < nY; n++) {
	                x[index] = data[xIndex][m][n];
	                y[index] = data[yIndex][m][n];
	                z[index] = data[zIndex][m][n];

	                index++;
	            }
	        }

	        gmt.setEvaluationXYZ(x, y, z);
	        return gmt;
	    }

	    /**
	     * Convert x,y,z facet vector, x,y,z normal, facet area, and gravity results to 2D array to be used in regridding
	     * from centers to fits vertices.
	     *
	     * @param gravAtLocations
	     * @param fitsPolyData
	     * @return
	     */
	    private static double[][] valuesToRegrid(List<GravityValues> gravAtLocations, vtkPolyData polyData) {
//	    	System.out.println("SBMTDistributedGravity: valuesToRegrid: ");
	        int numCells = polyData.GetNumberOfCells();

	        int numGravPlanes = regridInds.size();

//	      private static EnumSet<GridIndex> regridInds = EnumSet.of(
//	              GridIndex.NX, GridIndex.NY, GridIndex.NZ,
//	              GridIndex.ACCX, GridIndex.ACCY, GridIndex.ACCZ,
//	              GridIndex.ACCMAG, GridIndex.POT, GridIndex.ELE, GridIndex.SLP);

	        //fields needed for calculation of gravity planes but are not part of the gravity planes output.
	        //ex. X,Y,Z to the facet center.
	        int nonGravFields = 3;

	        double[][] tableResults = new double[nonGravFields + numGravPlanes][numCells];

	        // loop over all facets (cells) and extract center x,y,z + gravity values to 2D array
	        vtkIdList cellPointIDS = new vtkIdList();
	        for (int ii = 0; ii < numCells; ii++) {

	            CellInfo ci = CellInfo.getCellInfo(polyData, ii, cellPointIDS);

	            // get center vector
	            tableResults[GridIndex.X.index()][ii] = ci.center[0];
	            tableResults[GridIndex.Y.index()][ii] = ci.center[1];
	            tableResults[GridIndex.Z.index()][ii] = ci.center[2];

	            // get center normal vector
	            tableResults[GridIndex.NX.index()][ii] = ci.normal[0];
	            tableResults[GridIndex.NY.index()][ii] = ci.normal[1];
	            tableResults[GridIndex.NZ.index()][ii] = ci.normal[2];

	            // get grav acceleration vector
	            tableResults[GridIndex.ACCX.index()][ii] = gravAtLocations.get(ii).acc[0];
	            tableResults[GridIndex.ACCY.index()][ii] = gravAtLocations.get(ii).acc[1];
	            tableResults[GridIndex.ACCZ.index()][ii] = gravAtLocations.get(ii).acc[2];

	            double slope = getSlope(gravAtLocations.get(ii).acc, ci.normal);

	            // get grav magnitude, potential, elevation, slope
	            tableResults[GridIndex.ACCMAG.index()][ii] = getAccelerationMagnitude(gravAtLocations.get(ii).acc, slope);
	            tableResults[GridIndex.POT.index()][ii] = gravAtLocations.get(ii).potential;
	            tableResults[GridIndex.ELE.index()][ii] = getElevation(refPotential, gravAtLocations.get(ii).acc,
	                    gravAtLocations.get(ii).potential);
	            tableResults[GridIndex.SLP.index()][ii] = slope;

	            // get area
	             tableResults[GridIndex.AREA.index()][ii] = ci.area;

	        }

	        return tableResults;
	    }

	    /**
	     * Parse the header of the gravity ascii file and use it to update values in FitsHdrBuilder
	     *
	     * @param gravityFile
	     * @param infitsMap
	     * @throws IOException
	     * @throws HeaderCardException
	     */
	    public static FitsHdrBuilder parseGravityHeader(String gravityFile, FitsHdrBuilder hdrBuilder)
	            throws HeaderCardException, IOException {
//	    	System.out.println("SBMTDistributedGravity: parseGravityHeader: ");
	        // public static void gravityToFitsMap(String gravityFile, Map<String, HeaderCard> infitsMap)
	        // throws IOException, HeaderCardException {
	        InputStream fs = new FileInputStream(gravityFile);
	        InputStreamReader isr = new InputStreamReader(fs);
	        BufferedReader in = new BufferedReader(isr);

	        // First 4 lines contain the header.
	        String line = in.readLine();

	        // parse header to add keyword/values to infitsMap
	        line = in.readLine();
	        String[] tokens = line.trim().split("\\s+");
	        double density = Double.parseDouble(tokens[tokens.length - 1].trim());
	        hdrBuilder.setVCbyHeaderTag(HeaderTag.DENSITY, density, HeaderTag.DENSITY.comment());
	        // infitsMap.put(HeaderTag.DENSITY.toString(), new HeaderCard(HeaderTag.DENSITY.toString(), density,
	        // HeaderTag.DENSITY.comment()));

	        line = in.readLine();
	        tokens = line.trim().split("\\s+");
	        double rotationRate = Double.parseDouble(tokens[tokens.length - 1].trim());
	        hdrBuilder.setVCbyHeaderTag(HeaderTag.ROT_RATE, rotationRate, HeaderTag.ROT_RATE.comment());
	        // infitsMap.put(HeaderTag.ROT_RATE.toString(), new HeaderCard(HeaderTag.ROT_RATE.toString(), rotationRate,
	        // HeaderTag.ROT_RATE.comment()));

	        line = in.readLine();
	        tokens = line.trim().split("\\s+");
	        double refPotential = Double.parseDouble(tokens[tokens.length - 1].trim());
	        hdrBuilder.setVCbyHeaderTag(HeaderTag.REF_POT, refPotential, HeaderTag.REF_POT.comment());
	        // infitsMap.put(HeaderTag.REF_POT.toString(), new HeaderCard(HeaderTag.REF_POT.toString(), refPotential,
	        // HeaderTag.REF_POT.comment()));

	        in.close();

	        return hdrBuilder;
	    }


	    public static URI getJarURI()
	            throws URISyntaxException
	        {
	            final ProtectionDomain domain;
	            final CodeSource       source;
	            final URL              url;
	            final URI              uri;

	            domain = Main.class.getProtectionDomain();
	            source = domain.getCodeSource();
	            url    = source.getLocation();
	            uri    = url.toURI();
	            return (uri);
	        }

	        public static URI getFile(final URI    where,
	                                   final String fileName)
	            throws ZipException,
	                   IOException
	        {
	            final File location;
	            final URI  fileURI;

	            location = new File(where);

	            // not in a JAR, just return the path on disk
	            if(location.isDirectory())
	            {
	                fileURI = URI.create(where.toString() + fileName);
	            }
	            else
	            {
	                final ZipFile zipFile;

	                zipFile = new ZipFile(location);

	                try
	                {
	                    fileURI = extract(zipFile, fileName);
	                }
	                finally
	                {
	                    zipFile.close();
	                }
	            }

	            return (fileURI);
	        }

	        private static URI extract(final ZipFile zipFile,
	                                   final String  fileName)
	            throws IOException
	        {
	            final File         tempFile;
	            final ZipEntry     entry;
	            final InputStream  zipStream;
	            OutputStream       fileStream;

	            tempFile = File.createTempFile(fileName, Long.toString(System.currentTimeMillis()));
	            tempFile.deleteOnExit();
	            entry    = zipFile.getEntry(fileName);

	            if(entry == null)
	            {
	                throw new FileNotFoundException("cannot find file: " + fileName + " in archive: " + zipFile.getName());
	            }

	            zipStream  = zipFile.getInputStream(entry);
	            fileStream = null;

	            try
	            {
	                final byte[] buf;
	                int          i;

	                fileStream = new FileOutputStream(tempFile);
	                buf        = new byte[1024];
	                i          = 0;

	                while((i = zipStream.read(buf)) != -1)
	                {
	                    fileStream.write(buf, 0, i);
	                }
	            }
	            finally
	            {
	                close(zipStream);
	                close(fileStream);
	            }

	            return (tempFile.toURI());
	        }

	        private static void close(final Closeable stream)
	        {
	            if(stream != null)
	            {
	                try
	                {
	                    stream.close();
	                }
	                catch(final IOException ex)
	                {
	                    ex.printStackTrace();
	                }
	            }
	        }


}
