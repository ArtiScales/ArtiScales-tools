package fr.ign.artiscales.tools.carto;

import com.opencsv.CSVReader;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geopackages;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class JoinCSVToGeoFile {

    // public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException, FactoryException {
    // File geoFile = new File("/tmp/test.gpkg");
    // File csvFile = new File("/home/ubuntu/workspace/ParcelManager/src/main/resources/DensificationStudy/out/densificationStudyResult.csv");
    // File outFile = new File("/tmp/joined");
    // joinCSVToGeopackage(geoFile, "DEPCOM", csvFile, "DEPCOM", outFile, null);
    // }

    /**
     * @param geoFile
     * @param joinGeoField
     * @param csvFile
     * @param joinCsvField
     * @param outFile
     * @param attrsToStat
     * @param statsToDo
     * @return
     * @throws IOException
     * @deprecated not tested and unfinished method
     */

    public static File joinCSVToShapeFile(File geoFile, String joinGeoField, File csvFile, String joinCsvField, File outFile, List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        ShapefileDataStore sds = new ShapefileDataStore(geoFile.toURI().toURL());
        File result = joinCSVToGeoFile(sds.getFeatureSource(sds.getTypeNames()[0]).getFeatures(), joinGeoField, csvFile, joinCsvField, outFile, attrsToStat, statsToDo);
        sds.dispose();
        return result;
    }

    /**
     * @param geoFile
     * @param joinGeoField
     * @param csvFile
     * @param joinCsvField
     * @param outFile
     * @param attrsToStat
     * @param statsToDo
     * @return
     * @throws IOException
     * @deprecated not tested and unfinished method
     */

    public static File joinCSVToGeopackage(File geoFile, String joinGeoField, File csvFile, String joinCsvField, File outFile,
                                           List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        DataStore ds = Geopackages.getDataStore(geoFile);
        File result = joinCSVToGeoFile(Objects.requireNonNull(ds).getFeatureSource(ds.getTypeNames()[0]).getFeatures(), joinGeoField, csvFile, joinCsvField, outFile,
                attrsToStat, statsToDo);
        ds.dispose();
        return result;
    }

    /**
     * @param sfc
     * @param joinGeoField
     * @param csvFile
     * @param joinCsvField
     * @param outFile
     * @param attrsToStat
     * @param statsToDo
     * @return
     * @throws IOException
     * @deprecated not tested and unfinished method
     */
    public static File joinCSVToGeoFile(SimpleFeatureCollection sfc, String joinGeoField, File csvFile, String joinCsvField, File outFile,
                                        List<String> attrsToStat, List<StatisticOperation> statsToDo) throws IOException {
        // TODO finish to develop that
        CSVReader reader = new CSVReader(new FileReader(csvFile));
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
        for (String field : firstline) {
            if (sfTypeBuilder.get(field) != null) {
                System.out.println("schema already contains the " + field + " field. Switched");
                continue;
            }
            sfTypeBuilder.add(field, String.class);
        }
        SimpleFeatureBuilder build = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
        try (SimpleFeatureIterator it = sfc.features()) {
            while (it.hasNext()) {
                CSVReader r = new CSVReader(new FileReader(csvFile));
                r.readNext();
                List<String[]> read = r.readAll();
                SimpleFeature com = it.next();
                for (AttributeDescriptor field : schema.getAttributeDescriptors())
                    build.set(field.getLocalName(), com.getAttribute(field.getLocalName()));

                String valu = String.valueOf(com.getAttribute(joinGeoField));
                int count = 0;
                String[] lastLine = new String[read.get(0).length];
                for (String[] line : read)
                    if (line[cpIndice].equals(valu)) {
                        count++;
                        lastLine = line;
                    }
                if (count > 1)
                    System.out.println("joinCSVToGeoFile: More than one entry on the CSV. Putting the last line");
                int nbAttTmp = nbAtt - 1;
                for (String l : lastLine)
                    build.set(nbAttTmp++, l);
                if (statsToDo.contains(StatisticOperation.COUNT))
                    build.set("count", count);
                //TODO add other stats
                result.add(build.buildFeature(null));
                r.close();
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        reader.close();
        return CollecMgmt.exportSFC(result, outFile);
    }
}
