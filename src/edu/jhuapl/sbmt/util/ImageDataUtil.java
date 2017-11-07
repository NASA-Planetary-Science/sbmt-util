package edu.jhuapl.sbmt.util;

import vtk.vtkFloatArray;
import vtk.vtkImageData;
import vtk.vtkImageFlip;
import vtk.vtkImageReslice;
import vtk.vtkPointData;
import vtk.vtkTransform;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.util.VtkDataTypes;

public class ImageDataUtil
{
    public static final float FILL_CUTOFF = 1.e32f;

    /**
     * Helper function to create raw vtkImageData from float array
     *
     * @param depth Must be >= 1
     * @param array2D A matrix of floats with dimensions (height, width) or null
     * @param array3D A matrix of floats with dimensions (height, depth, width) or null
     * @param minValue Float array of length depth where min values at each layer will be stored
     * @param maxValue Float array of length depth where max values at each layer will be stored
     * @param fillDetector An object that identifies filler values, i.e., values that should not be displayed.
     */
    public static vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D, float[] minValue, float[] maxValue, FillDetector<Float> fillDetector)
    {
        vtkImageData image = new vtkImageData();
        if (transpose)
            image.SetDimensions(width, height, depth);
        else
            image.SetDimensions(height, width, depth);
        image.SetSpacing(1.0, 1.0, 1.0);
        image.SetOrigin(0.0, 0.0, 0.0);
        image.AllocateScalars(VtkDataTypes.VTK_FLOAT, 1);

        for (int k=0; k<depth; k++)
        {
            maxValue[k] = -Float.MAX_VALUE;
            minValue[k] = Float.MAX_VALUE;
        }

        // For performance, flatten out the 2D or 3D array into a 1D array and call
        // SetJavaArray directly on the pixel data since calling SetScalarComponentFromDouble
        // for every pixel takes too long.
        float[] array1D = new float[height * width * depth];

        for (int i=0; i<height; ++i)
            for (int j=0; j<width; ++j)
                for (int k=0; k<depth; k++)
                {
                    float value = 0.0f;
                    if (array2D != null)
                        value = array2D[i][j];
                    else if (array3D != null)
                        value = array3D[i][k][j];

                    // Convert "fill" values to NaN.
                    if (fillDetector.isFill(value))
                        value = Float.NaN;

                    if (transpose)
                        //image.SetScalarComponentFromDouble(j, height-1-i, k, 0, value);
                        array1D[(k * height + (height-1-i)) * width + j] = value;
                    else
                        //image.SetScalarComponentFromDouble(i, width-1-j, k, 0, value);
                        array1D[(k * width + (width-1-j)) * height + i] = value;

                    // Detect NaN and don't consider it for min/max value.
                    if (!Float.isFinite(value))
                        continue;

                    if (value > maxValue[k])
                        maxValue[k] = value;
                    if (value < minValue[k])
                        minValue[k] = value;
                }

        ((vtkFloatArray)image.GetPointData().GetScalars()).SetJavaArray(array1D);

        return image;
    }

    /**
     * Same as method above but without storing min and max values
     */
    public static vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D, FillDetector<Float> fillDetector)
    {
        // Unused
        float[] minValue = new float[depth];
        float[] maxValue = new float[depth];

        // Call helper
        return createRawImage(height, width, depth, transpose, array2D, array3D, minValue, maxValue, fillDetector);
    }

    /**
     * Same as methods above but with default fill detection.
     */
    public static vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D, float[] minValue, float[] maxValue)
    {
        return createRawImage(height, width, depth, transpose, array2D, array3D, minValue, maxValue, getDefaultFillDetector());
    }

    /**
     * Same as methods above but with default fill detection.
     */
    public static vtkImageData createRawImage(int height, int width, int depth, boolean transpose, float[][] array2D, float[][][] array3D)
    {
        return createRawImage(height, width, depth, transpose, array2D, array3D, getDefaultFillDetector());
    }


    public static FillDetector<Float> getDefaultFillDetector() {
        return new FillDetector<Float>() {

            @Override
            public boolean isFill(Float value)
            {
                return Float.compare(value, FILL_CUTOFF) >= 0 || Float.compare(value, -FILL_CUTOFF) <= 0;
            }

        };
    }


    /**
     * Accessing individual pixels of a vtkImageData is slow in java.
     * Therefore this function was written to allow converting a vtkImageData
     * to a java 3d array.  Returned 3d array has dimensions [depth][row][col]
     * @param image
     * @return
     */
    static public float[][][] vtkImageDataToArray3D(vtkImageData image)
    {
        int[] dims = image.GetDimensions();
        int width = dims[0];
        int height = dims[1];
        int depth = dims[2];
        vtkPointData pointdata = image.GetPointData();
        float[][][] array = new float[depth][height][width];
        Object dataObject = pointdata.GetScalars();
        if (dataObject instanceof vtkFloatArray)
        {
            vtkFloatArray data = (vtkFloatArray)dataObject;
            for(int k=0; k < depth; ++k)
            {
                for (int j=0; j < height; ++j)
                {
                    for (int i=0; i < width; ++i)
                    {
                        // calculate index
                        int index = k * width * height + j * width + i;
                        array[k][j][i] = (float)data.GetValue(index);
                    }
                }
            }
        }
        else if (dataObject instanceof vtkUnsignedCharArray)
        {
            vtkUnsignedCharArray data = (vtkUnsignedCharArray)dataObject;
            for(int k=0; k < depth; ++k)
            {
                for (int j=0; j < height; ++j)
                {
                    for (int i=0; i < width; ++i)
                    {
                        // calculate index
                        int index = k * width * height + j * width + i;
//                        char value = (char)data.GetValue(index);
//                        int ivalue = (int)value;
                        int ivalue = data.GetValue(index);
                        array[k][j][i] = (ivalue) / 255.0F;
                    }
                }
            }
        }

        return array;
    }

    /**
     * Accessing individual pixels of a vtkImageData is slow in java.
     * Therefore this function was written to allow converting a vtkImageData
     * to a java 2d array.
     * @param image
     * @return
     */
    static public float[][] vtkImageDataToArray2D(vtkImageData image)
    {
        int[] dims = image.GetDimensions();
        int height = dims[0];
        int width = dims[1];
        vtkPointData pointdata = image.GetPointData();
        vtkFloatArray data = (vtkFloatArray)pointdata.GetScalars();
        float[][] array = new float[height][width];
        int count = 0;
        for (int j=0; j < width; ++j)
            for (int i=0; i < height; ++i)
            {
                array[i][j] = (float)data.GetValue(count++);
            }

        return array;
    }

    /**
     * Version of vtkImageDataToArray2D for image cubes, returns a slice through the depth axis.
     *
     * @param image
     * @param slice
     * @return
     */
    static public float[][] vtkImageDataToArray2D(vtkImageData image, int slice)
    {
        int[] dims = image.GetDimensions();
        int height = dims[0];
        int width = dims[1];
        vtkPointData pointdata = image.GetPointData();
        vtkFloatArray data = (vtkFloatArray)pointdata.GetScalars();
        float[][] array = new float[height][width];
        for (int i=0; i < height; ++i)
            for (int j=0; j < width; ++j)
            {
                // calculate index
                int index = slice * width * height + j * height + i;
                array[i][j] = (float)data.GetValue(index);
            }

        return array;
    }

    /**
     * Version of vtkImageDataToArray1D for image cubes, returns a column along the depth axis.
     *
     * @param image
     * @param x
     * @param y
     * @return
     */
    static public float[] vtkImageDataToArray1D(vtkImageData image, int j, int i)
    {
        int[] dims = image.GetDimensions();
        int height = dims[0];
        int width = dims[1];
        int depth = dims[2];
        vtkPointData pointdata = image.GetPointData();
        vtkFloatArray data = (vtkFloatArray)pointdata.GetScalars();
        float[] array = new float[depth];
        for (int k=0; k < depth; ++k)
        {
            // calculate index
            int index = k * width * height + j * height + i;
            double value = data.GetValue(index);
            array[k] = (float)value;
        }

        return array;
    }

    /**
     * Given a 2D image of given length and width as well as texture coordinates
     * in the image (i.e. between 0 and 1) interpolate within the image to find
     * the pixel value at the texture coordinates using linear interpolation.
     * @param image
     * @param width
     * @param height
     * @param u
     * @param v
     * @return
     */
    static public float interpolateWithinImage(float[][] image, int width, int height, double u, double v)
    {
        final double x = u * (width - 1.0);
        final double y = v * (height - 1.0);
        final int x1 = (int)Math.floor(x);
        final int x2 = (int)Math.ceil(x);
        final int y1 = (int)Math.floor(y);
        final int y2 = (int)Math.ceil(y);

        // From http://en.wikipedia.org/wiki/Bilinear_interpolation
        final double value =
            image[x1][y1]*(x2-x)*(y2-y) +
            image[x2][y1]*(x-x1)*(y2-y) +
            image[x1][y2]*(x2-x)*(y-y1) +
            image[x2][y2]*(x-x1)*(y-y1);

        return (float)value;
    }

    /**
     *  Flip image along x axis.
     *
     * @param image
     */
    static public void flipImageXAxis(vtkImageData image)
    {
        int[] dims = image.GetDimensions();
        vtkImageFlip flip = new vtkImageFlip();
        flip.SetInputData(image);
        flip.SetInterpolationModeToNearestNeighbor();
        flip.SetOutputSpacing(1.0, 1.0, 1.0);
        flip.SetOutputOrigin(0.0, 0.0, 0.0);
        flip.SetOutputExtent(0, dims[1]-1, 0, dims[0]-1, 0, 0);
        flip.FlipAboutOriginOff();
        flip.SetFilteredAxes(0);
        flip.Update();

        vtkImageData flipOutput = flip.GetOutput();
        image.DeepCopy(flipOutput);
    }

    /**
     *  Flip image along y axis.
     *
     * @param image
     */
    static public void flipImageYAxis(vtkImageData image)
    {
        int[] dims = image.GetDimensions();
        vtkImageFlip flip = new vtkImageFlip();
        flip.SetInputData(image);
        flip.SetInterpolationModeToNearestNeighbor();
        flip.SetOutputSpacing(1.0, 1.0, 1.0);
        flip.SetOutputOrigin(0.0, 0.0, 0.0);
        flip.SetOutputExtent(0, dims[1]-1, 0, dims[0]-1, 0, 0);
        flip.FlipAboutOriginOff();
        flip.SetFilteredAxes(1);
        flip.Update();

        vtkImageData flipOutput = flip.GetOutput();
        image.DeepCopy(flipOutput);
    }

    /**
     *  Rotate image specified number of degrees
     *
     * @param image
     */
    static public void rotateImage(vtkImageData image, double angle)
    {
        int[] dims = image.GetDimensions();
        double[] center = {(dims[1]-1.0)/2.0, (dims[0]-1.0)/2.0};
//        double[] center = {(dims[0]-1.0)/2.0, (dims[1]-1.0)/2.0};

        vtkTransform imageTransform = new vtkTransform();
        imageTransform.PostMultiply();
        imageTransform.Translate(-center[1], -center[0], 0.0);
        imageTransform.RotateZ(angle);
        imageTransform.Translate(center[1], center[0], 0.0);

        vtkImageReslice algo = new vtkImageReslice();
        algo.SetInputData(image);
        algo.SetInformationInput(image);
        algo.SetResliceTransform(imageTransform);
        algo.SetInterpolationModeToNearestNeighbor();
        algo.SetOutputSpacing(1.0, 1.0, 1.0);
        algo.SetOutputOrigin(0.0, 0.0, 0.0);
//        algo.SetOutputExtent(0, dims[1]-1, 0, dims[0]-1, 0, 0);
        algo.SetOutputExtent(0, dims[0]-1, 0, dims[1]-1, 0, 0);
        algo.Update();

        vtkImageData output = algo.GetOutput();
        image.DeepCopy(output);
    }
}
