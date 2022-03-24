package fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.GeoJSON;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geopackages;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Shp;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

public class CollecMgmt {
    private static String defaultGISFileType = ".gpkg";
    private static String forcedGeomName;
/*    public static void main(String[] args) throws IOException {
        DataStore ds = getDataStore(new File("/home/mc/workspace/ici_pedestrian/input/voirie-pieton/plan-de-voirie-emprises-espaces-verts2.gpkg"));
//        DataStore ds = getDataStore(new File("/home/mc/workspace/ici_pedestrian/input/voirie-pieton/plan-de-voirie-emprises-espaces-verts.geojson"));
        SimpleFeatureCollection dd = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();

        try ( SimpleFeatureIterator it = dd.features()){
            while (it.hasNext())
                System.out.println(it.next());
        }
        System.out.println(dd.size());
    }*/

    public static SimpleFeatureCollection transformGeomToMultiPolygon(SimpleFeatureCollection parcel) {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        try (SimpleFeatureIterator it = parcel.features()) {
            while (it.hasNext())
                result.add(Schemas.setSFBSchemaWithMultiPolygon(it.next()).buildFeature(Attribute.makeUniqueId()));
        } catch (Error r) {
            r.printStackTrace();
        }
        return result;
    }

    /**
     * Get the default format of geographic files used (can either be <i>.shp</i>, <i>.gpkg</i> or <i>.geojson</i>).
     *
     * @return the format used (default, .gpkg)
     */
    public static String getDefaultGISFileType() {
        return defaultGISFileType;
    }

    /**
     * Set the default format of geographic files used.
     *
     * @param newDefaultGISFileType Can either be <i>.shp</i>, <i>.gpkg</i> or <i>.geojson</i>.
     */
    public static void setDefaultGISFileType(String newDefaultGISFileType) {
        defaultGISFileType = newDefaultGISFileType;
    }

    /**
     * Merge geofiles. They must be of the same type, Geopackage (.gpkg) or Shapefile (.shp). Try to keep attributes.
     *
     * @param file2MergeIn List of geofiles to merge
     * @param fileOut      Where to write the merged File
     * @return the merged File
     * @throws IOException Reading and writing
     */
    public static File mergeFiles(List<File> file2MergeIn, File fileOut) throws IOException {
        return mergeFiles(file2MergeIn, fileOut, null, true);
    }

    /**
     * Merge geofiles. They must be of the same type, Geopackage (.gpkg) or Shapefile (.shp). Load every SFC in memory.
     * If we want to keep attribute, different situations happens after a schema check :
     * <ul>
     *    <li>if they have the same schema, they are merged and schemas and attribute are kept.</li>
     *    <li>if they doesn't have the same schema but the same number of attribute, we make the assumption that attributes are the same and they are merged.</li>
     *     <li>if they doesn't have the same schema, we'll complete only the attributes that have the exact same name on every SFC.</li>
     * </ul>
     *
     * @param file2MergeIn   List of geofiles to merge
     * @param fileOut        Where to write the merged File
     * @param boundFile      apply a mask on the result
     * @param keepAttributes keep every attribute. Must be the same schema between every files.
     * @return the merged File
     * @throws IOException Reading, writing, or unknown extension
     */
    public static File mergeFiles(List<File> file2MergeIn, File fileOut, File boundFile, boolean keepAttributes) throws IOException {
        switch (file2MergeIn.get(0).getName().split("\\.")[file2MergeIn.get(0).getName().split("\\.").length - 1].toLowerCase()) {
            case "gpkg":
                return Geopackages.mergeGpkgFiles(file2MergeIn, fileOut, boundFile, keepAttributes);
            case "shp":
                return Shp.mergeVectFiles(file2MergeIn, fileOut, boundFile, keepAttributes);
            case "geojson":
            case "json":
                throw new IOException("Merge JSON file : not implemented yet");
        }
        throw new IOException("getDataStore: extension unknown");
    }

    /**
     * Merge collections of simple feature collection.
     * If we want to keep attribute, different situations happens after a schema check :
     * <ul>
     *     <li>if they have the same schema, they are merged and schemas and attribute are kept.</li>
     *     <li>if they doesn't have the same schema but the same number of attribute, we make the assumption that attributes are the same and they are merged.</li>
     *     <li>if they doesn't have the same schema, we'll complete only the attributes that have the exact same name on every SFC.</li>
     * </ul>
     *
     * @param sfcs           List of simplefeature to merge
     * @param boundFile      apply a mask on the result. Can be null.
     * @param keepAttributes keep every attribute. Must be the same schema between every file.
     * @return the merged File
     * @throws IOException Reading, writing, or unknown extension
     */
    public static SimpleFeatureCollection mergeSFC(List<SimpleFeatureCollection> sfcs, boolean keepAttributes, File boundFile) throws IOException {
        // sfBuilder used only if number of attributes's the same but with different schemas
        SimpleFeatureType schemaRef = sfcs.get(0).getSchema();
        DefaultFeatureCollection newParcelCollection = new DefaultFeatureCollection();
        List<String> matchingFields = new ArrayList<>();
        lookOutAttribute:
        if (keepAttributes) { // check if the schemas of the shape are the same and if not, if they have the same number of attributes
            int nbAttr = schemaRef.getAttributeCount();
            for (SimpleFeatureCollection sfc : sfcs) {
                SimpleFeatureType schemaComp = sfc.getSchema();
                if (schemaComp.equals(schemaRef) || nbAttr == schemaComp.getAttributeCount())
                    continue;
                System.out.println("Not the same amount of attributes in the file.");
                matchingFields = Schemas.getMatchingFields(sfcs);
                System.out.println("matching fields are " + matchingFields);
                break lookOutAttribute;
            }
        }
        List<String> finalMatchingFields = matchingFields;
        for (SimpleFeatureCollection sfc : sfcs) {
            if (keepAttributes) {
                SimpleFeatureBuilder defaultSFBuilder = new SimpleFeatureBuilder(schemaRef);
                try (SimpleFeatureIterator it = sfc.features()) {
                    while (it.hasNext()) {
                        SimpleFeature feat = it.next();
                        if (matchingFields.isEmpty())  // Same schema (or we assume that) : Merge the feature and assignate a new id number. If collections doesn't have the exactly same schema but the same number of attributes, we add every attribute regarding their position
                            Schemas.setFieldsToSFB(defaultSFBuilder, feat);
                        else  // we only keep matching fields
                            for (String field : finalMatchingFields)
                                defaultSFBuilder.set(field, feat.getAttribute(field));
                        newParcelCollection.add(defaultSFBuilder.buildFeature(Attribute.makeUniqueId()));
                    }
                }
            } else { // if we don't want to keep attributes, we create features out of new features containing only geometry
                SimpleFeatureBuilder basicSFBuilder = Schemas.getBasicSchema(schemaRef.getName().toString(), schemaRef.getGeometryDescriptor().getType().toString());
                try (SimpleFeatureIterator it = sfc.features()) {
                    while (it.hasNext()) {
                        basicSFBuilder.set(CollecMgmt.getDefaultGeomName(), it.next().getDefaultGeometry());
                        newParcelCollection.add(basicSFBuilder.buildFeature(Attribute.makeUniqueId()));
                    }
                }
            }
        }
        if (boundFile != null && boundFile.exists())
            return CollecTransform.selectIntersection(newParcelCollection, boundFile);
        else
            return newParcelCollection;
    }

    /**
     * Get the default geometric name associated with the type of geographic files used in the {@link #defaultGISFileType} argument.
     *
     * @return the format used (default, .gpkg)
     */
    public static String getDefaultGeomName() {
        if (forcedGeomName != null)
            return forcedGeomName;
        else if (defaultGISFileType.equals(".shp") || defaultGISFileType.equals(".geojson"))
            return "the_geom";
        else if (defaultGISFileType.equals(".gpkg"))
            return "geom";
        else
            return "";
    }

    public static void setDefaultGeomName(String geomName) {
        forcedGeomName = geomName;
    }

    public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String[] attributes) {
        return getEachUniqueFieldFromSFC(sfcIn, attributes, false);
    }

    /**
     * Get the unique values of a SimpleFeatureCollection from a combination of fields. Each fields are separated with a "-" character.
     *
     * @param sfcIn              input {@link SimpleFeatureCollection}
     * @param attributes         field name to create the combination of unique values
     * @param dontCheckAttribute don't check if the collection contains every input attribute (the error will be caught)
     * @return the list of unique values
     */
    public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String[] attributes, boolean dontCheckAttribute) {
        if (!dontCheckAttribute)
            for (String attribute : attributes)
                if (!isCollecContainsAttribute(sfcIn, attribute)) {
                    System.out.println("getEachUniqueFieldFromSFC:  no " + attribute + " found");
                    return null;
                }
        List<String> result = new ArrayList<>();
        Arrays.stream(sfcIn.toArray(new SimpleFeature[0])).forEach(sf -> {
            StringBuilder val = new StringBuilder();
            for (String attribute : attributes)
                try {
                    val.append("-").append((String.valueOf(sf.getAttribute(attribute))).replace(",", "-"));
                } catch (Exception ignored) {
                }
            if (val.toString().startsWith("-"))
                val = new StringBuilder(val.substring(1, val.length()));
            if (!result.contains(val.toString()))
                result.add(val.toString());
        });
        return result;
    }

    /**
     * Get the unique values of a SimpleFeatureCollection from a single field.
     *
     * @param sfcIn     input {@link SimpleFeatureCollection}
     * @param attribute field name to create the unique list
     * @return the list of unique values
     */
    public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String attribute) {
        String[] attributes = {attribute};
        return getEachUniqueFieldFromSFC(sfcIn, attributes);
    }

    /**
     * Get the unique values of a SimpleFeatureCollection from a single field.
     *
     * @param sfcIn     input {@link SimpleFeatureCollection}
     * @param attribute field name to create the unique list
     * @return the list of unique values
     */
    public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String attribute, boolean dontCheckAttribute) {
        return getEachUniqueFieldFromSFC(sfcIn, new String[]{attribute}, dontCheckAttribute);
    }

    /**
     * Check if the given Simple Feature contains the given field name. Uses the Schemas.isSchemaContainsAttribute method.
     *
     * @param feat               input feature
     * @param attributeFiledName name of the field (must respect case)
     * @return true if the feature contains the field name, false otherwise
     */
    public static boolean isSimpleFeatureContainsAttribute(SimpleFeature feat, String attributeFiledName) {
        return Schemas.isSchemaContainsAttribute(feat.getFeatureType(), attributeFiledName);
    }

    /**
     * Check if the given collection contains the given field name.
     *
     * @param collec             Input SimpleFeatureCollecton
     * @param attributeFiledName Name of the field (must respect case)
     * @return true if the collec contains the field name, false otherwise
     */
    public static boolean isCollecContainsAttribute(SimpleFeatureCollection collec, String attributeFiledName) {
        return Schemas.isSchemaContainsAttribute(collec.getSchema(), attributeFiledName);
    }

    public static DataStore getDataStore(URL url) throws IOException {
        return Geopackages.getDataStore(url);
    }

    /**
     * get the data Store matching the file's attribute. Must either be a geopackage (.gpkg), a shapefile (.shp), or a geojson (.json or .geojson).
     *
     * @param f input geographic file
     * @return The corresponding DataStore
     * @throws IOException If the file's not found or contains a wrong extension
     */
    public static DataStore getDataStore(File f) throws IOException {
        switch (f.getName().split("\\.")[f.getName().split("\\.").length - 1].toLowerCase()) {
            case "gpkg":
                return Geopackages.getDataStore(f);
            case "shp":
                return new ShapefileDataStore(f.toURI().toURL());
            case "geojson":
            case "json":
                return GeoJSON.getGeoJSONDataStore(f);
        }
        throw new IOException("getDataStore: extension unknown");
    }

    /**
     * Convert an attribute of a {@link SimpleFeatureCollection} to float type (needed for rasterization)
     *
     * @param sfcIn                  input collection
     * @param attributeToConvertName attribute to convert
     * @return the collection with converted field
     */
    public static SimpleFeatureCollection convertAttributeToFloat(SimpleFeatureCollection sfcIn, String attributeToConvertName) {
        return convertAttributeToFloat(sfcIn, Collections.singletonList(attributeToConvertName));
    }

    /**
     * Convert a list of attributes to float type(needed for rasterization)
     *
     * @param sfcIn                   input collection
     * @param attributesToConvertName list of attribute names to convert
     * @return the collection with converted field
     */
    public static SimpleFeatureCollection convertAttributeToFloat(SimpleFeatureCollection sfcIn, List<String> attributesToConvertName) {
        DefaultFeatureCollection df = new DefaultFeatureCollection();
        //set new builder informations
        SimpleFeatureType schema = sfcIn.getSchema();
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor attr : schema.getAttributeDescriptors())
            if (!attributesToConvertName.contains(attr.getLocalName()))
                sfTypeBuilder.add(attr);
        for (String attributeToConvertName : attributesToConvertName)
            sfTypeBuilder.add(attributeToConvertName, Float.class);
        sfTypeBuilder.setName(schema.getName());
        sfTypeBuilder.setCRS(schema.getCoordinateReferenceSystem());
        String geomName = schema.getGeometryDescriptor().getLocalName();
        sfTypeBuilder.setDefaultGeometry(geomName);
        SimpleFeatureBuilder newSchema = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
        // iterate and fill
        try (SimpleFeatureIterator it = sfcIn.features()) {
            while (it.hasNext()) {
                SimpleFeature feat = it.next();
                newSchema.set(geomName, feat.getDefaultGeometry());
                for (AttributeDescriptor attribute : feat.getFeatureType().getAttributeDescriptors()) {
                    if (!attributesToConvertName.contains(attribute.getLocalName()))
                        newSchema.set(attribute.getLocalName(), feat.getAttribute(attribute.getName()));
                    else {
                        Object at = feat.getAttribute(attribute.getName());
                        Float val;
                        if (at instanceof Double)
                            val = ((Double) at).floatValue();
                        else if (at instanceof Float)
                            val = (Float) at;
                        else if (at instanceof Integer)
                            val = ((Integer) feat.getAttribute(attribute.getName())).floatValue();
                        else if (at == null)
                            val = null;
                        else
                            val = Float.valueOf((String) at);
                        newSchema.set(attribute.getLocalName(), val);
                    }
                }
                df.add(newSchema.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Error e) {
            e.printStackTrace();
            System.out.println("Collec.convertAttributeToFloat: Impossible to cast " + attributesToConvertName + " to float. Input SFC returned");
            return sfcIn;
        }
        return df;
    }

    /**
     * Convert a list of {@link SimpleFeature} to a SimpleFeatureCollection.
     *
     * @param list list of SimpleFeature
     * @return A DefaultFeatureCollection wrote in memory (with the .collection() method)
     */
    public static SimpleFeatureCollection listToCollection(List<SimpleFeature> list) throws IOException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        result.addAll(list);
        return result.collection();
    }

    public static File exportSFC(List<SimpleFeature> listFeature, File fileOut) throws IOException {
        return exportSFC(listFeature, fileOut, true);
    }

    public static File exportSFC(List<SimpleFeature> listFeature, File fileOut, boolean overwrite) throws IOException {
        return exportSFC(listToCollection(listFeature), fileOut, overwrite);
    }

    public static File exportSFC(SimpleFeatureCollection toExport, File fileOut) throws IOException {
        return exportSFC(toExport, fileOut, true);
    }

    /**
     * Export a simple feature collection. Overwrite file if already exists
     *
     * @param toExport collection to export
     * @param fileOut  file to export
     * @return the wrote file
     * @throws IOException when writing the geo file
     */
    public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, boolean overwrite) throws IOException {
        if (toExport.isEmpty()) {
            System.out.println(fileOut.getName() + " is empty");
            return fileOut;
        }
        String n = fileOut.getName();
        String[] ext = n.split("\\.");
        return exportSFC(toExport, fileOut, ext.length > 1 ? "." + ext[1] : defaultGISFileType, overwrite);
    }

    public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, String outputType, boolean overwrite) throws IOException {
        return exportSFC(toExport, fileOut, toExport.getSchema(), outputType, overwrite);
    }

    /**
     * Export a simple feature collection. If the geo file already exists, either overwrite it or merge it with the existing feature collection.
     *
     * @param toExport   Collection to export
     * @param fileOut    file name and path where to write
     * @param ft         schema of the collection
     * @param outputType type of geo file to write
     * @param overwrite  do we overwrite or merge if an existing geo file is present ?
     * @return the geo file
     * @throws IOException when writing the geo file
     */
    public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft, String outputType, boolean overwrite) throws IOException {
        if (outputType.equals(".shp"))
            return Shp.exportSFCtoSHP(toExport, fileOut, ft, overwrite);
        else if (outputType.equals(".gpkg"))
            return Geopackages.exportSFCtoGPKG(toExport, fileOut, ft, overwrite);
        else if (defaultGISFileType != null && !defaultGISFileType.equals(""))
            return exportSFC(toExport, fileOut, ft, defaultGISFileType, overwrite);
        else
            throw new InvalidPropertiesFormatException("Cannot export " + toExport.getSchema().getName() + " to " + fileOut + ": unknown type " + outputType);
    }

    private static void coord2D(Coordinate c) {
        if (!(c instanceof CoordinateXY))
            c.setZ(Double.NaN);
    }

    public static File makeTransaction(DataStore newDataStore, SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft) throws IOException {
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore featureStore) {
            featureStore.setTransaction(transaction);
            try {
                SimpleFeatureCollection features = toExport.subCollection(new Filter() {
                    @Override
                    public boolean evaluate(Object object) {
                        SimpleFeature feature = (SimpleFeature) object;
                        return !((Geometry) feature.getDefaultGeometry()).isEmpty();
                    }

                    @Override
                    public Object accept(FilterVisitor visitor, Object extraData) {
                        return visitor.visit(Filter.INCLUDE, extraData);
                    }
                });
                DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("internal", ft);
                // FIXME Horrible Horrible Horrible hack to get the writer to work!!!
                GeometryFactory f = new GeometryFactory();
                try (FeatureIterator<SimpleFeature> iterator = features.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        SimpleFeature newFeature = SimpleFeatureBuilder.build(ft, feature.getAttributes(), null);
                        Geometry g = f.createGeometry((Geometry) feature.getDefaultGeometry());
                        g.apply(CollecMgmt::coord2D);
                        g.geometryChanged();
                        newFeature.setDefaultGeometry(g);
                        featureCollection.add(newFeature);
                    }
                }
                featureStore.addFeatures(featureCollection);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
                // toExport.accepts((Feature f) -> System.out.println(((SimpleFeature)f).getDefaultGeometry()), null);
            } finally {
                transaction.close();
            }
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
        newDataStore.dispose();
        return fileOut;
    }
}
