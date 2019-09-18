package edu.jhuapl.sbmt.util.nis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.sbmt.model.eros.nis.NIS;

public class NisSample
{
    final static NIS nis=new NIS();
    public static int spectrumLength = NIS.bandCentersLength;
    public static int timeStringLength = 23;

    int faceId;
    NisTime time;
    Vector3D toSun;
    Vector3D toSpacecraft;
    Vector3D normal;
    double[] spectrum;
    double weight;

    public NisSample(DataInputStream stream) throws IOException
    {
        faceId=stream.readInt();
        String timeString="";
        for (int i=0; i<timeStringLength; i++)
            timeString+=stream.readChar();
        time=new NisTime(timeString);
        toSun=new Vector3D(stream.readDouble(),stream.readDouble(),stream.readDouble());
        toSpacecraft=new Vector3D(stream.readDouble(),stream.readDouble(),stream.readDouble());
        normal=new Vector3D(stream.readDouble(),stream.readDouble(),stream.readDouble());
        weight=stream.readDouble();
        spectrum=new double[spectrumLength];
        for (int m=0; m<spectrumLength; m++)
            spectrum[m]=stream.readDouble();
    }

    public NisSample(int faceId, NisTime time, Vector3D normal, Vector3D toSun, Vector3D toSpacecraft, double[] spectrum, double weight)
    {
        this.faceId=faceId;
        this.time=time;
        this.toSun=toSun;
        this.toSpacecraft=toSpacecraft;
        this.normal=normal;
        this.spectrum=spectrum;
        this.weight=weight;
    }

    public void write(DataOutputStream stream) throws IOException
    {
        stream.writeInt(faceId);
        stream.writeChars(time.toString());
        stream.writeDouble(toSun.getX());
        stream.writeDouble(toSun.getY());
        stream.writeDouble(toSun.getZ());
        stream.writeDouble(toSpacecraft.getX());
        stream.writeDouble(toSpacecraft.getY());
        stream.writeDouble(toSpacecraft.getZ());
        stream.writeDouble(normal.getX());
        stream.writeDouble(normal.getY());
        stream.writeDouble(normal.getZ());
        stream.writeDouble(weight);
        for (int m=0; m<spectrumLength; m++)
            stream.writeDouble(spectrum[m]);
    }

}
