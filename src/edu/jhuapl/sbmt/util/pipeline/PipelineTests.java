//package edu.jhuapl.sbmt.util.pipeline;
//
//import java.awt.image.renderable.RenderableImage;
//import java.util.HashMap;
//import java.util.List;
//
//import org.apache.commons.lang3.tuple.Pair;
//import org.apache.commons.lang3.tuple.Triple;
//
//import com.google.common.collect.Lists;
//
//import vtk.vtkActor;
//
//import edu.jhuapl.saavtk.util.NativeLibraryLoader;
//import edu.jhuapl.sbmt.client.SmallBodyModel;
//import edu.jhuapl.sbmt.image.modules.io.builtIn.BuiltInFitsHeaderReader;
//import edu.jhuapl.sbmt.image.modules.io.builtIn.BuiltInFitsReader;
//import edu.jhuapl.sbmt.image.modules.io.builtIn.BuiltInOBJReader;
//import edu.jhuapl.sbmt.image.modules.io.builtIn.BuiltInVTKReader;
//import edu.jhuapl.sbmt.image.modules.pointing.InfofileReaderPublisher;
//import edu.jhuapl.sbmt.image.modules.pointing.SpiceBodyOperator;
//import edu.jhuapl.sbmt.image.modules.pointing.SpiceReaderPublisher;
//import edu.jhuapl.sbmt.image.modules.preview.VtkImagePreview;
//import edu.jhuapl.sbmt.image.modules.preview.VtkRendererPreview;
//import edu.jhuapl.sbmt.image.modules.preview.VtkRendererPreview2;
//import edu.jhuapl.sbmt.image.modules.rendering.LayerLinearInterpolaterOperator;
//import edu.jhuapl.sbmt.image.modules.rendering.LayerRotationOperator;
//import edu.jhuapl.sbmt.image.modules.rendering.RenderableImageGenerator;
//import edu.jhuapl.sbmt.image.modules.rendering.SceneBuilderOperator;
//import edu.jhuapl.sbmt.model.image.InfoFileReader;
//import edu.jhuapl.sbmt.pointing.spice.SpiceInfo;
//import edu.jhuapl.sbmt.pointing.spice.SpicePointingProvider;
//import edu.jhuapl.sbmt.util.TimeUtil;
//import edu.jhuapl.sbmt.util.pipeline.operator.IPipelineOperator;
//import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;
//import edu.jhuapl.sbmt.util.pipeline.publisher.Just;
//import edu.jhuapl.sbmt.util.pipeline.publisher.Publishers;
//import edu.jhuapl.sbmt.util.pipeline.subscriber.Sink;
//
//public class PipelineTests
//{
//
//	public static void main(String[] args) throws Exception
//	{
//		NativeLibraryLoader.loadAllVtkLibraries();
//		new PipelineTests();
//	}
//
//	public PipelineTests() throws Exception
//	{
////		test1();
////		test2();
////		test3();
////		test4();
//		test5();
//	}
//
//	private void test1() throws Exception
//	{
//		IPipelinePublisher<Layer> reader = new BuiltInFitsReader("/Users/steelrj1/Desktop/M0125990473F4_2P_IOF_DBL.FIT", new double[] {});
//		LayerLinearInterpolaterOperator linearInterpolator = new LayerLinearInterpolaterOperator(537, 412);
////		LayerMaskOperator maskOperator = new LayerMaskOperator(14, 14, 2, 2);
////		LayerTrimOperator trimOperator = new LayerTrimOperator(14, 14, 2, 2);
////		VtkImageRenderer renderer = new VtkImageRenderer();
////		VtkImageContrastOperator contrastOperator = new VtkImageContrastOperator(null);
////		VtkImageVtkMaskingOperator maskingOperator = new VtkImageVtkMaskingOperator(new int[] {0,0,0,0});
//		VtkImagePreview preview = new VtkImagePreview();
//
//		reader
//			.operate(linearInterpolator)
////			.operate(maskOperator)
////			.operate(trimOperator)
////			.operate(renderer)
////			.operate(contrastOperator)
////			.operate(maskingOperator)
//			.subscribe(preview)
//			.run();
//	}
//
//	private void test2() throws Exception
//	{
//		//***********************
//		//generate image layer
//		//***********************
//		IPipelinePublisher<Layer> reader = new BuiltInFitsReader("/Users/steelrj1/Desktop/M0125990473F4_2P_IOF_DBL.FIT", new double[] {});
//		LayerLinearInterpolaterOperator linearInterpolator = new LayerLinearInterpolaterOperator(537, 412);
//
//		List<Layer> updatedLayers = Lists.newArrayList();
//		reader
//			.operate(linearInterpolator)
//			.subscribe(Sink.of(updatedLayers)).run();
//
//		//generate image pointing (in: filename, out: ImagePointing)
//		IPipelinePublisher<InfoFileReader> pointingPublisher = new InfofileReaderPublisher("/Users/steelrj1/Desktop/M0125990473F4_2P_IOF_DBL.INFO");
//
//		//generate metadata (in: filename, out: ImageMetadata)
//		IPipelinePublisher<HashMap<String, String>> metadataReader = new BuiltInFitsHeaderReader("/Users/steelrj1/Desktop/M0125990473F4_2P_IOF_DBL.FIT");
//
//		//combine image source (in: Layer+ImageMetadata+ImagePointing, out: RenderableImage)
//		IPipelinePublisher<Layer> layerPublisher = new Just<Layer>(updatedLayers.get(0));
////		IPipelinePublisher<Object> imageComponents = Publishers.zip(layerPublisher, metadataReader, pointingPublisher);
//		IPipelinePublisher<Triple<Layer, HashMap<String, String>, InfoFileReader>> imageComponents = Publishers.formTriple(layerPublisher, metadataReader, pointingPublisher);
//
//		IPipelineOperator<Triple<Layer, HashMap<String, String>, InfoFileReader>, RenderableImage> renderableImageGenerator = new RenderableImageGenerator();
//
//
//		//***************************************************************************************
//		//generate image polydata with texture coords (in: RenderableImage, out: vtkPolydata)
//		//***************************************************************************************
//		List<RenderableImage> renderableImages = Lists.newArrayList();
//		imageComponents
//			.operate(renderableImageGenerator)
//			.subscribe(Sink.of(renderableImages)).run();
//
//		//***********************
//		//generate body polydata
//		//***********************
//		IPipelinePublisher<SmallBodyModel> vtkReader = new BuiltInVTKReader("/Users/steelrj1/.sbmt/cache/2/EROS/ver64q.vtk");
//
//		//*************************
//		//zip the sources together
//		//*************************
////		IPipelinePublisher<List<Object>> sceneObjects = Publishers.mergeLists(vtkReader, new Just<RenderableImage>(renderableImages.get(0)));
//		IPipelinePublisher<Pair<List<SmallBodyModel>, List<RenderableImage>>> sceneObjects = Publishers.formPair(Just.of(vtkReader.getOutputs()), Just.of(renderableImages));
//
//		//***************************************************************************
//		//Pass them into the scene builder to perform intersection calculations
//		//***************************************************************************
//		IPipelineOperator<Pair<List<SmallBodyModel>, List<RenderableImage>>, vtkActor> sceneBuilder = new SceneBuilderOperator();
//
//		//*******************************
//		//Throw them to the preview tool
//		//*******************************
//		VtkRendererPreview preview = new VtkRendererPreview(vtkReader.getOutputs().get(0));
//
//		sceneObjects
//			.operate(sceneBuilder) 	//feed the zipped sources to scene builder operator
//			.subscribe(preview)		//subscribe to the scene builder with the preview
//			.run();
//	}
//
//	private void test3() throws Exception
//	{
//		//***********************
//		//generate image layer
//		//***********************
//		IPipelinePublisher<Layer> reader = new BuiltInFitsReader("/Users/steelrj1/Desktop/dart_717891977_782_01.fits", new double[] {-32768.0, -32767.0, 4095.0});
//		LayerRotationOperator rotationOperator = new LayerRotationOperator();
//
//		List<Layer> updatedLayers = Lists.newArrayList();
//		reader
//			.operate(rotationOperator)
//			.subscribe(Sink.of(updatedLayers))
//			.run();
//
//		//generate image pointing (in: filename, out: ImagePointing)
//		IPipelinePublisher<InfoFileReader> pointingPublisher = new InfofileReaderPublisher("/Users/steelrj1/Desktop/dart_717891977_782_01.INFO");
//
//		//generate metadata (in: filename, out: ImageMetadata)
//		IPipelinePublisher<HashMap<String, String>> metadataReader = new BuiltInFitsHeaderReader("/Users/steelrj1/Desktop/dart_717891977_782_01.fits");
//
//		//combine image source (in: Layer+ImageMetadata+ImagePointing, out: RenderableImage)
//		IPipelinePublisher<Layer> layerPublisher = new Just<Layer>(updatedLayers.get(0));
////		IPipelinePublisher<Object> imageComponents = Publishers.zip(layerPublisher, metadataReader, pointingPublisher);
//		IPipelinePublisher<Triple<Layer, HashMap<String, String>, InfoFileReader>> imageComponents = Publishers.formTriple(layerPublisher, metadataReader, pointingPublisher);
////		System.out.println("PipelineTests: test3: number of image components " + imageComponents.getOutputs().size());
//		IPipelineOperator<Triple<Layer, HashMap<String, String>, InfoFileReader>, RenderableImage> renderableImageGenerator = new RenderableImageGenerator();
//
//
//		//***************************************************************************************
//		//generate image polydata with texture coords (in: RenderableImage, out: vtkPolydata)
//		//***************************************************************************************
//
//		List<RenderableImage> renderableImages = Lists.newArrayList();
//		imageComponents
//			.operate(renderableImageGenerator)
//			.subscribe(Sink.of(renderableImages))
//			.run();
//		//***********************
//		//generate body polydata
//		//***********************
//		IPipelinePublisher<SmallBodyModel> vtkReader = new BuiltInOBJReader(new String[]{"/Users/steelrj1/.sbmt1dart/cache/didymos/ideal-impact1-20200629-v01/shape/shape0.obj",
//					"/Users/steelrj1/.sbmt1dart/cache/dimorphos/ideal-impact1-20200629-v01/shape/shape0.obj"}, "DIDYMOS", "DIMORPHOS");
//
//		//*********************************
//		//Use SPICE to position the bodies
//		//*********************************
//		SpiceInfo spiceInfo = new SpiceInfo("DART", "920065803_FIXED", "DART_SPACECRAFT", "DIDYMOS", new String[] {"DIMORPHOS"}, new String[] {"DART_DRACO_2X2", "120065803_FIXED"});
//		IPipelinePublisher<SpicePointingProvider> pointingProviders = new SpiceReaderPublisher("/Users/steelrj1/dartspice/draco/impact.tm", spiceInfo, "DART_DRACO_2X2");
////		IPipelinePublisher<List<Object>> spiceBodyObjects = Publishers.mergeLists(vtkReader, pointingProviders);
//		IPipelinePublisher<Pair<SmallBodyModel, SpicePointingProvider>> spiceBodyObjects = Publishers.formPair(vtkReader, pointingProviders);
//		IPipelineOperator<Pair<SmallBodyModel, SpicePointingProvider>, SmallBodyModel> spiceBodyOperator = new SpiceBodyOperator("DIDYMOS", TimeUtil.str2et("2022-10-01T10:25:08.599"));
//		List<SmallBodyModel> updatedBodies = Lists.newArrayList();
//		spiceBodyObjects
//			.operate(spiceBodyOperator)
//			.subscribe(Sink.of(updatedBodies))
//			.run();
//
//
//		//*************************
//		//zip the sources together
//		//*************************
////		IPipelinePublisher<List<Object>> sceneObjects = Publishers.mergeLists(Just.of(updatedBodies), Just.of(renderableImages.get(0)));
//		IPipelinePublisher<Pair<List<SmallBodyModel>, List<RenderableImage>>> sceneObjects = Publishers.formPair(Just.of(updatedBodies), Just.of(renderableImages));
//
//		//***************************************************************************
//		//Pass them into the scene builder to perform intersection calculations
//		//***************************************************************************
//		IPipelineOperator<Pair<List<SmallBodyModel>, List<RenderableImage>>, vtkActor> sceneBuilder = new SceneBuilderOperator();
//
//		//*******************************
//		//Throw them to the preview tool
//		//*******************************
//		VtkRendererPreview preview = new VtkRendererPreview(vtkReader.getOutputs().get(0));
//
//		sceneObjects
//			.operate(sceneBuilder) 	//feed the zipped sources to scene builder operator
//			.subscribe(preview)		//subscribe to the scene builder with the preview
//			.run();
//	}
//
//	private void test4() throws Exception
//	{
//		IPipelinePublisher<Layer> reader = new BuiltInFitsReader("/Users/steelrj1/Desktop/dart_717891977_782_01.fits", new double[] {-32768.0, -32767.0, 4095.0});
//		VtkImagePreview preview = new VtkImagePreview();
//		reader
//			.subscribe(preview)
//			.run();
//	}
//
//	private void test5() throws Exception
//	{
//		SpiceInfo spiceInfo1 = new SpiceInfo("DART", "920065803_FIXED", "DART_SPACECRAFT", "DIDYMOS", new String[] {"DIMORPHOS"}, new String[] {"DART_DRACO_2X2", "120065803_FIXED"});
//		SpiceInfo spiceInfo2 = new SpiceInfo("DART", "120065803_FIXED", "DART_SPACECRAFT", "DIMORPHOS", new String[] {"DIDYMOS"}, new String[] {"DART_DRACO_2X2", "920065803_FIXED"});
//		SpiceInfo[] spiceInfos = new SpiceInfo[] {spiceInfo1, spiceInfo2};
//		String[] bodies = new String[]{"/Users/steelrj1/.sbmt1dart/cache/didymos/ideal-impact1-20200629-v01/shape/shape0.obj",
//			"/Users/steelrj1/.sbmt1dart/cache/dimorphos/ideal-impact1-20200629-v01/shape/shape0.obj"};
//		String[] bodyNames = new String[]{"DIDYMOS", "DIMORPHOS"};
//		String[] imageFiles = new String[] {"/Users/steelrj1/Desktop/dart_717891977_782_01.fits"};
//		String[] pointingFiles = new String[] {"/Users/steelrj1/Desktop/dart_717891977_782_01.INFO"};
//		VtkRendererPreview2 preview = new VtkRendererPreview2(imageFiles, pointingFiles, bodies, bodyNames, spiceInfos, "/Users/steelrj1/dartspice/draco/impact.tm", "DIDYMOS", "2022-10-01T10:25:08.599", "DART_DRACO_2X2");
//	}
//
//
//}