package edu.jhuapl.sbmt.util.hololens;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Paths;

import vtk.vtkNativeLibrary;
import vtk.vtkPolyData;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.colormap.Colormap;
import edu.jhuapl.saavtk.colormap.Colormaps;
import edu.jhuapl.saavtk.model.ShapeModelAuthor;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.util.ObjUtil;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;

public class HololensEros
{

    static
    {
        vtkNativeLibrary.LoadAllNativeLibraries();
        SmallBodyViewConfig.initialize();
    }

    public static void main(String[] args) throws IOException
    {
        int coloringIndex=2; // at the time of implementation index 1 is point-centered elevation data

        SmallBodyViewConfig config=SmallBodyViewConfig.getSmallBodyConfig(ShapeModelBody.EROS, ShapeModelAuthor.GASKELL);
        SmallBodyModel erosModel = SbmtModelFactory.createSmallBodyModel(config);
        erosModel.setColoringIndex(coloringIndex);

        Colormap colormap=Colormaps.getNewInstanceOfBuiltInColormap("Spectral_lowBlue");
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

        ObjUtil.writePolyDataToObj(polyData, Paths.get("/Users/zimmemi1/Desktop/test.obj"));

    }

}
