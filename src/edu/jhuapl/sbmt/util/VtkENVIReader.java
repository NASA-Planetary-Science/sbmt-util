package edu.jhuapl.sbmt.util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

import vtk.vtkImageData;
import vtk.vtkImageReader2;

import edu.jhuapl.saavtk.util.ImageDataUtil;

// This class was written to be used like vtkPNGReader but
// only for converting ENVI format files to vtkImageData
// Note: ENVI format image = binary + .hdr file
public class VtkENVIReader extends vtkImageReader2
{
    // Internal storage
    private String binaryFilename;
    private vtkImageData vtkData;
    private HashMap<String, String> headerMap;
    private float[] minValues;
    private float[] maxValues;

    public VtkENVIReader()
    {
        super();

        // Make these null at first on purpose
        vtkData = null;
        headerMap = null;
    }

    // Save filename
    @Override
    public void SetFileName(String filename){
        binaryFilename = getBinaryFilename(filename);
    }

    @Override
    public void Update()
    {
        // Read the header if we have not done so already
        if(headerMap == null)
        {
            headerMap = readEnviHeader();
        }

        // Try reading the binary if we could read the header correctly
        if(headerMap != null && vtkData == null)
        {
            vtkData = readEnviBinary(headerMap);
        }
    }

    // Return a deep copy for user
    @Override
    public vtkImageData GetOutput()
    {
        vtkImageData copiedData = new vtkImageData();
        copiedData.DeepCopy(vtkData);
        return copiedData;
    }

    public int getNumBands()
    {
        // Read the header if we have not done so already
        if(headerMap == null)
        {
            headerMap = readEnviHeader();
        }

        // Get the number of bands from it
        return Integer.valueOf(headerMap.get("bands"));
    }

    public HashMap<String,String> readEnviHeader()
    {
        // Header file name is always binary + extension
        String headerFilename = binaryFilename + ".hdr";

        // Read the header file
        HashMap<String, String> headerMap = null;
        try
        {
            // Read in the header
            headerMap = readENVIHeader(headerFilename);

            // Check to see if all required entries exist
            if(headerMap.containsKey("samples") &&
                    headerMap.containsKey("lines") &&
                    headerMap.containsKey("bands") &&
                    headerMap.containsKey("header offset") &&
                    headerMap.containsKey("data type") &&
                    headerMap.containsKey("interleave") &&
                    headerMap.containsKey("byte order"))
            {
                // If so then return findings
                return headerMap;
            }
            else
            {
                // ENVI header missing at least one required field
                System.err.println("ENVI header missing at least 1 of 7 " +
                        "required fields: samples, lines, bands, " +
                        "header offset, data type, interleave, byte order");
            }
        }
        catch(Exception e)
        {
            // Print out error message
            System.err.println("VtkENVIReader: Error reading ENVI header: " + e.getMessage());
        }

        // If we reached this point then something went wrong, return null
        return null;
    }

    // Helper function to read the ENVI file
    private vtkImageData readEnviBinary(HashMap<String, String> headerMap)
    {
        try
        {
            // Parse the header fields
            int samples = Integer.valueOf(headerMap.get("samples"));
            int lines = Integer.valueOf(headerMap.get("lines"));
            int bands = Integer.valueOf(headerMap.get("bands"));
            int headerOffset = Integer.valueOf(headerMap.get("header offset"));
            String interleave = headerMap.get("interleave").toLowerCase();
            int byteOrder = Integer.valueOf(headerMap.get("byte order"));

            // Check if interleave type is recognized
            switch(interleave){
            case "bsq":
            case "bil":
            case "bip":
                break;
            default:
                System.err.println("Interleave type " + interleave + " unrecognized");
                return null;
            }

            // We only support data type 4 (float) for now
            float[][][] binaryData = null;
            int dataType = Integer.valueOf(headerMap.get("data type"));
            switch(dataType)
            {
            case 4:
                // float
                binaryData = readENVIBinary4(binaryFilename, samples, lines, bands,
                        headerOffset, interleave, byteOrder);
                break;
            default:
                // All others not supported for now
                System.err.println("Unsupported data type " + dataType);
                break;
            }

            // If successful at reading binary, create a rawImage out of it and return
            if(binaryData != null)
            {
                minValues = new float[bands];
                maxValues = new float[bands];
                return ImageDataUtil.createRawImage(lines, samples, bands, true, null, binaryData, minValues, maxValues);
            }
        }
        catch(IOException e)
        {
            // Something went wrong
            System.err.println("Error reading ENVI binary: " + e.getMessage());
        }

        // If we reached this point then something went wrong, return null
        return null;
    }

    // Helper function to make a map out of ENVI header entries
    private HashMap<String, String> readENVIHeader(String headerFilename) throws IOException
    {
        // Read the header file
        BufferedReader br = new BufferedReader(new FileReader(headerFilename));

        // Check if the first line says "ENVI"
        String line = br.readLine().trim();
        if(!line.equals("ENVI"))
        {
            // Not an ENVI file
            System.err.println("ENVI file header must start with \"ENVI\"");
            br.close();
            return null;
        }

        // Add each entry to a map
        HashMap<String, String> headerMap = new HashMap<String, String>();
        String[] parts;
        while((line = br.readLine()) != null)
        {
            // Expecting key = value structure
            parts = line.split("=");
            if(parts.length == 2)
            {
                // Put key and value pair into map
                headerMap.put(parts[0].trim(), parts[1].trim());
            }
            else
            {
                // Unrecognized line, skip it
                System.err.println("Unrecognized line: " + line);
            }
        }

        br.close();
        return headerMap;
    }

    // Helper function to read in ENVI binary when header "data type = 4" (float)
    private float[][][] readENVIBinary4(String binaryFilename, int samples, int lines, int bands,
            int headerOffset, String interleave, int byteOrder) throws IOException
    {
        // Set up channels
        ByteBuffer buf = ByteBuffer.allocate(4*samples*lines*bands); // Each float is 4 bytes
        if(byteOrder == 0)
        {
            // Little Endian = LSB stored first
            buf.order(ByteOrder.LITTLE_ENDIAN);
        }else if(byteOrder == 1){
            // Big Endian = MSB stored first
            buf.order(ByteOrder.BIG_ENDIAN);
        }else{
            // Unrecognized byte order
            System.err.println("Unrecognized byte order " + byteOrder);
            return null;
        }

        // Open up file and skip over the header offset # of bytes
        FileInputStream fs = new FileInputStream(binaryFilename);
        int skippedBytes = 0;
        while(skippedBytes < headerOffset)
        {
            // Need to do this since skip() may sometimes skip less than desired # of bytes
            skippedBytes += fs.skip(headerOffset - skippedBytes);
        }

        // Read data into the byte buffer
        FileChannel fc = fs.getChannel(); // fc starts after initial skipped bytes
        fc.read(buf);
        fc.close();
        fs.close();

        // Create a float buffer from read in bytebuffer
        buf.flip();
        FloatBuffer fb = buf.asFloatBuffer();

        // Allocate storage
        float[][][] parsedData = new float[lines][bands][samples];

        // Read bytes in order
        int bufferIndex = 0;
        switch(interleave)
        {
        case "bsq":
            // Band sequential: col, then row, then depth
            for (int band=0; band < bands; band++)
            {
                for(int line=0; line < lines; line++)
                {
                    for(int sample=0; sample<samples; sample++)
                    {
                        parsedData[line][band][sample] = fb.get(bufferIndex++);
                    }
                }
            }
            break;
        case "bil":
            // Band interleaved by line: col, then depth, then row
            for(int line=0; line < lines; line++)
            {
                for (int band=0; band < bands; band++)
                {
                    for(int sample=0; sample<samples; sample++)
                    {
                        parsedData[line][band][sample] = fb.get(bufferIndex++);
                    }
                }
            }
            break;
        case "bip":
            // Band interleaved by pixel: depth, then col, then row
            for(int line=0; line < lines; line++)
            {
                for(int sample=0; sample<samples; sample++)
                {
                    for (int band=0; band < bands; band++)
                    {
                        parsedData[line][band][sample] = fb.get(bufferIndex++);
                    }
                }
            }
            break;
        }

        // Return the parsed data
        return parsedData;
    }

    public float[] getMinValues()
    {
        return minValues;
    }

    public float[] getMaxValues()
    {
        return maxValues;
    }

    // Whether a filename is consistent with that of an ENVI header
    public static boolean isENVIHeader(String filename){
        return filename.endsWith(".hdr");
    }

    // Whether a filename is consistent with that of an ENVI binary
    public static boolean isENVIBinary(String filename){
        // ENVI binaries do NOT have any file extension
        return !filename.matches("(.*)\\.[a-zA-Z0-9]*$");
    }

    // Whether a filename is consistent with that of an ENVI binary or header file
    public static boolean isENVIFilename(String filename){
        return isENVIHeader(filename) || isENVIBinary(filename);
    }

    // Get the corresponding ENVI binary filename
    public static String getBinaryFilename(String filename)
    {
        if(isENVIHeader(filename))
        {
            return filename.substring(0, filename.length()-4);
        }
        else if (isENVIBinary(filename))
        {
            return filename;
        }
        else
        {
            System.err.println("Filename " + filename +
                    " does not match ENVI header or binary");
            return null;
        }
    }

    // Get the corresponding ENVI header filename
    public static String getHeaderFilename(String filename)
    {
        if(isENVIHeader(filename))
        {
            return filename;
        }
        else if(isENVIBinary(filename))
        {
            return filename + ".hdr";
        }
        else
        {
            System.err.println("Filename " + filename +
                    " does not match ENVI header or binary");
            return null;
        }
    }

    // Check to see if both files exist
    public static boolean checkFilesExist(String filename)
    {
        // Get filenames of header and binary
        String headerFilename = getHeaderFilename(filename);
        String binaryFilename = getBinaryFilename(filename);

        // Create files
        if(headerFilename == null || binaryFilename == null)
        {
            return false;
        }
        else
        {
            // Make sure both files exist and are readable
            File headerFile = new File(headerFilename);
            File binaryFile = new File(binaryFilename);
            return headerFile.exists() && headerFile.canRead() && headerFile.isFile() &&
                binaryFile.exists() && binaryFile.canRead() && binaryFile.isFile();
        }
    }
}
