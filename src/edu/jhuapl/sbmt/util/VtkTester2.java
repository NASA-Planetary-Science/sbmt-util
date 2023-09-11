package edu.jhuapl.sbmt.util;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.awt.GLCanvas;

import vtk.vtkActor;
import vtk.vtkActor2D;
import vtk.vtkCell;
import vtk.vtkCellArray;
import vtk.vtkCellPicker;
import vtk.vtkDataSetMapper;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper2D;
import vtk.vtkPolyDataReader;
import vtk.rendering.vtkAbstractComponent;
import vtk.rendering.vtkAbstractEventInterceptor;

import edu.jhuapl.saavtk.gui.jogl.vtksbmtJoglCanvas;
import edu.jhuapl.saavtk.gui.jogl.vtksbmtJoglComponent;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;

public class VtkTester2
{
	public static void main(String[] args)
	{
		final boolean usePanel = false;

		NativeLibraryLoader.loadVtkLibraries();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// build VTK Pipeline
				// VTK rendering part
				final vtkAbstractComponent<?> joglWidget = usePanel ? vtksbmtJoglComponent.createGL()
						: new vtksbmtJoglCanvas();
				System.out.println(
						"We are using " + joglWidget.getComponent().getClass().getName() + " for the rendering.");

				vtkPolyDataReader reader = new vtkPolyDataReader();
				reader.SetFileName("/Users/steelrj1/Desktop/exterior_three_circle.vtk");
				reader.Update();

				vtkDataSetMapper mapper = new vtkDataSetMapper();
				mapper.SetInputConnection(reader.GetOutputPort());

				vtkActor actor = new vtkActor();
				actor.SetMapper(mapper);
				actor.GetProperty().SetLineWidth(2.0f);
//				actor.GetProperty().SetRenderLinesAsTubes(true);

				vtkPolyDataReader reader2 = new vtkPolyDataReader();
				reader2.SetFileName("/Users/steelrj1/Desktop/exterior_three_ellipse.vtk");
				reader2.Update();

				vtkDataSetMapper mapper2 = new vtkDataSetMapper();
				mapper2.SetInputConnection(reader2.GetOutputPort());

				vtkActor actor2 = new vtkActor();
				actor2.SetMapper(mapper2);
				actor2.GetProperty().SetLineWidth(2.0f);
//				actor2.GetProperty().SetRenderLinesAsTubes(true);

				vtkPolyData vBarPD = new vtkPolyData();
				vtkPoints vTmpP = new vtkPoints();
				vtkCellArray vTmpCA = new vtkCellArray();
				vBarPD.SetPoints(vTmpP);
				vBarPD.SetPolys(vTmpCA);

				vTmpP.SetNumberOfPoints(4);

				vTmpP.SetPoint(0, 10.0, 0.0, 0.0);
				vTmpP.SetPoint(1, 100, 0, 0.0);
				vTmpP.SetPoint(2, 100.0, 50.0, 0.0);
				vTmpP.SetPoint(3, 10, 50.0, 0.0);

				vtkIdList vTmpIL = new vtkIdList();
				vTmpIL.SetNumberOfIds(4);
				for (int i = 0; i < 4; ++i)
					vTmpIL.SetId(i, i);
				vTmpCA.InsertNextCell(vTmpIL);

				vtkPolyDataMapper2D vTmpPDM = new vtkPolyDataMapper2D();
				vTmpPDM.SetInputData(vBarPD);

				vtkActor2D vBarA = new vtkActor2D();
				vBarA.GetProperty().SetColor(1.0, 1.0, 1.0);
				vBarA.GetProperty().SetOpacity(0.5);
				vBarA.SetMapper(vTmpPDM);

				joglWidget.getRenderer().AddActor(actor);
				joglWidget.getRenderer().AddActor(actor2);
				joglWidget.getRenderer().AddActor(vBarA);

				// Add cell picker
				final vtkCellPicker picker = new vtkCellPicker();
				Runnable pickerCallback = new Runnable()
				{
					public void run()
					{
						if (picker.GetCellId() != -1)
						{
							vtkCell cell = picker.GetDataSet().GetCell(picker.GetCellId());
							System.out.println("Pick cell: " + picker.GetCellId() + " - Bounds: "
									+ Arrays.toString(cell.GetBounds()));
						}
					}
				};
				joglWidget.getRenderWindowInteractor().SetPicker(picker);
				picker.AddObserver("EndPickEvent", pickerCallback, "run");

				// Bind pick action to double-click
				joglWidget.getInteractorForwarder().setEventInterceptor(new vtkAbstractEventInterceptor()
				{

					public boolean mouseClicked(MouseEvent e)
					{
						// Request picking action on double-click
//						final double[] position =
//						{ e.getX(), joglWidget.getComponent().getHeight() - e.getY(), 0 };
//						if (e.getClickCount() == 2)
//						{
//							System.out.println("Click trigger the picking (" + position[0] + ", " + position[1] + ")");
//							picker.Pick(position, joglWidget.getRenderer());
//						}

						// We let the InteractionStyle process the event anyway
						return false;
					}
				});

				// UI part
				JFrame frame = new JFrame("SimpleVTK");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.getContentPane().setLayout(new BorderLayout());
				frame.getContentPane().add((GLCanvas)joglWidget.getComponent(), BorderLayout.CENTER);
				frame.setSize(400, 400);
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				joglWidget.resetCamera();
//				joglWidget.getComponent().requestFocus();

				// Add r:ResetCamera and q:Quit key binding
//				joglWidget.getComponent().addKeyListener(new KeyListener()
//				{
//					@Override
//					public void keyTyped(KeyEvent e)
//					{
//						if (e.getKeyChar() == 'r')
//						{
//							joglWidget.resetCamera();
//						}
//						else if (e.getKeyChar() == 'q')
//						{
//							System.exit(0);
//						}
//					}
//
//					@Override
//					public void keyReleased(KeyEvent e)
//					{
//					}
//
//					@Override
//					public void keyPressed(KeyEvent e)
//					{
//					}
//				});
			}
		});
	}
}
