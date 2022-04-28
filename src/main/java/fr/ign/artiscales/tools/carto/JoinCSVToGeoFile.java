package fr.ign.artiscales.tools.carto;

import com.opencsv.CSVReader;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.io.csv.Csv;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JoinCSVToGeoFile {

    /**
     * Joining tabular information of a .csv to a geoFile.
     * Fields that have the same name won't be double - only the one from the geoFile will be kept.
     *
     * @param geoFile      geo file containing the collection of features to join
     * @param joinGeoField field of the collection of features to join
     * @param csvFile      csv to join
     * @param joinCsvField common field of the csv to join
     * @param joinType     type of joining. Can be : <ul>
     *                     <li><b>inner</b> : geo file and .csv file must match to be included in the result.</li>
     *                     <li><b>left</b> : result will have every csv entries and only matching geo files entries.</li>
     *                     <li><b>right</b> : result will have every geo files entries and only matching csv entries.</li>
     *                     <li><b>full</b>: result have every geo files and csv entries. </li>
     *                     </ul>
     * @param outFile      write the result on this file
     * @param attrsToStat  list of attributes to do stats on
     * @param statsToDo    Statistical operations to do. Can be null
     * @return the joined file
     * @throws IOException reading .csv file and geo file
     */

    public static File joinCSVToGeoFile(File geoFile, String joinGeoField, File csvFile, String joinCsvField, String joinType, File outFile, List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(geoFile);
        SimpleFeatureCollection result = joinCSVToGeoFile(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), joinGeoField, csvFile, joinCsvField, joinType, attrsToStat, statsToDo);
        ds.dispose();
        return CollecMgmt.exportSFC(result, outFile);
    }

    /**
     * Joining tabular information of a .csv to a geoFile.
     * Fields that have the same name won't be double - only the one from the geoFile will be kept.
     * .csv is put into memory so it doesn't have to be too massive.
     * If features doesn't have a geometry (because we need to keep them from the .csv), their geometry is a Point with coordinate (0,0).
     *
     * @param sfc          collection of features to join
     * @param joinGeoField field of the collection of features to join
     * @param csvFile      csv to join
     * @param joinCsvField common field of the csv to join
     * @param joinType     type of joining. Can be : <ul>
     *                     <li><b>inner</b> : geo file and .csv file must match to be included in the result.</li>
     *                     <li><b>left</b> : result will have every csv entries and only matching geo files entries.</li>
     *                     <li><b>right</b> : result will have every geo files entries and only matching csv entries.</li>
     *                     <li><b>full</b>: result have every geo files and csv entries. </li>
     *                     </ul>
     * @param attrsToStat  list of attributes to do stats on
     * @param statsToDo    Statistical operations to do. Can be null
     * @return the joined simplefeaturecollection
     * @throws IOException reading .csv file
     */
    public static SimpleFeatureCollection joinCSVToGeoFile(SimpleFeatureCollection sfc, String joinGeoField, File csvFile, String joinCsvField, String joinType,
                                                           List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        // Get crucial Csv informations
        CSVReader reader = Csv.getCSVReader(csvFile);
        String[] firstline = reader.readNext();
        int cpIndice = Attribute.getIndice(firstline, joinCsvField);
        reader.close();

        //Prepare result geo file
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType schema = sfc.getSchema();
        int nbAtt = schema.getAttributeCount();
        // create the builder
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        sfTypeBuilder.setCRS(schema.getCoordinateReferenceSystem());
        sfTypeBuilder.setName(schema.getName() + "_" + csvFile.getName());
        if (attrsToStat != null && !attrsToStat.isEmpty())
            for (String attrToStat : attrsToStat)
                for (StatisticOperation statToDo : statsToDo)
                    sfTypeBuilder.add(attrToStat + "-" + statToDo, Double.class);
        for (AttributeDescriptor field : schema.getAttributeDescriptors())
            sfTypeBuilder.add(field);
        sfTypeBuilder.setDefaultGeometry(schema.getGeometryDescriptor().getLocalName());
        List<Integer> iToSkip = new ArrayList<>(); //list of indices that are doubled and be ignored in the copiyng of line
        int i = 0;
        for (String field : firstline) {
            i++;
            if (sfTypeBuilder.get(field) != null) {
                System.out.println("schema already contains the " + field + " field. Switched");
                iToSkip.add(i);
                continue;
            }
            sfTypeBuilder.add(field, String.class);
        }
        SimpleFeatureBuilder build = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

        // If we put csv data into memory
        CSVReader r = Csv.getCSVReader(csvFile);
        r.readNext();
        List<String[]> lines = r.readAll();
        r.close();
        HashMap<String[], Boolean> valuePutten = new HashMap<>(lines.size());
        if (joinType.equals("full") || joinType.equals("left"))
            for (String[] l : lines)
                valuePutten.put(l, false);
        //We now iterate over collection of geometries
        try (SimpleFeatureIterator it = sfc.features()) {
            while (it.hasNext()) {
                SimpleFeature sf = it.next();

                // find common value
                String value = String.valueOf(sf.getAttribute(joinGeoField));
                int count = 0;
                String[] correspondingLine = new String[lines.get(0).length];
                boolean foundCorrespondance = false;
                for (String[] line : lines)
                    if (line[cpIndice].equals(value)) {
                        count++;
                        correspondingLine = line;
                        foundCorrespondance = true;
                    }
                if (count > 1)
                    System.out.println("joinCSVToGeoFile: More than one entry on the CSV. Putting the last line");

                // get what to do regarding different versions :
                if ((joinType.equals("inner") || joinType.equals("left")) && !foundCorrespondance)  // only matching entries (or matching and .csv entries)
                    continue;

                for (AttributeDescriptor field : schema.getAttributeDescriptors())
                    build.set(field.getLocalName(), sf.getAttribute(field.getLocalName()));
                fillSFBWithCSV(nbAtt, correspondingLine, iToSkip, build);
                if (foundCorrespondance)
                    valuePutten.put(correspondingLine, true);

                //Do stat stuff
                if (statsToDo != null && statsToDo.contains(StatisticOperation.COUNT))
                    build.set("count", count);
                if (statsToDo != null && !statsToDo.isEmpty() && !statsToDo.contains(StatisticOperation.COUNT))
                    System.out.println("joinCSVToGeoFile - stats not implemented yet :" + statsToDo);

                result.add(build.buildFeature(Attribute.makeUniqueId()));

            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        // if full type, we put csv values that haven't found correspondence
        if (joinType.equals("full") || joinType.equals("left"))
            for (String[] correspondingLine : valuePutten.keySet())
                if (!valuePutten.get(correspondingLine)) {
                    fillSFBWithCSV(nbAtt, correspondingLine, iToSkip, build);
                    build.set(CollecMgmt.getDefaultGeomName(), (new GeometryFactory()).createPoint(new Coordinate(0, 0)));
                    result.add(build.buildFeature(Attribute.makeUniqueId()));
                }
        reader.close();
        return result;
    }

    private static void fillSFBWithCSV(int nbAtt, String[] correspondingLine, List<Integer> iToSkip, SimpleFeatureBuilder builder) {
        int nbAttTmp = nbAtt;
        boolean skip = false;
        for (String l : correspondingLine) { //copy lines
            nbAttTmp++;
            if (!skip && iToSkip.contains(nbAttTmp - nbAtt)) {
                skip = true; //if the value of the line is skipped, we roll back indices and don't check the next indice
                nbAttTmp--;
            } else {
                builder.set(nbAttTmp - 1, l);
                skip = false;
            }
        }
    }


}
