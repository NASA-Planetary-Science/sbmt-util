package edu.jhuapl.sbmt.util.hololens;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import vtk.vtkCellDataToPointData;
import vtk.vtkDoubleArray;
import vtk.vtkFloatArray;
import vtk.vtkPNGReader;
import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

import edu.jhuapl.saavtk.colormap.Colormap;
import edu.jhuapl.saavtk.colormap.Colormaps;
import edu.jhuapl.saavtk.model.ColoringData;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.saavtk.util.ObjUtil;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class HololensEros
{

    static
    {
        NativeLibraryLoader.loadVtkLibraries();
        SmallBodyViewConfig.initialize();
    }

    public static void main(String[] args) throws IOException
    {
        int coloringIndex=2; // at the time of implementation index 2 is point-centered elevation data

        SmallBodyViewConfig config=SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EROS, ShapeModelType.GASKELL);
        SmallBodyModel erosModel = SbmtModelFactory.createSmallBodyModel(config).get(0);
        erosModel.setColoringIndex(coloringIndex);

        Colormap colormap=Colormaps.getNewInstanceOfBuiltInColormap("Spectral_lowBlue");
        colormap.setRangeMin(0);
        colormap.setRangeMax(1);
        BufferedImage image=new BufferedImage(1, colormap.getNumberOfLevels(), BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<colormap.getNumberOfLevels(); i++)
        {
            double lev=(double)i/(double)(colormap.getNumberOfLevels()-1);
            image.setRGB(0, i, colormap.getColor(lev).getRGB());
        }
        Path pngPath=Paths.get("/Users/zimmemi1/Desktop/Spectral_lowBlue.png");
        ImageIO.write(image, "png", pngPath.toFile());

        vtkPNGReader reader=new vtkPNGReader();
        reader.SetFileName(pngPath.toString());
        reader.Update();

        // 2018-05-22 This code block was modified to work with the new classes
        // ColoringData and ColoringDataManager instead of the now removed ColoringInfo class.
        // However, because of other runtime errors, these changes were not tested. If this code
        // is used again, there may be side-effects, but that should turn up in routine testing.
        int numberElements = erosModel.getSmallBodyPolyData().GetNumberOfCells();
        ColoringData coloringData = erosModel.getColoringDataManager().get(numberElements).get(coloringIndex);
        coloringData.load();
        double[] coloringRange=coloringData.getDefaultRange();
        vtkDoubleArray values=new vtkDoubleArray();
        values.SetName("values");
        for (int i=0; i<numberElements; i++)
        {
            double scaledValue=(coloringData.getData().GetTuple1(i)-coloringRange[0])/(coloringRange[1]-coloringRange[0]);
            if (scaledValue<0)
                scaledValue=0;
            if (scaledValue>1)
                scaledValue=1;
            values.InsertNextValue(scaledValue);
        }
        erosModel.getSmallBodyPolyData().GetCellData().AddArray(values);

        vtkCellDataToPointData converter=new vtkCellDataToPointData();
        converter.SetInputData(erosModel.getSmallBodyPolyData());
        converter.Update();


        vtkPolyData polyData=converter.GetPolyDataOutput();
        vtkFloatArray texCoords=new vtkFloatArray();
        texCoords.SetNumberOfComponents(2);
        vtkDoubleArray pointValues=(vtkDoubleArray)polyData.GetPointData().GetArray("values");
        for (int i=0; i<polyData.GetNumberOfPoints(); i++)
            texCoords.InsertNextTuple2(0,pointValues.GetValue(i));
        erosModel.getSmallBodyPolyData().GetPointData().SetTCoords(texCoords);
        ObjUtil.writePolyDataToObj(erosModel.getSmallBodyPolyData(), reader.GetOutput(), Paths.get("/Users/zimmemi1/Desktop/erosWithElevationTexCoords.obj"), "");


        vtkPolyDataWriter writer=new vtkPolyDataWriter();
        writer.SetFileName("/Users/zimmemi1/Desktop/test.vtk");
        writer.SetFileTypeToBinary();
        writer.SetInputData(erosModel.getSmallBodyPolyData());
        writer.Write();

/*        Colormap colormap=Colormaps.getNewInstanceOfBuiltInColormap("Spectral_lowBlue");
        double[] coloringRange=erosModel.getCurrentColoringRange(coloringIndex);
        colormap.setRangeMin(coloringRange[0]);
        colormap.setRangeMax(coloringRange[1]);

        vtkPolyData polyData=erosModel.getSmallBodyPolyData();
        vtkUnsignedCharArray colors=new vtkUnsignedCharArray();
        colors.SetNumberOfComponents(3);
        for (int i=0; i<polyData.GetNumberOfPoints(); i++)
        {
            double[] pt=polyData.GetPoint(i);
            double value=erosModel.getColoringValue(coloringIndex, pt); // apply coloring index explicitly
            //double value=erosModel.getColoringInfoList().get(coloringIndex).coloringValues.GetValue(i);
            Color color=colormap.getColor(value);
            colors.InsertNextTuple3(color.getRed(), color.getGreen(), color.getBlue()); // these are int values in the range 0-255 being cast to double
        }
        colors.SetName(ObjUtil.pointDataColorArrayName);
        polyData.GetPointData().AddArray(colors);
        polyData.GetPointData().SetScalars(colors);

        ObjUtil.writePolyDataToObj(polyData, Paths.get("/Users/zimmemi1/Desktop/test.obj"));*/

/*        Path elevationFile=Paths.get("/Users/zimmemi1/Desktop/erosElevationScaled.txt");
        FileWriter writer=new FileWriter(elevationFile.toFile());
        ColoringInfo info=erosModel.getColoringInfoList().get(coloringIndex);
        writer.write("# values = "+info.coloringValues.GetNumberOfTuples()+"\n");
        for (int i=0; i<info.coloringValues.GetNumberOfTuples(); i++)
        {
            double scaledValue=(info.coloringValues.GetTuple1(i)-coloringRange[0])/(coloringRange[1]-coloringRange[0]);
            if (scaledValue<0)
                scaledValue=0;
            if (scaledValue>1)
                scaledValue=1;
            writer.write(scaledValue+"\n");
        }
        writer.close();

        Path colormapFile=Paths.get("/Users/zimmemi1/Desktop/colormap_Spectral_lowBlue.txt");
        colormap.setRangeMin(0);
        colormap.setRangeMax(1);
        FileWriter writer2=new FileWriter(colormapFile.toFile());
        writer2.write("colors = "+colormap.getNumberOfLevels()+"\n");
        for (int i=0; i<colormap.getNumberOfLevels(); i++)
        {
            Color c=colormap.getColor((double)i/(double)(colormap.getNumberOfLevels()-1));
            writer2.write(c.getRed()+" "+c.getGreen()+" "+c.getBlue()+"\n");
        }
        writer2.close();*/


    }

}
