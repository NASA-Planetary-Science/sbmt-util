package edu.jhuapl.sbmt.util.website;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.google.common.collect.Lists;

import edu.jhuapl.saavtk.config.ViewConfig;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.core.image.ImageSource;
import edu.jhuapl.sbmt.core.image.ImagingInstrument;
import edu.jhuapl.sbmt.query.database.GenericPhpQuery;
import edu.jhuapl.sbmt.tools.SqlManager;


public class WebsiteImageListMaker
{

    public static void main(String[] args) throws Exception
    {
        System.out.println("Tables:");
        SqlManager db = new SqlManager(null);
        List<List<Object>> tables = db.query("SHOW TABLES");
        for (int i = 0; i < tables.size(); i++)
            System.out.println(tables.get(i));

        List<ShapeModelBody> bodies=Lists.newArrayList();
        List<ShapeModelType> authors=Lists.newArrayList();
        List<String> versions=Lists.newArrayList();

        bodies.add(ShapeModelBody.EROS);
        authors.add(ShapeModelType.GASKELL);
        versions.add("");

        bodies.add(ShapeModelBody.ITOKAWA);
        authors.add(ShapeModelType.GASKELL);
        versions.add("");

       bodies.add( ShapeModelBody.VESTA);
       authors.add(ShapeModelType.GASKELL);
       versions.add("");

       bodies.add(ShapeModelBody.CERES);
       authors.add(ShapeModelType.GASKELL);
       versions.add("");

       bodies.add(ShapeModelBody.DEIMOS);
       authors.add(ShapeModelType.THOMAS);
       versions.add("");

       bodies.add(ShapeModelBody.PHOBOS);
       authors.add(ShapeModelType.GASKELL);
       versions.add("");

       bodies.add(ShapeModelBody.PHOBOS);
       authors.add(ShapeModelType.EXPERIMENTAL);
       versions.add("");

        bodies.add(ShapeModelBody._67P);
        authors.add(ShapeModelType.GASKELL);
        versions.add("SHAP5 V0.3");

        bodies.add(ShapeModelBody._67P);
        authors.add(ShapeModelType.DLR);
        versions.add("SHAP4S");

        bodies.add(ShapeModelBody._67P);
        authors.add(ShapeModelType.GASKELL);
        versions.add("V2");

        bodies.add(ShapeModelBody._67P);
        authors.add(ShapeModelType.GASKELL);
        versions.add("V3");

        bodies.add(ShapeModelBody.JUPITER);
        authors.add(null);
        versions.add("");

        bodies.add(ShapeModelBody.CALLISTO);
        authors.add(null);
        versions.add("");

        bodies.add(ShapeModelBody.EUROPA);
        authors.add(null);
        versions.add("");

        bodies.add(ShapeModelBody.GANYMEDE);
        authors.add(null);
        versions.add("");

        bodies.add(ShapeModelBody.IO);
        authors.add(null);
        versions.add("");

        bodies.add(ShapeModelBody.RQ36);
        authors.add(ShapeModelType.GASKELL);
        versions.add("V3 Image");

        bodies.add(ShapeModelBody.RQ36);
        authors.add(ShapeModelType.GASKELL);
        versions.add("V3 Image");

//        RQ36V4_MAP(SmallBodyConfig.getSmallBodyConfig(ShapeModelBody.RQ36, ShapeModelAuthor.GASKELL, "V4 Image"),
//               "/project/nearsdc/data/GASKELL/RQ36_V4/MAPCAM/imagelist-fullpath.txt", "RQ36V4_MAP"),
//        RQ36V4_POLY(SmallBodyConfig.getSmallBodyConfig(ShapeModelBody.RQ36, ShapeModelAuthor.GASKELL, "V4 Image"),
//                "/project/nearsdc/data/GASKELL/RQ36_V4/POLYCAM/imagelist-fullpath.txt", "RQ36V4_POLY"),

        bodies.add(ShapeModelBody.PLUTO);
        authors.add(null);
        versions.add("");


        // set date format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        SmallBodyViewConfig.initialize();
        List<ViewConfig> configs = SmallBodyViewConfig.getBuiltInConfigs();
        for (ViewConfig c : SmallBodyViewConfig.getBuiltInConfigs())
        {
            SmallBodyViewConfig config = (SmallBodyViewConfig) c;

            if (!bodies.contains(config.body))
            {
                System.out.println(config.body+" not in list");
                continue;
            }

            int idx=bodies.indexOf(config.body);
            if (authors.get(idx)!=null && authors.get(idx)!=config.author)
            {
                System.out.println(config.body+" with author "+config.author+" not in list");
                continue;
            }

            if (!versions.get(idx).equals("") && versions.get(idx)!=config.version)
            {
                System.out.println(config.body+" with author "+config.author+" and version "+config.version+" not in list");
                continue;
            }

            for (ImagingInstrument instrument : config.imagingInstruments) // for example MSI
            {

                for (ImageSource source : instrument.searchImageSources) // pointing (i.e. Gaskell Derived, SPICE)
                {

                    if (instrument.searchQuery instanceof GenericPhpQuery)
                    {

                        try
                        {

                            System.out.println(c.getUniqueName() + " " +
                                    instrument.instrumentName.name() + " " +
                                    source.name());

                            GenericPhpQuery genericQuery = (GenericPhpQuery) instrument.searchQuery;

                            String imagesDatabase = genericQuery
                                    .getTablePrefix(source) + "images_"
                                    + source.getDatabaseTableName();
                            List<List<Object>> columnNamesResult = db.query(
                                    "SHOW COLUMNS FROM " + imagesDatabase);
                            List<String> columnNames = Lists.newArrayList();
                            for (int i = 0; i < columnNamesResult.size(); i++)
                                columnNames.add((String) columnNamesResult
                                        .get(i).get(0));

                            String filename = imagesDatabase + ".txt";
                            System.out.println("Writing " + Paths.get(filename).toAbsolutePath().toString());
                            FileWriter writer = new FileWriter(
                                    new File(filename));
                            for (int i = 0; i < columnNames.size(); i++)
                                writer.write(columnNames.get(i) + ", ");
                            writer.write(System.lineSeparator());

                            List<List<Object>> result = db
                                    .query("SELECT * FROM " + imagesDatabase);
                            for (int i = 0; i < result.size(); i++)
                            {
                                for (int j = 0; j < result.get(i).size(); j++)
                                {
                                    if (j == 1) { // file name
                                        // TODO write full path instead of just file name??
                                        writer.write("/project/nearsdc/data" + genericQuery.getDataPath() + "/"+ String.valueOf(result.get(i).get(j)) + ", ");
                                    }
                                    else if (j == 2 || j == 3) { // start time / stop time
                                        // TODO format datetime correctly
                                        Date dt = new Date(Long.parseLong(String.valueOf(result.get(i).get(j))));
                                        writer.write(sdf.format(dt) + ", ");
                                    }
                                    else {
                                        writer.write(String.valueOf(result.get(i).get(j))+", ");
                                    }
                                }
                                writer.write(System.lineSeparator());
                            }
                            writer.close();

                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        /*
                         * List<List<String>> results = instrument.searchQuery
                         * .runQuery("", startDateJoda, endDateJoda, false,
                         * camerasSelected, filtersSelected, 1e-12, 1e12, 1e-12,
                         * 1e12, searchField, null, 1e-12, 1e12, 1e-12, 1e12,
                         * 1e-12, 1e12, null, source, 0, false);
                         * System.out.println(c.getUniqueName() + " " +
                         * instrument.instrumentName.name() + " " +
                         * source.name() + " " +
                         * camerasSelected+" "+filtersSelected+" "+results.size(
                         * ));
                         */
                    }
                }
            }
        }
    }



}
