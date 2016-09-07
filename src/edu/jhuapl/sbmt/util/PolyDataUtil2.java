package edu.jhuapl.sbmt.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkGenericCell;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.saavtk.util.IdPair;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;

import altwg.util.FitsUtil;

/**
 * This class contains various utility functions for operating on a vtkPolyData.
 *
 * @author kahneg1
 * @version 1.0
 *
 */
public class PolyDataUtil2 {

    public static final float INVALID_VALUE = -1.0e38f;

    /**
     * Read in a FITS shape model with format where the first 3 planes are lat,
     * lon, and radius.
     *
     * @param filename
     * @return
     * @throws Exception
     */
    static public vtkPolyData loadGlobalFitsLLRModel(String filename) throws Exception {
        int latIndex = 0;
        int lonIndex = 1;
        int radIndex = 2;

        int[] axes = new int[3];
        double[][][] data = FitsUtil.loadFits(filename, axes);

        vtkPolyData body = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray polys = new vtkCellArray();
        body.SetPoints(points);
        body.SetPolys(polys);

        int numRows = axes[1];
        int numCols = axes[2];

        int count = 0;
        int[][] indices = new int[numRows][numCols];
        for (int m = 0; m < numRows; ++m)
            for (int n = 0; n < numCols; ++n) {
                double lat = data[latIndex][m][n];
                double lon = data[lonIndex][m][n];
                double rad = data[radIndex][m][n];

                // Only include 1 point at each pole and don't include any
                // points
                // at longitude 360 since it's the same as longitude 0
                if ((m == 0 && n > 0) || (m == numRows - 1 && n > 0) || (n == numCols - 1)) {
                    indices[m][n] = -1;
                }
                else {
                    indices[m][n] = count++;
                    LatLon ll = new LatLon(lat * Math.PI / 180.0, lon * Math.PI / 180.0, rad);
                    double[] pt = MathUtil.latrec(ll);
                    points.InsertNextPoint(pt);
                }
            }

        // Now add connectivity information
        int i0, i1, i2, i3;
        vtkIdList idList = new vtkIdList();
        idList.SetNumberOfIds(3);
        for (int m = 0; m <= numRows - 2; ++m)
            for (int n = 0; n <= numCols - 2; ++n) {
                // Add triangles touching south pole
                if (m == 0) {
                    i0 = indices[m][0]; // index of south pole point
                    i1 = indices[m + 1][n];
                    if (n == numCols - 2)
                        i2 = indices[m + 1][0];
                    else
                        i2 = indices[m + 1][n + 1];

                    if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
                        idList.SetId(0, i0);
                        idList.SetId(1, i1);
                        idList.SetId(2, i2);
                        polys.InsertNextCell(idList);
                    }
                    else {
                        System.out.println("Error occurred");
                    }

                }
                // Add triangles touching north pole
                else if (m == numRows - 2) {
                    i0 = indices[m + 1][0]; // index of north pole point
                    i1 = indices[m][n];
                    if (n == numCols - 2)
                        i2 = indices[m][0];
                    else
                        i2 = indices[m][n + 1];

                    if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
                        idList.SetId(0, i0);
                        idList.SetId(1, i1);
                        idList.SetId(2, i2);
                        polys.InsertNextCell(idList);
                    }
                    else {
                        System.out.println("Error occurred");
                    }
                }
                // Add middle triangles that do not touch either pole
                else {
                    // Get the indices of the 4 corners of the rectangle to the
                    // upper right
                    i0 = indices[m][n];
                    i1 = indices[m + 1][n];
                    if (n == numCols - 2) {
                        i2 = indices[m][0];
                        i3 = indices[m + 1][0];
                    }
                    else {
                        i2 = indices[m][n + 1];
                        i3 = indices[m + 1][n + 1];
                    }

                    // Add upper left triangle
                    if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
                        idList.SetId(0, i0);
                        idList.SetId(1, i1);
                        idList.SetId(2, i2);
                        polys.InsertNextCell(idList);
                    }
                    else {
                        System.out.println("Error occurred");
                    }

                    // Add bottom right triangle
                    if (i2 >= 0 && i1 >= 0 && i3 >= 0) {
                        idList.SetId(0, i2);
                        idList.SetId(1, i1);
                        idList.SetId(2, i3);
                        polys.InsertNextCell(idList);
                    }
                    else {
                        System.out.println("Error occurred");
                    }
                }
            }

        // Add normal vectors
        vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
        normalsFilter.SetInputData(body);
        normalsFilter.SetComputeCellNormals(0);
        normalsFilter.SetComputePointNormals(1);
        normalsFilter.SplittingOff();
        normalsFilter.ConsistencyOn();
        normalsFilter.AutoOrientNormalsOn(); // We want this on in case of a bug
                                             // in the ordering of the vertices
                                             // of the triangles earlier in
                                             // this function
        normalsFilter.Update();

        vtkPolyData normalsOutput = normalsFilter.GetOutput();
        body.ShallowCopy(normalsOutput);

        normalsFilter.Delete();

        return body;
    }

    static public vtkPolyData loadLocalFitsLLRModel(String filename) throws Exception {
        int latIndex = 0;
        int lonIndex = 1;
        int radIndex = 2;

        vtkIdList idList = new vtkIdList();
        vtkPolyData dem = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        vtkCellArray polys = new vtkCellArray();
        dem.SetPoints(points);
        dem.SetPolys(polys);

        int[] axes = new int[3];
        double[][][] data = FitsUtil.loadFits(filename, axes);

        int liveSizeX = axes[1];
        int liveSizeY = axes[2];

        int[][] indices = new int[liveSizeX][liveSizeY];
        int c = 0;
        int i0, i1, i2, i3;

        // First add points to the vtkPoints array
        for (int m = 0; m < liveSizeX; ++m)
            for (int n = 0; n < liveSizeY; ++n) {
                indices[m][n] = -1;

                double lat = data[latIndex][m][n];
                double lon = data[lonIndex][m][n];
                double rad = data[radIndex][m][n];

                boolean valid = (lat != INVALID_VALUE && lon != INVALID_VALUE && rad != INVALID_VALUE);

                if (valid) {

                    LatLon ll = new LatLon(lat * Math.PI / 180.0, lon * Math.PI / 180.0, rad);
                    double[] pt = MathUtil.latrec(ll);
                    points.InsertNextPoint(pt);

                    indices[m][n] = c;

                    ++c;
                }
            }

        idList.SetNumberOfIds(3);

        // Now add connectivity information
        for (int m = 1; m < liveSizeX; ++m)
            for (int n = 1; n < liveSizeY; ++n) {
                // Get the indices of the 4 corners of the rectangle to the
                // upper left
                i0 = indices[m - 1][n - 1];
                i1 = indices[m][n - 1];
                i2 = indices[m - 1][n];
                i3 = indices[m][n];

                // Add upper left triangle
                if (i0 >= 0 && i1 >= 0 && i2 >= 0) {
                    idList.SetId(0, i0);
                    idList.SetId(1, i1);
                    idList.SetId(2, i2);
                    polys.InsertNextCell(idList);
                }
                // Add bottom right triangle
                if (i2 >= 0 && i1 >= 0 && i3 >= 0) {
                    idList.SetId(0, i2);
                    idList.SetId(1, i1);
                    idList.SetId(2, i3);
                    polys.InsertNextCell(idList);
                }
            }

        vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
        normalsFilter.SetInputData(dem);
        normalsFilter.SetComputeCellNormals(0);
        normalsFilter.SetComputePointNormals(1);
        normalsFilter.SplittingOff();
        normalsFilter.ConsistencyOn();
        normalsFilter.AutoOrientNormalsOff();
        if (needToFlipMapletNormalVectors(dem))
            normalsFilter.FlipNormalsOn();
        else
            normalsFilter.FlipNormalsOff();
        normalsFilter.Update();

        vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
        dem.DeepCopy(normalsFilterOutput);

        return dem;
    }

    /**
     * Compute the mean normal vector over the entire vtkPolyData by averaging
     * all the normal vectors of all cells.
     */
    public static double[] computeMeanPolyDataNormal(vtkPolyData polyData) {

        vtkIdList idList = new vtkIdList();

        // Average the normals
        double[] normal = { 0.0, 0.0, 0.0 };
        double[] pt0 = new double[3];
        double[] pt1 = new double[3];
        double[] pt2 = new double[3];
        double[] n = new double[3];

        int numCells = polyData.GetNumberOfCells();
        for (int i = 0; i < numCells; ++i) {
            polyData.GetCellPoints(i, idList);
            polyData.GetPoint(idList.GetId(0), pt0);
            polyData.GetPoint(idList.GetId(1), pt1);
            polyData.GetPoint(idList.GetId(2), pt2);
            MathUtil.triangleNormal(pt0, pt1, pt2, n);
            normal[0] += n[0];
            normal[1] += n[1];
            normal[2] += n[2];
        }

        normal[0] /= numCells;
        normal[1] /= numCells;
        normal[2] /= numCells;

        idList.Delete();

        return normal;
    }

    /**
     * Compute the centroid of all the points in the polydata.
     */
    public static double[] computePolyDataCentroid(vtkPolyData polyData) {
        // Average the normals
        double[] centroid = { 0.0, 0.0, 0.0 };

        int numPoints = polyData.GetNumberOfPoints();
        double[] p = new double[3];
        for (int i = 0; i < numPoints; ++i) {
            polyData.GetPoint(i, p);
            centroid[0] += p[0];
            centroid[1] += p[1];
            centroid[2] += p[2];
        }

        centroid[0] /= numPoints;
        centroid[1] /= numPoints;
        centroid[2] /= numPoints;

        return centroid;
    }

    static public boolean needToFlipMapletNormalVectors(vtkPolyData polydata) {

        double[] normal = computeMeanPolyDataNormal(polydata);
        double[] centroid = computePolyDataCentroid(polydata);
        MathUtil.vhat(normal, normal);
        MathUtil.vhat(centroid, centroid);
        return MathUtil.vdot(centroid, normal) < 0.0;
    }

    static public void convertFitsLLRModelToAscii(String fitsfilename, String outasciifile, boolean convertToXYZ,
            boolean localFits) throws Exception {
        int latIndex = 0;
        int lonIndex = 1;
        int radIndex = 2;

        int[] axes = new int[3];
        double[][][] data = FitsUtil.loadFits(fitsfilename, axes);

        int numRows = axes[1];
        int numCols = axes[2];

        FileWriter os = new FileWriter(outasciifile);
        BufferedWriter out = new BufferedWriter(os);

        int c = 0;
        vtkPolyData polydata = null;
        if (localFits)
            polydata = loadLocalFitsLLRModel(fitsfilename);
        else
            polydata = loadGlobalFitsLLRModel(fitsfilename);

        for (int m = 0; m < numRows; ++m)
            for (int n = 0; n < numCols; ++n) {
                double lat = data[latIndex][m][n];
                double lon = data[lonIndex][m][n];
                double rad = data[radIndex][m][n];

                double[] normal = null;
                // When converting a global model, we only included 1 point at
                // each pole and didn't include any points at longitude 360
                // since
                // it's the same as longitude 0. Therefore to find the normal
                // vector
                // to this point, it seems simplest to just search for it. This
                // is slow
                // but it's only for a few points.
                if (!localFits && ((m == 0 && n > 0) || (m == numRows - 1 && n > 0) || (n == numCols - 1))) {
                    lon = 0.0;
                    LatLon ll = new LatLon(lat * Math.PI / 180.0, lon * Math.PI / 180.0, rad);
                    double[] p = MathUtil.latrec(ll);
                    int idx = polydata.FindPoint(p);
                    if (idx < 0)
                        System.out.println("Error: Could not find closest point");
                    normal = polydata.GetPointData().GetNormals().GetTuple3(idx);
                }
                else {
                    normal = polydata.GetPointData().GetNormals().GetTuple3(c++);
                }

                if (convertToXYZ) {
                    LatLon ll = new LatLon(lat * Math.PI / 180.0, lon * Math.PI / 180.0, rad);
                    double[] p = MathUtil.latrec(ll);
                    out.write(String.format("%.16e %.16e %.16e %.16e %.16e %.16e %d %d\n", p[0], p[1], p[2], normal[0],
                            normal[1], normal[2], m, n));
                }
                else {
                    out.write(String.format("%.16e %.16e %.16e %.16e %.16e %.16e %d %d\n", lat, lon, rad, normal[0],
                            normal[1], normal[2], m, n));
                }
            }

        out.close();
    }

    /**
     * Saves out plate centers and plate normal vectors of poly data. Each line
     * in output file contains these 6 values separated by spaces.
     *
     * <pre>
     * 1. x plate center
     * 2. y plate center
     * 3. z plate center
     * 4. x normal
     * 5. y normal
     * 6. z normal
     * </pre>
     *
     * @param polydata
     * @param outasciifile
     * @throws IOException
     */
    static public void savePlateCentersOfPolyData(vtkPolyData polydata, String outasciifile) throws IOException {
        FileWriter fstream = new FileWriter(outasciifile);
        BufferedWriter out = new BufferedWriter(fstream);

        vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
        normalsFilter.SetInputData(polydata);
        normalsFilter.SetComputeCellNormals(1);
        normalsFilter.SetComputePointNormals(0);
        normalsFilter.SplittingOff();
        normalsFilter.ConsistencyOn();
        normalsFilter.AutoOrientNormalsOff();
        normalsFilter.Update();
        vtkDataArray normals = normalsFilter.GetOutput().GetCellData().GetNormals();

        int numberCells = polydata.GetNumberOfCells();
        vtkIdList idList = new vtkIdList();

        double[] pt0 = new double[3];
        double[] pt1 = new double[3];
        double[] pt2 = new double[3];
        double[] c = new double[3];
        double[] n = new double[3];

        for (int i = 0; i < numberCells; ++i) {
            polydata.GetCellPoints(i, idList);
            polydata.GetPoint(idList.GetId(0), pt0);
            polydata.GetPoint(idList.GetId(1), pt1);
            polydata.GetPoint(idList.GetId(2), pt2);
            MathUtil.triangleCenter(pt0, pt1, pt2, c);
            n = normals.GetTuple3(i);
            out.write(c[0] + " " + c[1] + " " + c[2] + " " + n[0] + " " + n[1] + " " + n[2] + "\r\n");
        }

        out.close();
    }

    static public void adjustShapeModelToOtherShapeModel(vtkPolyData frompolydata, vtkPolyData topolydata)
            throws Exception {
        vtkPoints points = frompolydata.GetPoints();
        int numberPoints = frompolydata.GetNumberOfPoints();

        double diagonalLength = new BoundingBox(topolydata.GetBounds()).getDiagonalLength();

        vtksbCellLocator cellLocator = new vtksbCellLocator();
        cellLocator.SetDataSet(topolydata);
        cellLocator.CacheCellBoundsOn();
        cellLocator.AutomaticOn();
        cellLocator.BuildLocator();

        vtkGenericCell cell = new vtkGenericCell();
        double tol = 1e-6;
        double[] t = new double[1];
        double[] intersectPoint = new double[3];
        double[] pcoords = new double[3];
        int[] subId = new int[1];
        int[] cell_id = new int[1];

        double[] p = new double[3];
        double[] startPt = new double[] { 0.0, 0.0, 0.0 };
        for (int i = 0; i < numberPoints; ++i) {
            points.GetPoint(i, p);

            double[] radialVector = new double[3];
            MathUtil.vhat(p, radialVector);
            double[] lookPt = new double[] { diagonalLength * radialVector[0], diagonalLength * radialVector[1],
                    diagonalLength * radialVector[2] };

            int result = cellLocator.IntersectWithLine(startPt, lookPt, tol, t, intersectPoint, pcoords, subId,
                    cell_id, cell);

            if (result <= 0) {
                throw new Exception("Error: no intersection");
            }

            points.SetPoint(i, intersectPoint);
        }

    }

    /**
     * The following function was adapted from from the file
     * Wm5PolyhedralMassProperties.cpp in the Geometric Tools source code
     * (http://www.geometrictools.com)
     *
     * @param polydata
     * @return mass
     */
    public static double getMassProperties(vtkPolyData polydata, double[] center, double[][] inertiaWorld,
            double[][] inertiaCOM) {
        int numberOfCells = polydata.GetNumberOfCells();
        vtkIdList idList = new vtkIdList();

        double[] v0 = new double[3];
        double[] v1 = new double[3];
        double[] v2 = new double[3];

        final double oneDiv6 = 1.0 / 6.0;
        final double oneDiv24 = 1.0 / 24.0;
        final double oneDiv60 = 1.0 / 60.0;
        final double oneDiv120 = 1.0 / 120.0;

        // order: 1, x, y, z, x^2, y^2, z^2, xy, yz, zx
        double[] integral = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double[] V1mV0 = new double[3];
        double[] V2mV0 = new double[3];
        double[] N = new double[3];

        for (int i = 0; i < numberOfCells; ++i) {
            polydata.GetCellPoints(i, idList);
            int id0 = idList.GetId(0);
            int id1 = idList.GetId(1);
            int id2 = idList.GetId(2);
            polydata.GetPoint(id0, v0);
            polydata.GetPoint(id1, v1);
            polydata.GetPoint(id2, v2);

            // Get cross product of edges and normal vector.
            MathUtil.vsub(v1, v0, V1mV0);
            MathUtil.vsub(v2, v0, V2mV0);
            MathUtil.vcrss(V1mV0, V2mV0, N);
            // Vector3<Real> V1mV0 = v1 - v0;
            // Vector3<Real> V2mV0 = v2 - v0;
            // Vector3<Real> N = V1mV0.Cross(V2mV0);

            // Compute integral terms.
            double tmp0, tmp1, tmp2;
            double f1x, f2x, f3x, g0x, g1x, g2x;
            tmp0 = v0[0] + v1[0];
            f1x = tmp0 + v2[0];
            tmp1 = v0[0] * v0[0];
            tmp2 = tmp1 + v1[0] * tmp0;
            f2x = tmp2 + v2[0] * f1x;
            f3x = v0[0] * tmp1 + v1[0] * tmp2 + v2[0] * f2x;
            g0x = f2x + v0[0] * (f1x + v0[0]);
            g1x = f2x + v1[0] * (f1x + v1[0]);
            g2x = f2x + v2[0] * (f1x + v2[0]);

            double f1y, f2y, f3y, g0y, g1y, g2y;
            tmp0 = v0[1] + v1[1];
            f1y = tmp0 + v2[1];
            tmp1 = v0[1] * v0[1];
            tmp2 = tmp1 + v1[1] * tmp0;
            f2y = tmp2 + v2[1] * f1y;
            f3y = v0[1] * tmp1 + v1[1] * tmp2 + v2[1] * f2y;
            g0y = f2y + v0[1] * (f1y + v0[1]);
            g1y = f2y + v1[1] * (f1y + v1[1]);
            g2y = f2y + v2[1] * (f1y + v2[1]);

            double f1z, f2z, f3z, g0z, g1z, g2z;
            tmp0 = v0[2] + v1[2];
            f1z = tmp0 + v2[2];
            tmp1 = v0[2] * v0[2];
            tmp2 = tmp1 + v1[2] * tmp0;
            f2z = tmp2 + v2[2] * f1z;
            f3z = v0[2] * tmp1 + v1[2] * tmp2 + v2[2] * f2z;
            g0z = f2z + v0[2] * (f1z + v0[2]);
            g1z = f2z + v1[2] * (f1z + v1[2]);
            g2z = f2z + v2[2] * (f1z + v2[2]);

            // Update integrals.
            integral[0] += N[0] * f1x;
            integral[1] += N[0] * f2x;
            integral[2] += N[1] * f2y;
            integral[3] += N[2] * f2z;
            integral[4] += N[0] * f3x;
            integral[5] += N[1] * f3y;
            integral[6] += N[2] * f3z;
            integral[7] += N[0] * (v0[1] * g0x + v1[1] * g1x + v2[1] * g2x);
            integral[8] += N[1] * (v0[2] * g0y + v1[2] * g1y + v2[2] * g2y);
            integral[9] += N[2] * (v0[0] * g0z + v1[0] * g1z + v2[0] * g2z);
        }

        integral[0] *= oneDiv6;
        integral[1] *= oneDiv24;
        integral[2] *= oneDiv24;
        integral[3] *= oneDiv24;
        integral[4] *= oneDiv60;
        integral[5] *= oneDiv60;
        integral[6] *= oneDiv60;
        integral[7] *= oneDiv120;
        integral[8] *= oneDiv120;
        integral[9] *= oneDiv120;

        // mass
        double mass = integral[0];

        // center of mass
        center[0] = integral[1] / mass;
        center[1] = integral[2] / mass;
        center[2] = integral[3] / mass;

        // inertia relative to world origin
        inertiaWorld[0][0] = integral[5] + integral[6];
        inertiaWorld[0][1] = -integral[7];
        inertiaWorld[0][2] = -integral[9];
        inertiaWorld[1][0] = inertiaWorld[0][1];
        inertiaWorld[1][1] = integral[4] + integral[6];
        inertiaWorld[1][2] = -integral[8];
        inertiaWorld[2][0] = inertiaWorld[0][2];
        inertiaWorld[2][1] = inertiaWorld[1][2];
        inertiaWorld[2][2] = integral[4] + integral[5];

        // inertia relative to center of mass
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
                inertiaCOM[i][j] = inertiaWorld[i][j];
        inertiaCOM[0][0] -= mass * (center[1] * center[1] + center[2] * center[2]);
        inertiaCOM[0][1] += mass * center[0] * center[1];
        inertiaCOM[0][2] += mass * center[2] * center[0];
        inertiaCOM[1][0] = inertiaCOM[0][1];
        inertiaCOM[1][1] -= mass * (center[2] * center[2] + center[0] * center[0]);
        inertiaCOM[1][2] += mass * center[1] * center[2];
        inertiaCOM[2][0] = inertiaCOM[0][2];
        inertiaCOM[2][1] = inertiaCOM[1][2];
        inertiaCOM[2][2] -= mass * (center[0] * center[0] + center[1] * center[1]);

        return mass;
    }

    public static class PolyDataStatistics {
        public int numberPlates;
        public int numberVertices;
        public int numberEdges;
        public int enumberDuplicateVertices;
        public double surfaceArea;
        public double meanCellArea;
        public double minCellArea;
        public double maxCellArea;
        public double stdCellArea;
        public double varCellArea;
        public double meanEdgeLength;
        public double minEdgeLength;
        public double maxEdgeLength;
        public double stdEdgeLength;
        public double varEdgeLength;
        public boolean isClosed;
        public BoundingBox boundingBox = new BoundingBox();
        public int eulerPolyhedronFormula;
        public double[][] inertiaCOM = new double[3][3];
        public double[][] inertiaWorld = new double[3][3];
        public double[] centroid = new double[3];
        public double volume;
    }

    public static PolyDataStatistics getPolyDataStatistics(vtkPolyData polydata) {

        PolyDataStatistics statistics = new PolyDataStatistics();

        // First determine if the shape model is closed
        vtkPolyData boundary = new vtkPolyData();
        // The boundary of a closed surface is empty
        PolyDataUtil.getBoundary(polydata, boundary);
        statistics.isClosed = (boundary.GetNumberOfCells() == 0);

        polydata.BuildCells();
        vtkIdList idList = new vtkIdList();

        int numberOfCells = polydata.GetNumberOfCells();

        double[] pt0 = new double[3];
        double[] pt1 = new double[3];
        double[] pt2 = new double[3];
        HashSet<IdPair> edges = new LinkedHashSet<IdPair>();

        DescriptiveStatistics areaStatistics = new DescriptiveStatistics();
        for (int i = 0; i < numberOfCells; ++i) {
            polydata.GetCellPoints(i, idList);
            int id0 = idList.GetId(0);
            int id1 = idList.GetId(1);
            int id2 = idList.GetId(2);
            polydata.GetPoint(id0, pt0);
            polydata.GetPoint(id1, pt1);
            polydata.GetPoint(id2, pt2);

            double area = MathUtil.triangleArea(pt0, pt1, pt2);
            areaStatistics.addValue(area);

            IdPair edge0 = id0 < id1 ? new IdPair(id0, id1) : new IdPair(id1, id0);
            IdPair edge1 = id1 < id2 ? new IdPair(id1, id2) : new IdPair(id2, id1);
            IdPair edge2 = id2 < id0 ? new IdPair(id2, id0) : new IdPair(id0, id2);
            edges.add(edge0);
            edges.add(edge1);
            edges.add(edge2);
        }

        DescriptiveStatistics edgeStatistics = new DescriptiveStatistics();
        for (IdPair edge : edges) {
            polydata.GetPoint(edge.id1, pt0);
            polydata.GetPoint(edge.id2, pt1);
            double length = MathUtil.distanceBetween(pt0, pt1);
            edgeStatistics.addValue(length);
        }

        if (statistics.isClosed) {
            statistics.volume = getMassProperties(polydata, statistics.centroid, statistics.inertiaWorld,
                    statistics.inertiaCOM);

            // For debugging, print out this information to make sure it agrees
            // with our implementation
            // vtkMassProperties massProp = new vtkMassProperties();
            // massProp.SetInputData(polydata);
            // massProp.Update();
            //
            // System.out.println("Surface Area = " +
            // massProp.GetSurfaceArea());
            // System.out.println("Volume = " + massProp.GetVolume());
            // System.out.println("Mean Plate Area = " +
            // massProp.GetSurfaceArea() / polydata.GetNumberOfCells());
            // System.out.println("Min Plate Area = " +
            // massProp.GetMinCellArea());
            // System.out.println("Max Plate Area = " +
            // massProp.GetMaxCellArea());
        }

        statistics.eulerPolyhedronFormula = polydata.GetNumberOfPoints() - edges.size() + numberOfCells;

        polydata.ComputeBounds();
        statistics.boundingBox.setBounds(polydata.GetBounds());

        statistics.numberPlates = numberOfCells;
        statistics.numberVertices = polydata.GetNumberOfPoints();
        statistics.numberEdges = edges.size();
        statistics.surfaceArea = areaStatistics.getSum();
        statistics.meanCellArea = areaStatistics.getMean();
        statistics.minCellArea = areaStatistics.getMin();
        statistics.maxCellArea = areaStatistics.getMax();
        statistics.stdCellArea = areaStatistics.getStandardDeviation();
        statistics.varCellArea = areaStatistics.getVariance();
        statistics.meanEdgeLength = edgeStatistics.getMean();
        statistics.minEdgeLength = edgeStatistics.getMin();
        statistics.maxEdgeLength = edgeStatistics.getMax();
        statistics.stdEdgeLength = edgeStatistics.getStandardDeviation();
        statistics.varEdgeLength = edgeStatistics.getVariance();

        return statistics;
    }

    /**
     * Given a vtkPolyData containing only triangles, return a new vtkPolyData
     * whose points are the centers of the triangles of the vtkPolyData passed
     * to the function. The returned poly data contains no cells, only points
     */
    public static vtkPolyData getPlateCenters(vtkPolyData polyData) {
        vtkPolyData outPolyData = new vtkPolyData();
        vtkPoints points = new vtkPoints();
        outPolyData.SetPoints(points);
        vtkIdList idList = new vtkIdList();
        double[] pt0 = new double[3];
        double[] pt1 = new double[3];
        double[] pt2 = new double[3];
        double[] center = new double[3];
        int numCells = polyData.GetNumberOfCells();
        for (int i = 0; i < numCells; ++i) {

            polyData.GetCellPoints(i, idList);

            if (idList.GetNumberOfIds() != 3) {
                System.err.println("Error: Cells must have exactly 3 vertices!");
                continue;
            }

            polyData.GetPoint(idList.GetId(0), pt0);
            polyData.GetPoint(idList.GetId(1), pt1);
            polyData.GetPoint(idList.GetId(2), pt2);

            MathUtil.triangleCenter(pt0, pt1, pt2, center);

            points.InsertNextPoint(center);
        }
        return outPolyData;
    }
}
