package fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom.Lines;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import si.uom.SI;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CollecTransform {

    /**
     * Return a SimpleFeature with the merged geometries and the schema of the input collection but no attribute
     *
     * @param collec input {@link SimpleFeatureCollection}
     * @return a {@link SimpleFeature} with no values
     */
    public static SimpleFeature unionSFC(SimpleFeatureCollection collec) {
        SimpleFeatureBuilder builder = Schemas.getSFBSchemaWithMultiPolygon(collec.getSchema());
        builder.set(collec.getSchema().getGeometryDescriptor().getLocalName(), Geom.unionSFC(collec));
        return builder.buildFeature(Attribute.makeUniqueId());
    }

    /**
     * Return a SimpleFeature with the merged geometries and the schema of the input collection but no attribute
     *
     * @param collec input {@link SimpleFeatureCollection}
     * @return a {@link SimpleFeature} with no values
     */
    public static SimpleFeature unionSFC(SimpleFeatureCollection collec, int precision) {
        SimpleFeatureBuilder builder = Schemas.getSFBSchemaWithMultiPolygon(collec.getSchema());
        builder.set(collec.getSchema().getGeometryDescriptor().getLocalName(), Geom.unionPrecisionReduce(collec, precision));
        return builder.buildFeature(Attribute.makeUniqueId());
    }

    /**
     * Return a SimpleFeature with the schema of the collection with an attribute on the corresponding field.
     *
     * @param collec    input {@link SimpleFeatureCollection}
     * @param field     Field name to copy attribute in
     * @param attribute {@link String} to copy in the feature
     * @return The {@link SimpleFeature} with only field and its value
     */
    public static SimpleFeature unionSFC(SimpleFeatureCollection collec, String field, String attribute) {
        String geomName = collec.getSchema().getGeometryDescriptor().getLocalName();
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("union");
        sfTypeBuilder.add(geomName, MultiPolygon.class);
        sfTypeBuilder.add(field, String.class);
        sfTypeBuilder.setDefaultGeometry(geomName);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
        builder.set(geomName, unionSFC(collec));
        builder.set(field, attribute);
        return builder.buildFeature(Attribute.makeUniqueId());
    }

    /**
     * Return a SimpleFeature with a single point as a geometry and the sum of every of the numeric fields of the input collection. SimpleFeature geometries must be the same (we get the first one so if not, no errors will be thrown)
     *
     * @param collec input {@link SimpleFeatureCollection} of same geometry points
     * @return The {@link SimpleFeature} with same schemas and summed numeric values
     */
    public static SimpleFeature unionAttributesOfAPoint(SimpleFeatureCollection collec, StatisticOperation stat) {
        if (collec.size() == 1)
            return collec.features().next();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(collec.getSchema());
        builder.set(collec.getSchema().getGeometryDescriptor().getLocalName(), collec.features().next().getDefaultGeometry());
        for (AttributeDescriptor attDesc : collec.getSchema().getAttributeDescriptors()) {
//            if (attDesc.getClass().getClass().equals(Double.class) || attDesc.getClass().getClass().equals(Integer.class) ||
//                    attDesc.getClass().getClass().equals(Float.class) || attDesc.getClass().getClass().equals(Long.class)) { // attribute is a numeric type, we sum it
            // todo dirty. Find a solution with what's before
            try {
                builder.set(attDesc.getLocalName(), OpOnCollec.getCollectionAttributeDescriptiveStat(collec, attDesc.getLocalName(), stat));
            } catch (NumberFormatException n) {
                builder.set(attDesc.getLocalName(), collec.features().next().getAttribute(attDesc.getLocalName()));
            }
        }
        return builder.buildFeature(Attribute.makeUniqueId());
    }

    /**
     * Create a {@link SimpleFeatureCollection} with features which designed attribute field matches a precise attribute value.
     *
     * @param sFCToDivide SimpleFeatureCollection to sort
     * @param fieldName   field name to select features from
     * @param attribute   wanted field
     * @return a collection with matching features
     * @throws IOException
     */
    public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String fieldName, String attribute) throws IOException {
        String[] attributes = {attribute};
        String[] fieldNames = {fieldName};
        return getSFCPart(sFCToDivide, fieldNames, attributes);
    }

    /**
     * Create a {@link SimpleFeatureCollection} with features which a list of attribute field matches a list of strings. The index of the couple fieldName/attribute must match.
     *
     * @param sFCToDivide SimpleFeatureCollection to sort
     * @param fieldNames  array of field names
     * @param attributes  array of values
     * @return a collection with matching features
     * @throws IOException
     */
    public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String[] fieldNames, String[] attributes)
            throws IOException {
        int shortestIndice = Math.min(fieldNames.length, attributes.length);
        if (fieldNames.length != attributes.length)
            System.out.println("not same number of indices between fieldNames and attributes. Took the shortest one");
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        Arrays.stream(sFCToDivide.toArray(new SimpleFeature[0])).forEach(feat -> {
            boolean add = true;
            for (int i = 0; i < shortestIndice; i++)
                if (!feat.getAttribute(fieldNames[i]).equals(attributes[i])) {
                    add = false;
                    break;
                }
            if (add)
                result.add(feat);
        });
        return result.collection();
    }

    /**
     * clean the {@link SimpleFeatureCollection} of feature which area is inferior to areaMin
     *
     * @param collecIn Input {@link SimpleFeatureCollection}
     * @param areaMin
     * @return the cleaned {@link SimpleFeatureCollection}
     * @throws IOException
     */
    public static SimpleFeatureCollection delTinyParcels(SimpleFeatureCollection collecIn, double areaMin) throws IOException {
        DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
        try (SimpleFeatureIterator it = collecIn.features()) {
            while (it.hasNext()) {
                SimpleFeature feat = it.next();
                try {
                    if (((Geometry) feat.getDefaultGeometry()).getArea() > areaMin)
                        newParcel.add(feat);
                } catch (NullPointerException np) {
                    System.out.println("this feature has no gemoetry : TODO check if normal " + feat);
                }
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        return newParcel.collection();
    }

    public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, File boxFile) throws IOException {
        return selectIntersection(SFCIn, boxFile, 0);
    }

    public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, File boxFile, double distance) throws IOException {
        DataStore dsZone = CollecMgmt.getDataStore(boxFile);
        Geometry bBox = Geom.unionSFC(DataUtilities.collection(dsZone.getFeatureSource(dsZone.getTypeNames()[0]).getFeatures()));
        SimpleFeatureCollection result = selectIntersection(SFCIn, bBox, distance);
        dsZone.dispose();
        return result;
    }

    public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection collection, Geometry geom, double distance) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        return collection.subCollection(ff.dwithin(ff.property(collection.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geom),
                distance, SI.METRE.toString()));
    }

    public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, SimpleFeatureCollection bBox) {
        return selectIntersection(SFCIn, Geom.unionSFC(bBox));
    }

    public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, SimpleFeature feat) {
        return selectIntersection(SFCIn, (Geometry) feat.getDefaultGeometry());
    }

    public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, Geometry bBox) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        return SFCIn.subCollection(ff.intersects(ff.property(SFCIn.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(bBox)));
    }

    /**
     * Sort a SimpleFeatureCollection by its feature's area (must be a collection of polygons). Uses a sorted collection and a stream method.
     *
     * @param sFCToSort SimpleFeature
     * @return The sorted {@link SimpleFeatureCollection}
     * @throws IOException from {@link DefaultFeatureCollection}
     */
    public static SimpleFeatureCollection sortSFCWithArea(SimpleFeatureCollection sFCToSort) throws IOException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SortedMap<Double, SimpleFeature> parcelMap = new TreeMap<>();
        Arrays.stream(sFCToSort.toArray(new SimpleFeature[0])).forEach(parcel -> parcelMap.put(((Geometry) parcel.getDefaultGeometry()).getArea(), parcel));
        for (Map.Entry<Double, SimpleFeature> entry : parcelMap.entrySet())
            result.add(entry.getValue());
        return result.collection();
    }

    /**
     * convert a collection of simple feature (which geometries are either {@link org.locationtech.jts.geom.Polygon} or {@link org.locationtech.jts.geom.MultiPolygon}) to a
     * {@link MultiLineString}. It takes into account the exterior and the interior lines.
     *
     * @param inputSFC input {@link SimpleFeatureCollection}
     * @return A list of {@link LineString}
     */
    public static MultiLineString fromPolygonSFCtoRingMultiLines(SimpleFeatureCollection inputSFC) {
        return Lines.getListLineStringAsMultiLS(fromPolygonSFCtoListRingLines(inputSFC), new GeometryFactory());
    }

    /**
     * Convert a collection of simple feature (which geometries are either {@link Polygon} or {@link MultiPolygon}) to a list of {@link LineString}. It takes into account the
     * exterior and the interior lines.
     *
     * @param inputSFC input {@link SimpleFeatureCollection}
     * @return A list of {@link LineString}
     */
    public static List<LineString> fromPolygonSFCtoListRingLines(SimpleFeatureCollection inputSFC) {
        List<LineString> lines = new ArrayList<>();
        try (SimpleFeatureIterator iterator = inputSFC.features()) {
            while (iterator.hasNext()) {
                Geometry geom = (Geometry) iterator.next().getDefaultGeometry();
                if (geom instanceof MultiPolygon) {
                    for (int i = 0; i < geom.getNumGeometries(); i++) {
                        MultiLineString mls = Lines.getMultiLineString(geom.getGeometryN(i));
                        for (int j = 0; j < mls.getNumGeometries(); j++)
                            lines.add((LineString) mls.getGeometryN(j));
                    }
                } else {
                    MultiLineString mls = Lines.getMultiLineString(geom);
                    for (int j = 0; j < mls.getNumGeometries(); j++)
                        lines.add((LineString) mls.getGeometryN(j));
                }
            }
        }
        return lines;
    }

    /**
     * @param sfcToSort
     * @param sfcIntersection
     * @return
     */
    public static SimpleFeatureCollection getSFCfromSFCIntersection(SimpleFeatureCollection sfcToSort, SimpleFeatureCollection sfcIntersection) {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Geometry geometry = Geom.unionSFC(sfcIntersection);
        SimpleFeatureCollection collec = sfcToSort
                .subCollection(ff.intersects(ff.property(sfcToSort.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
        if (collec.isEmpty())
            return null;
        Arrays.stream(collec.toArray(new SimpleFeature[0])).forEach(theFeature -> {
            Geometry theFeatureGeom = (Geometry) theFeature.getDefaultGeometry();
            if (geometry.contains(theFeatureGeom)
                    || (theFeatureGeom.intersects(geometry) && geometry.intersection(theFeatureGeom).getArea() > theFeatureGeom.getArea() * 0.5))
                result.add(theFeature);
        });
        return result;
    }

    /**
     * Discretize the input {@link SimpleFeatureCollection} by generating a grid and cuting features by it. Should preserve attributes (untested).
     *
     * @param in             Input {@link SimpleFeatureCollection}
     * @param gridResolution Resolution of the grid's mesh
     * @return the discretized {@link SimpleFeatureCollection}
     * @throws IOException
     */
    public static SimpleFeatureCollection gridDiscretize(SimpleFeatureCollection in, int gridResolution) throws IOException {
        DefaultFeatureCollection dfCuted = new DefaultFeatureCollection();
        SimpleFeatureBuilder finalFeatureBuilder = Schemas.getSFBSchemaWithMultiPolygon(in.getSchema());
        SpatialIndexFeatureCollection sifc = new SpatialIndexFeatureCollection(in);
        SimpleFeatureCollection gridFeatures = Grids.createSquareGrid(in.getBounds(), gridResolution).getFeatures();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        String geomName = in.getSchema().getGeometryDescriptor().getLocalName();
        try (SimpleFeatureIterator iterator = gridFeatures.features()) {
            while (iterator.hasNext()) {
                SimpleFeature featureGrid = iterator.next();
                Geometry gridGeometry = (Geometry) featureGrid.getDefaultGeometry();
                SimpleFeatureIterator chosenFeatIterator = sifc.subCollection(ff.bbox(ff.property(geomName), featureGrid.getBounds())).features();
                List<Geometry> list = new ArrayList<>();
                List<Object> attr = new ArrayList<>();
                while (chosenFeatIterator.hasNext()) {
                    SimpleFeature f = chosenFeatIterator.next();
                    Geometry g = (Geometry) f.getDefaultGeometry();
                    if (g.intersects(gridGeometry)) {
                        attr = f.getAttributes();
                        list.add(g);
                    }
                }
                Geometry diffGeom = Geom.scaledGeometryReductionIntersection(Arrays.asList(Geom.unionGeom(list), gridGeometry));
                if (diffGeom != null && !diffGeom.isEmpty()) {
                    for (Object a : attr)
                        finalFeatureBuilder.add(a);
                    finalFeatureBuilder.set(geomName, diffGeom);
                    dfCuted.add(finalFeatureBuilder.buildFeature(Attribute.makeUniqueId()));
                }
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        return dfCuted.collection();
    }

    /**
     * Get the value of a feature's field from a SimpleFeatureCollection that intersects a given Simplefeature (that is most of the time, a parcel or building). If the given
     * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection.
     *
     * @param geometry  input {@link Geometry}
     * @param sfc       Input {@link SimpleFeatureCollection}
     * @param fieldName The name of the field in which to look for the attribute
     * @return the wanted filed from the (most) intersecting {@link SimpleFeature}}
     */
    public static String getIntersectingFieldFromSFC(Geometry geometry, SimpleFeatureCollection sfc, String fieldName) {
        SimpleFeature feat = getIntersectingSimpleFeatureFromSFC(geometry, sfc);
        return feat != null ? (String) feat.getAttribute(fieldName) : null;
    }

    /**
     * Get the {@link SimpleFeature} out of a {@link SimpleFeatureCollection} that intersects a given Geometry (that is most of the time, a parcel or building). If the given
     * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection.
     *
     * @param geometry input {@link Geometry}
     * @param inputSFC
     * @return the (most) intersecting {@link SimpleFeature}}
     */
    public static SimpleFeature getIntersectingSimpleFeatureFromSFC(Geometry geometry, SimpleFeatureCollection inputSFC) {
        try {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
            SortedMap<Double, SimpleFeature> index = new TreeMap<>();
            SimpleFeatureCollection collec = inputSFC
                    .subCollection(ff.intersects(ff.property(inputSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
            if (collec.isEmpty()) {
                // logger.debug("intersection between " + geometry + " and " + parcels.getSchema().getName() + " null");
                return null;
            }
            try (SimpleFeatureIterator collecIt = collec.features()) {
                while (collecIt.hasNext()) {
                    SimpleFeature theFeature = collecIt.next();
                    Geometry theFeatureGeom = ((Geometry) theFeature.getDefaultGeometry()).buffer(1);
                    if (theFeatureGeom.contains(geometry))
                        return theFeature;
                        // if the parcel is in between two features, we put the feature in a sorted collection
                    else if (theFeatureGeom.intersects(geometry))
                        index.put(Geom.scaledGeometryReductionIntersection(Arrays.asList(theFeatureGeom, geometry)).getArea(), theFeature);
                }
            } catch (Exception problem) {
                problem.printStackTrace();
            }
            return index.size() > 0 ? index.get(index.lastKey()) : null;
        } catch (Exception e) {
            return getIntersectingSimpleFeatureFromSFC(geometry, inputSFC, new PrecisionModel(10));
        }
    }

    /**
     * Get the {@link SimpleFeature} out of a {@link SimpleFeatureCollection} that intersects a given Geometry (that is most of the time, a parcel or building). If the given
     * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection. Reduce the precision of the {@link Geometry}s
     *
     * @param geometry       input {@link Geometry}
     * @param inputSFC
     * @param precisionModel
     * @return the (most) intersecting {@link SimpleFeature}}
     */
    public static SimpleFeature getIntersectingSimpleFeatureFromSFC(Geometry geometry, SimpleFeatureCollection inputSFC,
                                                                    PrecisionModel precisionModel) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Geometry givenFeatureGeom = GeometryPrecisionReducer.reduce(geometry, precisionModel);
        SortedMap<Double, SimpleFeature> index = new TreeMap<>();
        SimpleFeatureCollection collec = inputSFC
                .subCollection(ff.intersects(ff.property(inputSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
        if (collec.isEmpty()) {
            // logger.debug("intersection between " + geometry + " and " + parcels.getSchema().getName() + " null");
            return null;
        }
        try (SimpleFeatureIterator collecIt = collec.features()) {
            while (collecIt.hasNext()) {
                SimpleFeature theFeature = collecIt.next();
                Geometry theFeatureGeom = GeometryPrecisionReducer.reduce((Geometry) theFeature.getDefaultGeometry(), precisionModel).buffer(1);
                if (theFeatureGeom.contains(givenFeatureGeom))
                    return theFeature;
                    // if the parcel is in between two features, we put the feature in a sorted
                    // collection
                else if (theFeatureGeom.intersects(givenFeatureGeom))
                    index.put(Objects.requireNonNull(Geom.scaledGeometryReductionIntersection(Arrays.asList(theFeatureGeom, givenFeatureGeom))).getArea(), theFeature);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        return index.size() > 0 ? index.get(index.lastKey()) : null;
    }
}
