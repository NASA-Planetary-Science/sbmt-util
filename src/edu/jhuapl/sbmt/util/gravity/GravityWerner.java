package edu.jhuapl.sbmt.util.gravity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import vtk.vtkIdList;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.util.MathUtil;

/**
 * This class computes gravitation potential and acceleration of a
 * closed triangular plate model using the method of Werner as described in
 * Werner R. A. and D. J. Scheeres (1997) CeMDA, 65, 313-344.
 */
public class GravityWerner extends Gravity {
    private static class EdgeKey {
        int p1;
        int p2;

        @Override
        public boolean equals(Object obj) {
            EdgeKey key = (EdgeKey) obj;
            return p1 == key.p1 && p2 == key.p2;
        }

        @Override
        public int hashCode() {
            // This hash seems to produce efficient look ups. Not sure what
            // the best hash is though.
            return p1;
        }
    };

    private static class EdgeData {
        double[][] E = new double[3][3];
        double edgeLength;
        int p1;
        int p2;
    };

    private static class FaceData {
        double[][] F = new double[3][3];
        int[] pointIds = new int[3];
    };

    private static class Point {
        double[] point = new double[3];
    }

    private static class PointData {
        double[] r = new double[3];
        double r_mag;
    }

    private List<EdgeData> edgeData = new ArrayList<EdgeData>();
    private List<FaceData> faceData = new ArrayList<FaceData>();
    private List<Point> pointCache = new ArrayList<Point>();

    private static void addMatrices(double[][] a, double[][] b, double[][] c) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
                c[i][j] = a[i][j] + b[i][j];
    }

    private static void multiply3x3(double[][] A, double[] v, double[] u) {
        u[0] = A[0][0] * v[0] + A[0][1] * v[1] + A[0][2] * v[2];
        u[1] = A[1][0] * v[0] + A[1][1] * v[1] + A[1][2] * v[2];
        u[2] = A[2][0] * v[0] + A[2][1] * v[1] + A[2][2] * v[2];
    }

    private static double abs(double a) {
        return (a <= 0.0) ? 0.0 - a : a;
    }

    private static void outer(double[] x, double[] y, double[][] A) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                A[i][j] = x[i] * y[j];
    }


    /*
    // For debugging
    private static void printmatrix(String str, double[][] m)
    {
        System.out.println(str + " : ");
        for (int i=0; i<3; ++i)
        {
            for (int j=0; j<3; ++j)
                System.out.print(m[i][j] + " ");
            System.out.println();
        }
    }
     */


    public GravityWerner(vtkPolyData polyData) {
        super(polyData);

        // cache the points to avoid JNI access
        int numPoints = polyData.GetNumberOfPoints();
        for (int i = 0; i < numPoints; ++i) {
            Point p = new Point();
            polyData.GetPoint(i, p.point);
            pointCache.add(p);
        }

        LinkedHashMap<EdgeKey, EdgeData> edgeDataMap = new LinkedHashMap<EdgeKey, EdgeData>();

        vtkIdList idList = new vtkIdList();

        int numFaces = polyData.GetNumberOfCells();
        // Compute the edge data
        for (int i = 0; i < numFaces; ++i) {
            polyData.GetCellPoints(i, idList);

            int[] pointIds = new int[3];
            pointIds[0] = idList.GetId(0);
            pointIds[1] = idList.GetId(1);
            pointIds[2] = idList.GetId(2);

            double[] pt1 = pointCache.get(pointIds[0]).point;
            double[] pt2 = pointCache.get(pointIds[1]).point;
            double[] pt3 = pointCache.get(pointIds[2]).point;

            double[] cellNormal = new double[3];
            MathUtil.triangleNormal(pt1, pt2, pt3, cellNormal);

            for (int j = 0; j < 3; ++j) {
                int p1;
                int p2;
                if (j < 2) {
                    p1 = pointIds[j];
                    p2 = pointIds[j + 1];
                }
                else {
                    p1 = pointIds[2];
                    p2 = pointIds[0];
                }

                // Put the point with the lowest id into ed so that
                // the 2 identical edges always have the same point
                EdgeKey key = new EdgeKey();
                if (p1 < p2) {
                    key.p1 = p1;
                    key.p2 = p2;
                }
                else {
                    key.p1 = p2;
                    key.p2 = p1;
                }

                EdgeData ed;
                if (edgeDataMap.containsKey(key)) {
                    ed = edgeDataMap.get(key);
                }
                else {
                    // If key not found
                    ed = new EdgeData();
                    ed.E[0][0] = ed.E[0][1] = ed.E[0][2] = 0.0;
                    ed.E[1][0] = ed.E[1][1] = ed.E[1][2] = 0.0;
                    ed.E[2][0] = ed.E[2][1] = ed.E[2][2] = 0.0;
                    ed.edgeLength = 0.0;
                    ed.p1 = key.p1;
                    ed.p2 = key.p2;
                    // it =
                    // edgeDataMap.insert(pair<EdgeKey,EdgeData>(key,ed)).first;
                    edgeDataMap.put(key, ed);
                }

                // Compute unit vector from p1 to p2
                double[] edgeUnitVector = new double[3];
                pt1 = pointCache.get(p1).point;
                pt2 = pointCache.get(p2).point;
                MathUtil.vsub(pt2, pt1, edgeUnitVector);
                ed.edgeLength = MathUtil.unorm(edgeUnitVector, edgeUnitVector);
                // Compute half of the E dyad
                double[] edgeNormal = new double[3];
                MathUtil.vcrss(edgeUnitVector, cellNormal, edgeNormal);

                double[][] E = new double[3][3];
                outer(cellNormal, edgeNormal, E);

                addMatrices(ed.E, E, ed.E);
            }
        }

        // Now convert the edgeDataMap to a vector
        edgeData = new ArrayList<EdgeData>(edgeDataMap.values());

        // Compute the face data
        for (int i = 0; i < numFaces; ++i) {
            FaceData fd = new FaceData();
            faceData.add(fd);

            polyData.GetCellPoints(i, idList);
            fd.pointIds[0] = idList.GetId(0);
            fd.pointIds[1] = idList.GetId(1);
            fd.pointIds[2] = idList.GetId(2);

            double[] pt1 = pointCache.get(fd.pointIds[0]).point;
            double[] pt2 = pointCache.get(fd.pointIds[1]).point;
            double[] pt3 = pointCache.get(fd.pointIds[2]).point;

            // Compute the F dyad
            double[] normal = new double[3];
            MathUtil.triangleNormal(pt1, pt2, pt3, normal);
            outer(normal, normal, fd.F);
        }
    }

    private double compute_wf(FaceData fd, PointData[] pointData) {
        PointData pd1 = pointData[fd.pointIds[0]];
        PointData pd2 = pointData[fd.pointIds[1]];
        PointData pd3 = pointData[fd.pointIds[2]];

        double[] cross = new double[3];
        MathUtil.vcrss(pd2.r, pd3.r, cross);

        double numerator = MathUtil.vdot(pd1.r, cross);
        double denominator = pd1.r_mag * pd2.r_mag * pd3.r_mag + pd1.r_mag * MathUtil.vdot(pd2.r, pd3.r) + pd2.r_mag
                * MathUtil.vdot(pd3.r, pd1.r) + pd3.r_mag * MathUtil.vdot(pd1.r, pd2.r);

        // TODO do a better comparison. Do not hard code 1e-9
        if (abs(numerator) < 1e-9)
            numerator = -0.0;

        return 2.0 * Math.atan2(numerator, denominator);
    }

    private double compute_Le(EdgeData ed, PointData[] pointData) {
        PointData pd1 = pointData[ed.p1];
        PointData pd2 = pointData[ed.p2];

        if (abs(pd1.r_mag + pd2.r_mag - ed.edgeLength) < 1e-9) {
            return 0.0;
        }

        return Math.log((pd1.r_mag + pd2.r_mag + ed.edgeLength) / (pd1.r_mag + pd2.r_mag - ed.edgeLength));
    }

    /**
     * Note this function is optimized for speed and avoids all JNI calls.
     *
     * @param fieldPoint
     *            input point at which to compute acceleration and potential
     * @param acc
     *            this is filled with the returned acceleration
     * @return potential
     */
    @Override
    public double getGravity(double[] fieldPoint, double[] acc) {
        double potential = 0.0;
        acc[0] = 0.0;
        acc[1] = 0.0;
        acc[2] = 0.0;

        // Cache all the vectors from field point to vertices and their
        // magnitudes
        int numPoints = pointCache.size();
        PointData[] pointData = new PointData[numPoints];
        for (int i = 0; i < numPoints; ++i) {
            double[] point = pointCache.get(i).point;
            PointData pd = new PointData();
            MathUtil.vsub(point, fieldPoint, pd.r);
            pd.r_mag = MathUtil.vnorm(pd.r);
            pointData[i] = pd;
        }

        double[] Er = new double[3];
        double rEr;
        double[] Fr = new double[3];
        double rFr;

        int numEdges = edgeData.size();
        for (int i = 0; i < numEdges; ++i) {
            EdgeData ed = edgeData.get(i);

            // Any vertex of the cell will do, so just choose the first one.
            PointData pd = pointData[ed.p1];

            double Le = compute_Le(ed, pointData);

            multiply3x3(ed.E, pd.r, Er);
            rEr = MathUtil.vdot(pd.r, Er);
            potential -= (rEr * Le);

            acc[0] -= Er[0] * Le;
            acc[1] -= Er[1] * Le;
            acc[2] -= Er[2] * Le;
        }

        int numFaces = faceData.size();
        for (int i = 0; i < numFaces; ++i) {
            FaceData fd = faceData.get(i);

            // Any vertex of the cell will do, so just choose the first one.
            PointData pd = pointData[fd.pointIds[0]];

            double wf = compute_wf(fd, pointData);

            multiply3x3(fd.F, pd.r, Fr);
            rFr = MathUtil.vdot(pd.r, Fr);

            potential += (rFr * wf);

            acc[0] += Fr[0] * wf;
            acc[1] += Fr[1] * wf;
            acc[2] += Fr[2] * wf;
        }

        return 0.5 * potential;
    }

    public boolean isInsidePolyhedron(double[] fieldPoint) {
        // Cache all the vectors from field point to vertices and their
        // magnitudes
        int numPoints = pointCache.size();
        PointData[] pointData = new PointData[numPoints];
        for (int i = 0; i < numPoints; ++i) {
            double[] point = pointCache.get(i).point;
            PointData pd = new PointData();
            MathUtil.vsub(point, fieldPoint, pd.r);
            pd.r_mag = MathUtil.vnorm(pd.r);
            pointData[i] = pd;
        }

        double sum = 0.0;
        int numFaces = faceData.size();
        for (int i = 0; i < numFaces; ++i) {
            FaceData fd = faceData.get(i);
            sum += compute_wf(fd, pointData);
        }

        // This sum is equal to 4*pi if the point is inside the polyhedron and
        // equals zero when outside.
        return sum >= 2.0 * Math.PI;
    }

}
