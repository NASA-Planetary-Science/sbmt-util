package edu.jhuapl.sbmt.util.nis;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.model.eros.nis.NIS;
import edu.jhuapl.sbmt.model.eros.nis.NISSpectrum;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;

public class LocalNISSpectrum extends NISSpectrum
{
    private static NIS nis=new NIS();

    public LocalNISSpectrum(File nisFile, ISmallBodyModel smallBodyModel)
            throws IOException
    {
        super(nisFile.getAbsolutePath(), (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel, nis);
        serverpath=nisFile.toString();
    }

}
