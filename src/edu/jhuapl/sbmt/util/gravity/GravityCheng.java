package edu.jhuapl.sbmt.util.gravity;

import java.util.ArrayList;

import vtk.vtkIdList;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.util.MathUtil;

/**
 * This class computes gravitation potential and acceleration of a
 * closed triangular plate model using the approximation derived by A.
 * Cheng published in Cheng, A.F. et al., 2012, Efficient Calculation
 * of Effective Potential and Gravity on Small Bodies, ACM, 1667, p. 6447.
 * It is simpler and faster and the Werner and Scheeres method but not
 * as accurate.
 */
public class GravityCheng extends Gravity {

    private static class FaceData {
        double[] center = new double[3];
        double[] normal = new double[3]; // with length equal to twice plate
                                         // area
        int[] pointIds = new int[3];
    };

    private static class Point {
        double[] point = new double[3];
    }

    private ArrayList<FaceData> faceData = new ArrayList<FaceData>();
    private ArrayList<Point> pointCache = new ArrayList<Point>();

    public GravityCheng(vtkPolyData polyData) {
        super(polyData);

        // cache the points to avoid JNI access
        int numPoints = polyData.GetNumberOfPoints();
        for (int i = 0; i < numPoints; ++i) {
            Point p = new Point();
            polyData.GetPoint(i, p.point);
            pointCache.add(p);
        }

        vtkIdList idList = new vtkIdList();
        // Compute the face data
        int numFaces = polyData.GetNumberOfCells();
        for (int i = 0; i < numFaces; ++i) {
            FaceData fc = new FaceData();
            faceData.add(fc);

            polyData.GetCellPoints(i, idList);
            int p1 = idList.GetId(0);
            int p2 = idList.GetId(1);
            int p3 = idList.GetId(2);
            fc.pointIds[0] = p1;
            fc.pointIds[1] = p2;
            fc.pointIds[2] = p3;

            double[] pt1 = pointCache.get(p1).point;
            double[] pt2 = pointCache.get(p2).point;
            double[] pt3 = pointCache.get(p3).point;

            MathUtil.triangleCenter(pt1, pt2, pt3, fc.center);
            MathUtil.triangleNormal(pt1, pt2, pt3, fc.normal);
            double area = MathUtil.triangleArea(pt1, pt2, pt3);
            MathUtil.vscl(2.0 * area, fc.normal, fc.normal);
        }

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

        // Compute the edge data
        int numFaces = faceData.size();
        for (int i = 0; i < numFaces; ++i) {
            FaceData fc = faceData.get(i);

            double[] x_minus_R = new double[3];
            MathUtil.vsub(fieldPoint, fc.center, x_minus_R);

            double x_minus_R_dot_N = MathUtil.vdot(x_minus_R, fc.normal);
            double mag_x_minus_R = MathUtil.vnorm(x_minus_R);

            if (mag_x_minus_R == 0.0) {
                // No contribution to potential if we reach here. Only
                // acceleration.
                int p1 = fc.pointIds[0];
                int p2 = fc.pointIds[1];
                int p3 = fc.pointIds[2];

                double[] pt1 = pointCache.get(p1).point;
                double[] pt2 = pointCache.get(p2).point;
                double[] pt3 = pointCache.get(p3).point;

                double[] _2vjik = { 2.0 * pt2[0] - pt1[0] - pt3[0], 2.0 * pt2[1] - pt1[1] - pt3[1],
                        2.0 * pt2[2] - pt1[2] - pt3[2] };
                double[] _2vijk = { 2.0 * pt1[0] - pt2[0] - pt3[0], 2.0 * pt1[1] - pt2[1] - pt3[1],
                        2.0 * pt1[2] - pt2[2] - pt3[2] };
                double[] _2vkij = { 2.0 * pt3[0] - pt1[0] - pt2[0], 2.0 * pt3[1] - pt1[1] - pt2[1],
                        2.0 * pt3[2] - pt1[2] - pt2[2] };
                double factor = 3.0 / MathUtil.vnorm(_2vjik) + 3.0 / MathUtil.vnorm(_2vijk) + 3.0
                        / MathUtil.vnorm(_2vkij);

                acc[0] -= fc.normal[0] * factor;
                acc[1] -= fc.normal[1] * factor;
                acc[2] -= fc.normal[2] * factor;
            }
            else {
                potential += x_minus_R_dot_N / mag_x_minus_R;

                acc[0] -= ((fc.normal[0] - x_minus_R[0] * x_minus_R_dot_N / (mag_x_minus_R * mag_x_minus_R)) / mag_x_minus_R);
                acc[1] -= ((fc.normal[1] - x_minus_R[1] * x_minus_R_dot_N / (mag_x_minus_R * mag_x_minus_R)) / mag_x_minus_R);
                acc[2] -= ((fc.normal[2] - x_minus_R[2] * x_minus_R_dot_N / (mag_x_minus_R * mag_x_minus_R)) / mag_x_minus_R);
            }
        }

        potential *= 0.25;
        acc[0] *= 0.25;
        acc[1] *= 0.25;
        acc[2] *= 0.25;

        return potential;
    }

}
