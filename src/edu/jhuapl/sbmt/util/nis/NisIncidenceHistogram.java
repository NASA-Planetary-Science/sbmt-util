package edu.jhuapl.sbmt.util.nis;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.LinearSpace;

public class NisIncidenceHistogram extends RegularHistogram2D
{

    public NisIncidenceHistogram(int nPhaseBins, int nPlaneBins)
    {
        super(LinearSpace.create(0, 180, nPhaseBins+1), LinearSpace.create(0, 90, nPlaneBins+1));
    }

    public void add(List<NisSample> samples)
    {
        for (int i=0; i<samples.size(); i++)
        {
            try
            {
                NisSample sample=samples.get(i);
                double phaseAngle=Math.toDegrees(Vector3D.angle(sample.toSpacecraft, sample.toSun));
                double planeAngle=Math.toDegrees(Vector3D.angle(new Vector3D(0.5,sample.toSpacecraft,0.5,sample.toSun), sample.normal));
                incrementCount(phaseAngle,planeAngle);
            } catch (HistogramValueOutOfBoundsException e)
            {
                e.printStackTrace();
            }
        }
    }


}
