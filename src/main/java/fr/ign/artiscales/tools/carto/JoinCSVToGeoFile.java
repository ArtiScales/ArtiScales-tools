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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JoinCSVToGeoFile {

    // public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException, FactoryException {
    // File geoFile = new File("/tmp/test.gpkg");
    // File csvFile = new File("/home/ubuntu/workspace/ParcelManager/src/main/resources/DensificationStudy/out/densificationStudyResult.csv");
    // File outFile = new File("/tmp/joined");
    // joinCSVToGeopackage(geoFile, "DEPCOM", csvFile, "DEPCOM", outFile, null);
    // }

    /**
     * Joining tabular informations of a .csv to a geoFile.
     * Fields that have the same name won't be double - only the one from the geoFile will be kept.
     *
     * @param geoFile      geo file containing the collection of features to join
     * @param joinGeoField field of the collection of features to join
     * @param csvFile      csv to join
     * @param joinCsvField common field of the csv to join
     * @param outFile      write the result on this file
     * @param attrsToStat  list of attributes to to stats on
     * @param statsToDo    Statistical operations to do. Can be null
     * @return the joined file
     * @throws IOException reading .csv file and geo file
     * @deprecated untested
     */

    public static File joinCSVToGeoFile(File geoFile, String joinGeoField, File csvFile, String joinCsvField, File outFile, List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(geoFile);
        SimpleFeatureCollection result = joinCSVToGeoFile(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), joinGeoField, csvFile, joinCsvField, attrsToStat, statsToDo);
        ds.dispose();
        return CollecMgmt.exportSFC(result, outFile);
    }

    /**
     * Joining tabular information of a .csv to a geoFile.
     * Fields that have the same name won't be double - only the one from the geoFile will be kept.
     *
     * @param sfc          collection of features to join
     * @param joinGeoField field of the collection of features to join
     * @param csvFile      csv to join
     * @param joinCsvField common field of the csv to join
     * @param attrsToStat  list of attributes to to stats on
     * @param statsToDo    Statistical operations to do. Can be null
     * @return the joined simplefeaturecollection
     * @throws IOException reading .csv file
     * @deprecated untested
     */
    public static SimpleFeatureCollection joinCSVToGeoFile(SimpleFeatureCollection sfc, String joinGeoField, File csvFile, String joinCsvField,
                                                           List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        // TODO finish to develop that
        CSVReader reader = Csv.getCSVReader(csvFile);
        String[] firstline = reader.readNext();
        int cpIndice = Attribute.getIndice(firstline, joinCsvField);
        reader.close();
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
        try (SimpleFeatureIterator it = sfc.features()) {
            while (it.hasNext()) {
                SimpleFeature com = it.next();
                for (AttributeDescriptor field : schema.getAttributeDescriptors())
                    build.set(field.getLocalName(), com.getAttribute(field.getLocalName()));
                String valu = String.valueOf(com.getAttribute(joinGeoField));
                int count = 0;
                // reading and running through the .csv
                CSVReader r = Csv.getCSVReader(csvFile);
                r.readNext();
                List<String[]> read = r.readAll();
                String[] lastLine = new String[read.get(0).length];
                for (String[] line : read)
                    if (line[cpIndice].equals(valu)) {
                        count++;
                        lastLine = line;
                    }
                if (count > 1)
                    System.out.println("joinCSVToGeoFile: More than one entry on the CSV. Putting the last line");
                int nbAttTmp = nbAtt;
                boolean skip = false;
                for (String l : lastLine) { //copy lines
                    nbAttTmp++;
                    if (!skip && iToSkip.contains(nbAttTmp - nbAtt)) {
                        skip = true; //if the value of the line is skipped, we roll back indices and don't check the next indice
                        nbAttTmp--;
                    } else {
                        build.set(nbAttTmp - 1, l);
                        skip = false;
                    }
                }
                if (statsToDo != null && statsToDo.contains(StatisticOperation.COUNT))
                    build.set("count", count);

                //TODO add other stats
                result.add(build.buildFeature(Attribute.makeUniqueId()));
                r.close();
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        reader.close();
        return result;
    }
}
