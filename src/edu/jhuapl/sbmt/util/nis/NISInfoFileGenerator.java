package edu.jhuapl.sbmt.util.nis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;

import com.google.common.collect.Maps;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client2.SbmtMultiMissionTool;
import edu.jhuapl.sbmt.config.SmallBodyViewConfig;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.client.Mission;
import edu.jhuapl.sbmt.model.SbmtModelFactory;
import edu.jhuapl.sbmt.model.eros.nis.NISSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;

public class NISInfoFileGenerator
{
    static Map<String,Vector3D> nisFileToSunPositionMap=Maps.newHashMap();

	 static
	    {
	        try
	        {
	            File nisSunFile=new File("/Users/steelrj1/Desktop/NIS Data/nisSunVectors.txt");

	            Scanner scanner=new Scanner(nisSunFile);
	            boolean found=false;
	            while (scanner.hasNextLine() && !found)
	            {
	                String line=scanner.nextLine();
	                String[] tokens=line.replaceAll(",", "").trim().split("\\s+");
	                String file=tokens[0];
	                String x=tokens[1];
	                String y=tokens[2];
	                String z=tokens[3];
	                nisFileToSunPositionMap.put(file,new Vector3D(Double.valueOf(x),Double.valueOf(y),Double.valueOf(z)).normalize());
	            }
	            scanner.close();
	        }
	        catch (IOException e)
	        {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }

	public NISInfoFileGenerator(String sourceDirectory, String destinationDirectory, String inputList, SmallBodyModel smallBodyModel) throws IOException
	{
		File inputFile = new File(sourceDirectory, inputList);
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		while (line != null && !line.equals(""))
		{
			String[] parts = line.split(" ");
			File spectrumFile = new File(sourceDirectory, parts[0]);

			System.out.println("NISInfoFileGenerator: NISInfoFileGenerator: spectrumFile " + spectrumFile.getAbsolutePath());

			NISSpectrum spectrum = new NISSpectrum(spectrumFile.getAbsolutePath(), (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel, SpectrumInstrumentFactory.getInstrumentForName("NIS"));
			spectrum.isCustomSpectra = true;
			spectrum.readSpectrumFromFile();

			File subDir = new File (destinationDirectory,  parts[0].substring(0, 4));
			subDir.mkdirs();
			File outputFile = new File(subDir, File.separator + FilenameUtils.getBaseName(spectrumFile.getAbsolutePath()) + ".INFO");

			System.out.println("NISInfoFileGenerator: NISInfoFileGenerator: output file " + outputFile.getAbsolutePath());

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

			DateTime dateTime = spectrum.getDateTime();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

	        String timeString = sdf.format(dateTime.toDate());
			double[] spacecraftPosition = spectrum.getSpacecraftPosition();
			double[] frustumCenter = spectrum.getFrustumCenter();
			double[] frustum1 = spectrum.getFrustum1();
			double[] frustum2 = spectrum.getFrustum2();
			double[] frustum3 = spectrum.getFrustum3();
			double[] frustum4 = spectrum.getFrustum4();
			double[] toSunUnitVector = nisFileToSunPositionMap.get(parts[0]).toArray();


			writer.write("START_TIME          = " + timeString);
			writer.newLine();
			writer.write("STOP_TIME           = " + timeString);
			writer.newLine();
			writer.write("SPACECRAFT_POSITION = ( " + spacecraftPosition[0] + " , " + spacecraftPosition[1] + " , " + spacecraftPosition[2] + " )");
			writer.newLine();
			writer.write("BORESIGHT_DIRECTION = ( " + frustumCenter[0] + " , " + frustumCenter[1] + " , " + frustumCenter[2] + " )");
			writer.newLine();
			writer.write("FRUSTUM1            = ( " + frustum1[0] + " , " + frustum1[1] + " , " + frustum1[2] + " )");
			writer.newLine();
			writer.write("FRUSTUM2            = ( " + frustum2[0] + " , " + frustum2[1] + " , " + frustum2[2] + " )");
			writer.newLine();
			writer.write("FRUSTUM3            = ( " + frustum3[0] + " , " + frustum3[1] + " , " + frustum3[2] + " )");
			writer.newLine();
			writer.write("FRUSTUM4            = ( " + frustum4[0] + " , " + frustum4[1] + " , " + frustum4[2] + " )");
			writer.newLine();
			writer.write("SUN_POSITION_LT     = ( " + toSunUnitVector[0] + " , " + toSunUnitVector[1] + " , " + toSunUnitVector[2] + " )");
			line = reader.readLine();
			writer.flush();
			writer.close();
		}
		reader.close();

	}

	public static void main(String[] args) throws IOException
	{
		String versionString = null;
		boolean aplVersion = true;
		String bodyName=ShapeModelBody.EROS.name();
	    String authorName=ShapeModelType.GASKELL.name();
//	        String rootURL = safeUrlPaths.getUrl("/disks/d0180/htdocs-sbmt/internal/sbmt");

		 // Important: set the mission before changing things in the Configuration. Otherwise,
        // setting the mission will undo those changes.
        SbmtMultiMissionTool.configureMission();

        // basic default configuration, most of these will be overwritten by the configureMission() method
        Configuration.setAPLVersion(aplVersion);
        Configuration.setRootURL("https://sbmt.jhuapl.edu/sbmt/stage/");

        // authentication
        Configuration.authenticate();

        // initialize view config
        SmallBodyViewConfig.fromServer = false;

        SmallBodyViewConfig.initialize();

        // VTK
        System.setProperty("java.awt.headless", "true");
        NativeLibraryLoader.loadHeadlessVtkLibraries();

        SmallBodyViewConfig config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EROS, ShapeModelType.GASKELL);
//        if (versionString != null)
//        	config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.valueOf(bodyName), ShapeModelType.provide(authorName), versionString);
//        else
//        	config = SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.valueOf(bodyName), ShapeModelType.provide(authorName));
//        DBRunInfo[] runInfos = config.databaseRunInfos;

        Mission mission = SbmtMultiMissionTool.getMission();

        SmallBodyModel smallBodyModel = SbmtModelFactory.createSmallBodyModel(config);

        File sourceDirectory = new File("/Users/steelrj1/Desktop/NIS Data/2000");
        File destinationDirectory = new File("/Users/steelrj1/Desktop/NIS Data/2000/Infofiles");
        File infoDir = new File(sourceDirectory, "info");

        NISInfoFileGenerator generator = new NISInfoFileGenerator(sourceDirectory.getAbsolutePath(), destinationDirectory.getAbsolutePath(), "nisTimes.txt", smallBodyModel);
	}

}
