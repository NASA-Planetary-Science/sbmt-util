package edu.jhuapl.sbmt.util;

import java.io.File;

import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.BPMetaBuilder;

import nom.tam.fits.FitsException;

/**
 * Interface for loading, filling out, or parsing PDS4 xml labels.
 * @author espirrc1
 *
 */
public interface BackPlanesPDS4XML
{

    /**
     * Use this method to use an existing PDS3 label as the source metadata on which to
     * describe a new PDS4 product.
     * initializes and returns the builder so that other methods can add to the builder;
     * @param pds3Fname
     * @return
     */
    public BPMetaBuilder pds3ToXmlMeta(String pds3Fname, String outXmlFname);

    /**
     * Use this method to use an existing PDS4 label as the source metadata on which to
     * describe a new PDS4 product.
     * initializes and returns the builder so that other methods can add to it.
     * @param pds4Fname
     * @return
     */
    public BPMetaBuilder pds4ToXmlMeta(String pds4Fname, String outXmlFname);

    /**
     * Method for adding information parsed from a FITS file to the builder.
     * Returns the builder so that other methods can add to it.
     * @param fitsFile
     * @param xmlMetaBuilder
     * @return
     * @throws FitsException
     */
    public BPMetaBuilder fitsToXmlMeta(File fitsFile, BPMetaBuilder xmlMetaBuilder) throws FitsException;

    /**
     * Generate XML document from XmlMetadata
     * @param metaData - metadata to be used in populating XmlDoc
     * @param xmlTemplate - path to XML template file
     */
    public BackPlanesXml metaToXmlDoc(BackPlanesXmlMeta metaData, String xmlTemplate);

}
