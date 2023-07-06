package edu.jhuapl.sbmt.util.gravity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import vtk.vtkAbstractPointLocator;
import vtk.vtkIdList;
import vtk.vtkPointLocator;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;

import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Point3D;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.util.PolyDataUtil2;
import edu.jhuapl.sbmt.util.gravity.ParallelGrid.GridFunction;
import edu.jhuapl.sbmt.util.gravity.ParallelLoop.LoopFunction;

import altwg.util.FitsUtil;

/**
 * Gravity program. This the abstract base class for computing gravity values
 * (acceleration, potential, elevation, and slope) at points near an arbitrary shape model.
 * There is a single abstract method, getGravity, which subclasses must define in order to use this
 * class. This function returns the acceleration and potential at a point. Currently, we
 * provide to such subclasses. The first, GravityCheng, implements the gravity method
 * as derived by Andy Cheng as published in Cheng, A.F. et al., 2012,
 * Efficient Calculation of Effective Potential and Gravity on Small Bodies, ACM, 1667, p. 6447.
 * The second method, GravityWerner, implements the Werner and Scheeres method as published
 * in Werner, R.A. and Scheeres, D.J., 1997, Exterior gravitation of a polyhedron derived
 * and compared with harmonic and mascon gravitation representations of asteroid 4769 Castalia,
 * Celestial Mechanics and Dynamical Astronomy, Vol. 65, pp. 313-344. We recommened the Werner
 * method whenever possible since it is more accurate, though it is slower than the Cheng method.
 *
 * To use this class, instantiate one of the subclasses, then set options with the various setters.
 * Finally, call runGravity() and this returns an array with gravity results. In addition this function
 * saves out any generated files, depending on the options set.
 *
 * A static convenience method is provided, getGravityAtPoints, which can be used to compute
 * the gravity at an array of points.
 *
 * This class supports computing gravity at 5 different types of point sets. The type you
 * want can be set with the setHowToEvaluate setter method. These are:
 * 1. evaluate at all plate centers of global shape model
 * 2. evaluate at all plate vertices of global shape model
 * 3. evaluate at points in provided text file
 * 4. evaluate at points in provided FITS file
 * 5. evaluate at points in provided Java List
 *
 */
abstract public class Gravity {

    /**
     * Subclasses must define this function.
     *
     * @param fieldPoint 3D point at which to compute acceleration and potential
     * @param acc 3D acceleration vector at fieldPoint computed by this function
     * @return potential at fieldPoint
     */
    abstract protected double getGravity(double[] fieldPoint, double[] acc);

    private static final double G = 6.67384e-11 * 1.0e-9;

    public static enum HowToEvaluate {
        EVALUATE_AT_CENTERS, EVALUATE_AT_VERTICES, EVALUATE_AT_POINTS_IN_TEXT_FILE, EVALUATE_AT_POINTS_IN_FITS_FILE, EVALUATE_AT_POINTS_IN_LIST
    };

    public static class GravityValues {
        public double[] acc = new double[3];
        public double potential;
    }

    private static class CellInfo {
        public double[] pt0 = new double[3];
        public double[] pt1 = new double[3];
        public double[] pt2 = new double[3];
        public double[] normal = new double[3];
        public double[] center = new double[3];
        public double area;
        public double latitude;
        public double longitude;
        public double radius;
        public double tilt;
    }

    private static class PointInfo {
        public double[] pt = new double[3];
        public double[] normal = new double[3];
        public double latitude;
        public double longitude;
        public double radius;
        public double tilt;
    }

    private vtkPolyData globalShapeModelPolyData;
    private double density;
    private double rotationRate;
    private HowToEvaluate howToEvalute;
    private String fieldpointsfile;
    private double refPotential;
    private boolean refPotentialProvided;
    private String outfile;
    private double tiltRadius;
    private String inputfitsfile;
    private boolean localFits = true;
    private List<double[]> pointsToComputeGravity;
    private double[][][] gridToComputeGravity;

    public Gravity(vtkPolyData globalShapeModelPolyData) {
        this.globalShapeModelPolyData = globalShapeModelPolyData;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public void setRotationRate(double rotationRate) {
        this.rotationRate = rotationRate;
    }

    public void setHowToEvalute(HowToEvaluate howToEvalute) {
        this.howToEvalute = howToEvalute;
    }

    public void setFieldpointsfile(String fieldpointsfile) {
        this.fieldpointsfile = fieldpointsfile;
    }

    public void setRefPotential(double refPotential) {
        this.refPotential = refPotential;
    }

    public void setRefPotentialProvided(boolean refPotentialProvided) {
        this.refPotentialProvided = refPotentialProvided;
    }

    public void setOutfile(String outfile) {
        this.outfile = outfile;
    }

    public void setTiltRadius(double tiltRadius) {
        this.tiltRadius = tiltRadius;
    }

    public void setInputfitsfile(String inputfitsfile) {
        this.inputfitsfile = inputfitsfile;
    }

    public void setLocalFits(boolean localFits) {
        this.localFits = localFits;
    }

    public void setPointsToComputeGravityAt(List<double[]> pointsToComputeGravity)
    {
        this.pointsToComputeGravity = pointsToComputeGravity;
    }

    public List<GravityValues> runGravity() throws Exception {

        if ((howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_TEXT_FILE || howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE)
                && refPotentialProvided == false) {
            System.out.println("Error: When evaluating at points in a file, you must provide a value for the\n"
                    + "reference potential with the --ref-potential option.");
            return null;
        }

        if (howToEvalute == HowToEvaluate.EVALUATE_AT_VERTICES &&
                globalShapeModelPolyData.GetPointData().GetNormals() == null) {
            // Add normal vectors if not present
            vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
            normalsFilter.SetInputData(globalShapeModelPolyData);
            normalsFilter.SetComputePointNormals(1);
            // If cell normals are already present, then turn this on
            normalsFilter.SetComputeCellNormals(globalShapeModelPolyData.GetCellData().GetNormals() == null ? 0 : 1);
            normalsFilter.SplittingOff();
            normalsFilter.ConsistencyOn();
            normalsFilter.AutoOrientNormalsOff();
            normalsFilter.Update();

            vtkPolyData normalsOutput = normalsFilter.GetOutput();
            globalShapeModelPolyData.DeepCopy(normalsOutput);

            normalsFilter.Delete();
        }

        List<GravityValues> results = null;

        if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
            results = getGravityAtLocations();
            saveResultsAtCenters(outfile, globalShapeModelPolyData, results);
        }
        if (howToEvalute == HowToEvaluate.EVALUATE_AT_VERTICES) {
            results = getGravityAtLocations();
            saveResultsAtVertices(outfile, globalShapeModelPolyData, results);
        }
        if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_LIST) {
            results = getGravityAtLocations();
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_TEXT_FILE) {
            results = getGravityAtLocations();
            saveResultsAtPointsInFile(outfile, results);
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE) {
            // Convert the fits file to ASCII
            fieldpointsfile = outfile + ".ascii";
            PolyDataUtil2.convertFitsLLRModelToAscii(inputfitsfile, fieldpointsfile, true, localFits);
            results = getGravityAtLocations();
            vtkPolyData fitspolydata = null;
            if (localFits)
                fitspolydata = PolyDataUtil2.loadLocalFitsLLRModel(inputfitsfile);
            else
                fitspolydata = PolyDataUtil2.loadGlobalFitsLLRModel(inputfitsfile);
            saveResultsAtPointsInFitsFile(inputfitsfile, fitspolydata, outfile, results);
            new File(fieldpointsfile).delete();
        }

        return results;
    }

    /**
     * Convenience function for running the gravity program at a list of points.
     * On output, the elevation, acceleration and potential arrays will contain
     * values at each point.
     *
     * @param xyzPointList
     * @param density
     * @param rotationRate
     * @param referencePotential
     * @param shapeModelFile
     * @param elevation
     * @param accelerationMagnitude
     * @param accelerationVector
     * @param potential
     * @throws Exception
     */
    public static void getGravityAtPoints(
            List<double[]> xyzPointList,
            double density,
            double rotationRate,
            double referencePotential,
            vtkPolyData shapeModel,
            List<Double> elevation,
            List<Double> accelerationMagnitude,
            List<Point3D> accelerationVector,
            List<Double> potential) throws Exception
   {
        // Run the gravity program
        Gravity gravityProgram = new GravityWerner(shapeModel);
        gravityProgram.setDensity(density);
        gravityProgram.setRotationRate(rotationRate);
        gravityProgram.setRefPotential(referencePotential);
        gravityProgram.setRefPotentialProvided(true);
        gravityProgram.setHowToEvalute(HowToEvaluate.EVALUATE_AT_POINTS_IN_LIST);
        gravityProgram.setPointsToComputeGravityAt(xyzPointList);

        List<GravityValues> results = gravityProgram.runGravity();

        elevation.clear();
        accelerationMagnitude.clear();
        accelerationVector.clear();
        potential.clear();

        for (GravityValues values : results)
        {
            double e = gravityProgram.getElevation(referencePotential, values.acc, values.potential);
            elevation.add(e);
            accelerationVector.add(new Point3D(values.acc));
            accelerationMagnitude.add(MathUtil.vnorm(values.acc));
            potential.add(values.potential);
       }
   }

    /**
     * Similar to the saveProfile function but this one uses the gravity program
     * directly to compute the slope, elevation, acceleration, and potential columns
     * rather than simply getting the value from the plate data. It also has
     * a third argument for passing in a different shape model to use which is useful
     * for saving profiles on mapmaker maplets since we can't run the gravity program
     * on a maplet. It also does not save out any of the plate data.
     *
     * @param file
     * @param otherPolyhedralModel - use this small body for running gravity program. If null
     *                              use small body model passed into constructor.
     * @throws Exception
     */
    public static void saveProfileUsingGravityProgram(List<Vector3D> aPointL, File file, PolyhedralModel smallBodyModel) throws Exception
    {
        final String lineSeparator = System.getProperty("line.separator");

        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);

        // write header
        out.write("Distance (m)");
        out.write(",X (m)");
        out.write(",Y (m)");
        out.write(",Z (m)");
        out.write(",Latitude (deg)");
        out.write(",Longitude (deg)");
        out.write(",Radius (m)");

        out.write(",Slope (deg)");
        out.write(",Elevation (m)");
        out.write(",Gravitational Acceleration (m/s^2)");
        out.write(",Gravitational Potential (J/kg)");

        out.write(lineSeparator);

        // Run the gravity program
        List<Double> elevation = new ArrayList<Double>();
        List<Double> accelerationMagnitude = new ArrayList<Double>();
        List<Point3D> accelerationVector = new ArrayList<Point3D>();
        List<Double> potential = new ArrayList<Double>();
        List<double[]> pointList = new ArrayList<double[]>();
        for (Vector3D aPt : aPointL)
            pointList.add(aPt.toArray());
        Gravity.getGravityAtPoints(
                pointList,
                smallBodyModel.getDensity(),
                smallBodyModel.getRotationRate(),
                smallBodyModel.getReferencePotential(),
                smallBodyModel.getSmallBodyPolyData(),
                elevation,
                accelerationMagnitude,
                accelerationVector,
                potential);

        // To compute the distance, assume we have a straight line connecting the first
        // and last points of xyzPointList. For each point, p, in xyzPointList, find the point
        // on the line closest to p. The distance from p to the start of the line is what
        // is placed in heights. Use SPICE's nplnpt function for this.

        double[] first = aPointL.get(0).toArray();
        double[] last = aPointL.get(aPointL.size()-1).toArray();
        double[] lindir = new double[3];
        lindir[0] = last[0] - first[0];
        lindir[1] = last[1] - first[1];
        lindir[2] = last[2] - first[2];

        // The following can be true if the user clicks on the same point twice
        boolean zeroLineDir = MathUtil.vzero(lindir);

        double[] pnear = new double[3];
        double[] notused = new double[1];

        int i = 0;
        for (Vector3D aPt : aPointL)
        {
      	  double[] xyzArr = aPt.toArray();

            double distance = 0.0;
            if (!zeroLineDir)
            {
                MathUtil.nplnpt(first, lindir, xyzArr, pnear, notused);
                distance = 1000.0 * MathUtil.distanceBetween(first, pnear);
            }

            out.write(String.valueOf(distance));

            out.write("," + 1000.0 * aPt.getX());
            out.write("," + 1000.0 * aPt.getY());
            out.write("," + 1000.0 * aPt.getZ());

            LatLon llr = MathUtil.reclat(xyzArr).toDegrees();
            out.write("," + llr.lat);
            out.write("," + llr.lon);
            out.write("," + 1000.0 * llr.rad);

            // compute slope. Note to get the normal, use the smallBodyModel passed into constructor of
            // this class, not the smallBodyModel passed into this function.
            // The slope is the angular separation between the (negative) acceleration vector and
            // the normal vector.
            double[] normal = smallBodyModel.getClosestNormal(xyzArr);
            double[] accVector = accelerationVector.get(i).xyz;
            accVector[0] = -accVector[0];
            accVector[1] = -accVector[1];
            accVector[2] = -accVector[2];
            double slope = MathUtil.vsep(normal, accVector) * 180.0 / Math.PI;

            out.write("," + slope);
            out.write("," + elevation.get(i));
            out.write("," + accelerationMagnitude.get(i));
            out.write("," + potential.get(i));

            out.write(lineSeparator);
            ++i;
        }

        out.close();
    }



    private void getCellPoints(vtkPolyData polydata, int cellId, vtkIdList idList, double[] pt0, double[] pt1,
            double[] pt2) {
        polydata.GetCellPoints(cellId, idList);

        int numberOfCells = idList.GetNumberOfIds();
        if (numberOfCells != 3) {
            System.err.println("Error: Cells must have exactly 3 vertices!");
            return;
        }

        polydata.GetPoint(idList.GetId(0), pt0);
        polydata.GetPoint(idList.GetId(1), pt1);
        polydata.GetPoint(idList.GetId(2), pt2);
    }

    private CellInfo getCellInfo(vtkPolyData polydata, int cellId, vtkIdList idList) {
        CellInfo ci = new CellInfo();
        getCellPoints(polydata, cellId, idList, ci.pt0, ci.pt1, ci.pt2);

        MathUtil.triangleNormal(ci.pt0, ci.pt1, ci.pt2, ci.normal);
        MathUtil.triangleCenter(ci.pt0, ci.pt1, ci.pt2, ci.center);
        ci.area = MathUtil.triangleArea(ci.pt0, ci.pt1, ci.pt2);

        LatLon llr = MathUtil.reclat(ci.center).toDegrees();
        ci.latitude = llr.lat;
        ci.longitude = llr.lon;
        ci.radius = llr.rad;

        ci.tilt = getTilt(ci.center, ci.normal);

        return ci;
    }

    private PointInfo getPointInfo(vtkPolyData polydata, int pointId) {
        PointInfo ci = new PointInfo();

        polydata.GetPoint(pointId, ci.pt);

        ci.normal = polydata.GetPointData().GetNormals().GetTuple3(pointId).clone();

        LatLon llr = MathUtil.reclat(ci.pt).toDegrees();
        ci.latitude = llr.lat;
        ci.longitude = llr.lon;
        ci.radius = llr.rad;

        ci.tilt = getTilt(ci.pt, ci.normal);

        return ci;
    }

    private double getTilt(double[] radialVector, double[] normalVector) {
        return Math.toDegrees(MathUtil.vsep(radialVector, normalVector));
    }

    private DescriptiveStatistics getTiltStatistics(vtkAbstractPointLocator pointLocator, vtkPolyData polyData,
            double[] radialVector, double[] normalVector, vtkIdList idList) {
        pointLocator.FindPointsWithinRadius(tiltRadius, radialVector, idList);

        DescriptiveStatistics tiltStats = new DescriptiveStatistics();

        double[] pt = new double[3];
        int size = idList.GetNumberOfIds();
        for (int i = 0; i < size; ++i) {
            int id = idList.GetId(i);
            polyData.GetPoint(id, pt);
            double tilt = getTilt(pt, normalVector);
            tiltStats.addValue(tilt);
        }

        return tiltStats;
    }

    // From
    // https://stackoverflow.com/questions/523871/best-way-to-concatenate-list-of-string-objects
    private String concatStringsWSep(List<Double> strings, String separator) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (Double s : strings) {
            if (s == null)
                sb.append(sep).append("NA");
            else
                sb.append(sep).append(String.format("%-21.16f", s));
            sep = separator;
        }
        return sb.toString();
    }

    private void saveResultsAtCenters(String csvfile, vtkPolyData polydata, List<GravityValues> results)
            throws IOException {
        FileWriter ofs = new FileWriter(csvfile);
        BufferedWriter out = new BufferedWriter(ofs);

        out.write("X (km),Y (km),Z (km),Latitude (deg),Longitude (deg),Radius (km),Nx,Ny,Nz,Area (km^2),"
                + "Gravitational Acceleration X (m/s^2),Gravitational Acceleration Y (m/s^2),Gravitational Acceleration Z (m/s^2),"
                + "Gravitational Acceleration Magnitude (m/s^2),Gravitation Potential (J/kg),Elevation (meters),Slope (deg),"
                + "Tilt (deg),Mean Tilt (deg),Stdev Tilt (deg)\n");

        vtkIdList idList = new vtkIdList();
        vtkPolyData polyDataCenters = PolyDataUtil2.getPlateCenters(polydata);
        vtkPointLocator pointLocator = new vtkPointLocator();
        pointLocator.FreeSearchStructure();
        pointLocator.SetDataSet(polyDataCenters);
        pointLocator.BuildLocator();

        int numCells = polydata.GetNumberOfCells();
        for (int i = 0; i < numCells; ++i) {
            CellInfo ci = getCellInfo(polydata, i, idList);
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
            row.add(ci.tilt);
            DescriptiveStatistics tiltStats = getTiltStatistics(pointLocator, polyDataCenters, ci.center, ci.normal,
                    idList);
            row.add(tiltStats.getMean());
            row.add(tiltStats.getStandardDeviation());
            out.write(concatStringsWSep(row, ",") + "\n");
        }

        out.close();
    }

    private void saveResultsAtVertices(String csvfile, vtkPolyData polydata, List<GravityValues> results)
            throws IOException {
        FileWriter ofs = new FileWriter(csvfile);
        BufferedWriter out = new BufferedWriter(ofs);

        out.write("X (km),Y (km),Z (km),Latitude (deg),Longitude (deg),Radius (km),Nx,Ny,Nz,"
                + "Gravitational Acceleration X (m/s^2),Gravitational Acceleration Y (m/s^2),Gravitational Acceleration Z (m/s^2),"
                + "Gravitational Acceleration Magnitude (m/s^2),Gravitation Potential (J/kg),Elevation (meters),Slope (deg),"
                + "Tilt (deg),Mean Tilt (deg),Stdev Tilt (deg)\n");

        vtkIdList idList = new vtkIdList();
        SmallBodyModel smallBodyModel = new SmallBodyModel(csvfile, polydata);

        int numPoints = polydata.GetNumberOfPoints();
        for (int i = 0; i < numPoints; ++i) {
            PointInfo pi = getPointInfo(polydata, i);
            List<Double> row = new ArrayList<Double>();
            row.add(pi.pt[0]);
            row.add(pi.pt[1]);
            row.add(pi.pt[2]);
            row.add(pi.latitude);
            row.add(pi.longitude);
            row.add(pi.radius);
            row.add(pi.normal[0]);
            row.add(pi.normal[1]);
            row.add(pi.normal[2]);
            row.add(results.get(i).acc[0]);
            row.add(results.get(i).acc[1]);
            row.add(results.get(i).acc[2]);
            double slope = getSlope(results.get(i).acc, pi.normal);
            double elevation = getElevation(refPotential, results.get(i).acc, results.get(i).potential);
            double accMag = getAccelerationMagnitude(results.get(i).acc, slope);
            row.add(accMag);
            row.add(results.get(i).potential);
            row.add(elevation);
            row.add(slope);
            row.add(pi.tilt);
            DescriptiveStatistics tiltStats = getTiltStatistics(smallBodyModel.getPointLocator(), polydata, pi.pt,
                    pi.normal, idList);
            row.add(tiltStats.getMean());
            row.add(tiltStats.getStandardDeviation());
            out.write(concatStringsWSep(row, ",") + "\n");
        }

        out.close();
    }

    private void saveResultsAtPointsInFile(String csvfile, List<GravityValues> results) throws IOException {
        FileReader ifs = new FileReader(fieldpointsfile);
        BufferedReader in = new BufferedReader(ifs);

        FileWriter ofs = new FileWriter(csvfile);
        BufferedWriter out = new BufferedWriter(ofs);

        out.write("X (km),Y (km),Z (km),Latitude (deg),Longitude (deg),Radius (km),Nx,Ny,Nz,"
                + "Gravitational Acceleration X (m/s^2),Gravitational Acceleration Y (m/s^2),Gravitational Acceleration Z (m/s^2),"
                + "Gravitational Acceleration Magnitude (m/s^2),Gravitation Potential (J/kg),Elevation (meters),Slope (deg),"
                + "Tilt (deg)\n");

        double[] pt = new double[3];
        double[] normal = new double[3];
        String line;
        int i = 0;
        while ((line = in.readLine()) != null) {
            List<Double> row = new ArrayList<Double>();
            String[] tokens = line.trim().split("\\s+");
            pt[0] = Double.parseDouble(tokens[0]);
            pt[1] = Double.parseDouble(tokens[1]);
            pt[2] = Double.parseDouble(tokens[2]);
            boolean hasNormal = false;
            if (tokens.length >= 6) {
                normal[0] = Double.parseDouble(tokens[3]);
                normal[1] = Double.parseDouble(tokens[4]);
                normal[2] = Double.parseDouble(tokens[5]);
                hasNormal = true;
            }
            row.add(pt[0]);
            row.add(pt[1]);
            row.add(pt[2]);
            LatLon llr = MathUtil.reclat(pt).toDegrees();
            row.add(llr.lat);
            row.add(llr.lon);
            row.add(llr.rad);
            row.add(hasNormal ? normal[0] : null);
            row.add(hasNormal ? normal[1] : null);
            row.add(hasNormal ? normal[2] : null);
            row.add(results.get(i).acc[0]);
            row.add(results.get(i).acc[1]);
            row.add(results.get(i).acc[2]);
            // If no normal vector provided, just assume slope of zero
            double slope = 0.0;
            if (hasNormal)
                slope = getSlope(results.get(i).acc, normal);
            double elevation = getElevation(refPotential, results.get(i).acc, results.get(i).potential);
            double accMag = getAccelerationMagnitude(results.get(i).acc, slope);
            row.add(accMag);
            row.add(results.get(i).potential);
            row.add(elevation);
            row.add(hasNormal ? slope : null);
            Double tilt = null;
            if (hasNormal) {
                tilt = getTilt(pt, normal);
            }
            row.add(tilt);
            out.write(concatStringsWSep(row, ",") + "\n");
            ++i;
        }

        in.close();
        out.close();
    }

    private void saveResultsAtPointsInFitsFile(String inputfitsfile, vtkPolyData fitspolydata, String outputfitsfile,
            List<GravityValues> results) throws Exception {
        FileReader ifs = new FileReader(fieldpointsfile);
        BufferedReader in = new BufferedReader(ifs);

        // Get the dimensions of the input fits file
        int[] axes = new int[3];
        FitsUtil.loadFits(inputfitsfile, axes);

        double[][][] outdata = new double[19][axes[1]][axes[2]];
        vtkIdList idList = new vtkIdList();
        SmallBodyModel smallBodyModel = new SmallBodyModel(outputfitsfile, fitspolydata);

        double[] pt = new double[3];
        double[] normal = new double[3];
        String line;
        int i = 0;
        while ((line = in.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            pt[0] = Double.parseDouble(tokens[0]);
            pt[1] = Double.parseDouble(tokens[1]);
            pt[2] = Double.parseDouble(tokens[2]);
            normal[0] = Double.parseDouble(tokens[3]);
            normal[1] = Double.parseDouble(tokens[4]);
            normal[2] = Double.parseDouble(tokens[5]);
            int m = Integer.parseInt(tokens[6]);
            int n = Integer.parseInt(tokens[7]);
            LatLon llr = MathUtil.reclat(pt).toDegrees();
            outdata[0][m][n] = llr.lat;
            outdata[1][m][n] = llr.lon;
            outdata[2][m][n] = llr.rad;
            outdata[3][m][n] = pt[0];
            outdata[4][m][n] = pt[1];
            outdata[5][m][n] = pt[2];
            outdata[6][m][n] = normal[0];
            outdata[7][m][n] = normal[1];
            outdata[8][m][n] = normal[2];
            outdata[9][m][n] = results.get(i).acc[0];
            outdata[10][m][n] = results.get(i).acc[1];
            outdata[11][m][n] = results.get(i).acc[2];
            double slope = getSlope(results.get(i).acc, normal);
            double elevation = getElevation(refPotential, results.get(i).acc, results.get(i).potential);
            double accMag = getAccelerationMagnitude(results.get(i).acc, slope);
            outdata[12][m][n] = accMag;
            outdata[13][m][n] = results.get(i).potential;
            outdata[14][m][n] = elevation;
            outdata[15][m][n] = slope;
            double tilt = getTilt(pt, normal);
            outdata[16][m][n] = tilt;
            DescriptiveStatistics tiltStats = getTiltStatistics(smallBodyModel.getPointLocator(), fitspolydata, pt,
                    normal, idList);
            outdata[17][m][n] = tiltStats.getMean();
            outdata[18][m][n] = tiltStats.getStandardDeviation();
            ++i;
        }

        in.close();

        FitsUtil.saveFits(outdata, outfile, null);
    }

    private List<GravityValues> getGravityAtLocations() throws InterruptedException, ExecutionException, IOException {
        List<GravityValues> results = new ArrayList<GravityValues>();

        if (howToEvalute == HowToEvaluate.EVALUATE_AT_VERTICES) {
            results = getGravityAtShapeModelVertices();
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
            results = getGravityAtPlateCenters();
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_LIST) {
            results = getGravityAtPoints();
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_TEXT_FILE) {
            results = getGravityAtPoints();
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_POINTS_IN_FITS_FILE) {
            results = getGravityAtGrid();
        }

        if (howToEvalute == HowToEvaluate.EVALUATE_AT_VERTICES
                || howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
            refPotential = getRefPotential(results);
            System.out.println("Reference Potential = " + refPotential);
        }

        return results;
    }

    private double getRefPotential(List<GravityValues> results) {
        int numFaces = globalShapeModelPolyData.GetNumberOfCells();

        if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS && results.size() != numFaces) {
            System.err.println("Error: Size of array not equal to number of plates");
            System.exit(1);
        }
        else if (howToEvalute == HowToEvaluate.EVALUATE_AT_VERTICES
                && results.size() != globalShapeModelPolyData.GetNumberOfPoints()) {
            System.err.println("Error: Size of array not equal to number of vertices");
            System.exit(1);
        }

        vtkIdList idList = new vtkIdList();

        double[] pt1 = new double[3];
        double[] pt2 = new double[3];
        double[] pt3 = new double[3];
        double potTimesAreaSum = 0.0;
        double totalArea = 0.0;
        for (int i = 0; i < numFaces; ++i) {
            getCellPoints(globalShapeModelPolyData, i, idList, pt1, pt2, pt3);

            double potential = 0.0;
            if (howToEvalute == HowToEvaluate.EVALUATE_AT_CENTERS) {
                potential = results.get(i).potential;
            }
            else if (howToEvalute == HowToEvaluate.EVALUATE_AT_VERTICES) {
                // Average potential at 3 vertices
                int p1 = idList.GetId(0);
                int p2 = idList.GetId(1);
                int p3 = idList.GetId(2);
                potential = (results.get(p1).potential + results.get(p2).potential + results.get(p3).potential) / 3.0;
            }

            double area = MathUtil.triangleArea(pt1, pt2, pt3);

            potTimesAreaSum += potential * area;
            totalArea += area;
        }

        return potTimesAreaSum / totalArea;
    }

    private double getSlope(double[] acc, double[] normal) {
        double[] negativeAcc = new double[3];
        negativeAcc[0] = -acc[0];
        negativeAcc[1] = -acc[1];
        negativeAcc[2] = -acc[2];
        return Math.toDegrees(MathUtil.vsep(normal, negativeAcc));
    }

    public double getElevation(double refPotential, double[] acc, double potential) {
        double accMag = MathUtil.vnorm(acc);
        return (potential - refPotential) / accMag;
    }

    private double getAccelerationMagnitude(double[] acc, double slope) {
        double accMag = MathUtil.vnorm(acc);
        if (slope > 90.0)
            accMag = -Math.abs(accMag);
        else
            accMag = Math.abs(accMag);
        return accMag;
    }

    private double getGravityWithUnits(double[] fieldPoint, double[] acc) {
        double[] pt = fieldPoint;
        double potential = 1.0e6 * 1.0e12 * G * density * getGravity(pt, acc);

        acc[0] *= 1.0e3 * 1.0e12 * G * density;
        acc[1] *= 1.0e3 * 1.0e12 * G * density;
        acc[2] *= 1.0e3 * 1.0e12 * G * density;

        // add centrifugal force
        if (rotationRate != 0.0) {
            potential -= 1.0e6 * 0.5 * rotationRate * rotationRate * (pt[0] * pt[0] + pt[1] * pt[1]);
            acc[0] += 1.0e3 * rotationRate * rotationRate * pt[0];
            acc[1] += 1.0e3 * rotationRate * rotationRate * pt[1];
            // do nothing for z component
        }

        return potential;
    }

    private List<GravityValues> getGravityAtPlateCenters(int startId, int endId) {
        List<GravityValues> results = new ArrayList<GravityValues>();

        vtkIdList idList = new vtkIdList();

        for (int i = startId; i < endId; ++i) {
            globalShapeModelPolyData.GetCellPoints(i, idList);
            double[] pt1 = new double[3];
            double[] pt2 = new double[3];
            double[] pt3 = new double[3];
            int id1 = idList.GetId(0);
            int id2 = idList.GetId(1);
            int id3 = idList.GetId(2);
            globalShapeModelPolyData.GetPoint(id1, pt1);
            globalShapeModelPolyData.GetPoint(id2, pt2);
            globalShapeModelPolyData.GetPoint(id3, pt3);
            double[] center = new double[3];

            MathUtil.triangleCenter(pt1, pt2, pt3, center);

            GravityValues r = new GravityValues();
            r.potential = getGravityWithUnits(center, r.acc);

            results.add(r);
        }

        return results;
    }

    private List<GravityValues> getGravityAtPlateCenters() throws InterruptedException, ExecutionException {

        List<GravityValues> results = ParallelLoop.runParallelLoop(globalShapeModelPolyData.GetNumberOfCells(), new LoopFunction() {
            @Override
            public List<GravityValues> func(int startId, int stopId) {
                return getGravityAtPlateCenters(startId, stopId);
            }
        });

        return results;
    }

    private List<GravityValues> getGravityAtShapeModelVertices(int startId, int endId) {
        List<GravityValues> results = new ArrayList<GravityValues>();

        for (int i = startId; i < endId; ++i) {
            double[] pt = new double[3];
            globalShapeModelPolyData.GetPoint(i, pt);

            GravityValues r = new GravityValues();
            r.potential = getGravityWithUnits(pt, r.acc);

            results.add(r);
        }

        return results;
    }

    private List<GravityValues> getGravityAtShapeModelVertices() throws InterruptedException, ExecutionException {

        List<GravityValues> results = ParallelLoop.runParallelLoop(globalShapeModelPolyData.GetNumberOfPoints(), new LoopFunction() {
            @Override
            public List<GravityValues> func(int startId, int stopId) {
                return getGravityAtShapeModelVertices(startId, stopId);
            }
        });

        return results;
    }

    private List<GravityValues> getGravityAtPoints() {
        List<GravityValues> results = new ArrayList<GravityValues>();

        for (int i = 0; i < pointsToComputeGravity.size(); ++i) {
            double[] pt = pointsToComputeGravity.get(i);

            GravityValues r = new GravityValues();
            r.potential = getGravityWithUnits(pt, r.acc);

            results.add(r);
        }

        return results;
    }

    private GravityValues[][] getGravityAtGrid(double[][][] grid, int startRow, int stopRow) {
        int numRows = grid[0][0].length;
        int numCols = grid[0].length;
        GravityValues[][] results = new GravityValues[numRows][numCols];
        for (int m = startRow; m < stopRow; ++m)
            for (int n = 0; n < numCols; ++n) {
                double lat = grid[0][m][n];
                double lon = grid[1][m][n];
                double rad = grid[2][m][n];
                LatLon ll = new LatLon(lat * Math.PI / 180.0, lon * Math.PI / 180.0, rad);
                double[] pt = MathUtil.latrec(ll);
                GravityValues r = new GravityValues();
                r.potential = getGravityWithUnits(pt, r.acc);
                results[m][n] = r;
            }
        return results;
    }

    private List<GravityValues> getGravityAtGrid() throws InterruptedException, ExecutionException {
        int numRows = gridToComputeGravity[0][0].length;
        GravityValues[][] resultsGrid = ParallelGrid.runParallelGrid(numRows, new GridFunction() {
            @Override
            public GravityValues[][] func(int startRow, int stopRow) {
                return getGravityAtGrid(gridToComputeGravity, startRow, stopRow);
            }
        });

        // Convert 2D array to List
        List<GravityValues> results = new ArrayList<GravityValues>();
        int numCols = gridToComputeGravity[0].length;
        for (int m = 0; m < numRows; ++m)
            for (int n = 0; n < numCols; ++n) {
                results.add(resultsGrid[m][n]);
            }

        return results;
    }
}
