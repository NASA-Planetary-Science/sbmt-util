package edu.jhuapl.sbmt.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;;

public class ImgBackplanesFile implements BackplanesFile
{
    @Override
    public void write(float[] data, String source, String outputFile, int imageWidth, int imageHeight, int nBackplanes) throws IOException
    {
        int datasize = nBackplanes*imageHeight*imageWidth;
        OutputStream out = new FileOutputStream(outputFile);
        byte[] buf = new byte[4 * datasize];
        for (int i=0; i<datasize; ++i)
        {
            int v = Float.floatToIntBits(data[i]);
            buf[4*i + 0] = (byte)(v >>> 24);
            buf[4*i + 1] = (byte)(v >>> 16);
            buf[4*i + 2] = (byte)(v >>>  8);
            buf[4*i + 3] = (byte)(v >>>  0);
        }
        out.write(buf, 0, buf.length);
        out.close();
    }
}
