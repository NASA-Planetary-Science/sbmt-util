package edu.jhuapl.sbmt.util.nis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class NisSampleFile implements Iterable<NisSample>
{
    public static void write(Path file, List<NisSample> samples)
    {
        try
        {
            DataOutputStream stream=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile())));
            stream.writeInt(samples.size());
            write(samples, stream);
            stream.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void append(Path file, List<NisSample> samples)
    {
        try
        {
            DataInputStream stream=new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())));
            int nOldSamples=stream.readInt();
            stream.close();
            RandomAccessFile raf=new RandomAccessFile(file.toFile(), "rw");
            raf.writeInt(nOldSamples+samples.size());
            raf.close();
            DataOutputStream ostream=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile(),true)));
            write(samples,ostream);
            ostream.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void write(List<NisSample> sampleSet, DataOutputStream stream) throws IOException
    {
        for (int j=0; j<sampleSet.size(); j++)
            sampleSet.get(j).write(stream);
    }

    Path file;

    public NisSampleFile(Path file)
    {
        this.file=file;
        try
        {
            stream=new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())));
            nSamples=stream.readInt();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void resetStream()
    {
        try
        {
            stream.close();
            stream=new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())));
            stream.readInt();
        }
        catch (IOException e)
        {
        }
    }

    DataInputStream stream;
    int nSamples;

    @Override
    public Iterator<NisSample> iterator()
    {
        return new Iterator<NisSample>()
        {
            NisSample next;


            @Override
            public boolean hasNext()    // NOTE: calling hasNext more than once between calls to next() has the effect of advancing the stream; this might be an unexpected side effect but is probably outside most use cases
            {
                try
                {
                    next=new NisSample(stream);
                }
                catch (IOException e)
                {
                    try
                    {
                        stream.close();
                    }
                    catch (IOException e1)
                    {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    return false;
                }
                return true;
            }

            @Override
            public NisSample next()
            {
                return next;
            }

        };
    }

    public double getMaxIntensity()
    {
        double maxIntensity=Double.NEGATIVE_INFINITY;
        resetStream();
        Iterator<NisSample> iterator=iterator();
        while (iterator.hasNext())
        {
            NisSample sample=iterator.next();
            double intensity=0;
            for (int m=0; m<sample.spectrumLength; m++)
                intensity+=sample.spectrum[m];
            if (intensity>maxIntensity)
                maxIntensity=intensity;
        }
        return maxIntensity;
    }


}
