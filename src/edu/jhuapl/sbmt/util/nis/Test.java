package edu.jhuapl.sbmt.util.nis;

public class Test
{
    public static void main(String[] args)
    {
        NisDataSource data=new NisDataSource();
        data.setResolutionLevel(Integer.valueOf(args[1]));
        //
        NisProcessor processor=new NisProcessor(data, args[0]);
        processor.run();
        processor.writeSamples(data.basePath.resolve(args[0]).resolve("faceSamples."+args[1]+".dat"));
        //
//        List<NisSample> sampleSet=NisSampleFile.read(data.basePath.resolve(args[0]).resolve("faceSamples."+args[1]+".dat"));
//        System.out.println(sampleSet.size());
    }
}
