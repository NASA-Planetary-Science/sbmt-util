package edu.jhuapl.sbmt.util;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhuapl.sbmt.model.eros.MSIFits;

import altwg.Fits.HeaderTags;
import nom.tam.fits.HeaderCard;

public class FitsBackplanesFile implements BackplanesFile
{
    @Override
    public void write(float[] data, String source, String outputFile, int imageWidth,
            int imageHeight, int nBackplanes) throws Exception
    {
        //Convert from float[] to double[][][]
        int i = 0;
        double[][][] imgData = new double[nBackplanes][imageHeight][imageWidth]; //double[][][] data = new double[numPlanes][numLines][numSamples];
        for (int pp = 0; pp < nBackplanes; pp++)
        {
            for (int ll = 0; ll < imageHeight; ll++)
            {
                for (int ss = 0; ss < imageWidth; ss++)
                {
                    // System.out.printf("%d, %d, %d\n", pp, ll, ss);
                    imgData[pp][ll][ss] = data[i];
                    i++;
                }
            }
        }

        write(imgData, source, outputFile, nBackplanes);
    }

    public void write(double[][][] imgData, String source, String outputFile, int nBackplanes) throws Exception
    {
        List<BackplaneInfo> planeList = BackplaneInfo.getPlanes(nBackplanes);

        Map<String, HeaderCard> prevHeaderValues = new HashMap<String, HeaderCard>();

        //parse input filename. use as value for fits keyword DATASRCF
        String fileName = Paths.get(outputFile).getFileName().toString();

        HeaderCard fitsHdrCard = new HeaderCard(HeaderTags.DATASRCF.toString(), source, null);
        prevHeaderValues.put(HeaderTags.DATASRCF.toString(), fitsHdrCard);

        //parse output fits name. use as value for fits keyword PRODNAME
        fileName = Paths.get(outputFile).getFileName().toString();
        fitsHdrCard = new HeaderCard(HeaderTags.PRODNAME.toString(), fileName, null);
        prevHeaderValues.put(HeaderTags.PRODNAME.toString(), fitsHdrCard);

        MSIFits.savePlanes2Fits(imgData, planeList, outputFile, prevHeaderValues);
    }
 }
