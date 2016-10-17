package edu.jhuapl.sbmt.util;

/**
 * Interface describing a backplanes file.
 *
 * @author nguyel1
 *
 */
public interface BackplanesFile
{
    /**
     * Write the backplanes to file.
     *
     * @param data - the 3D backplanes data
     * @param source - the name of the 2D image file from which the pixel values originated
     * @param outputFile - output backplanes file name
     * @param imageWidth - size of the first axis in the 2D image
     * @param imageHeight - size of the second axis in the 2D image
     * @param nBackplanes - number of backplanes
     * @throws Exception
     */
    public void write(float[] data, String source, String outputFile, int imageWidth, int imageHeight, int nBackplanes) throws Exception;
}
