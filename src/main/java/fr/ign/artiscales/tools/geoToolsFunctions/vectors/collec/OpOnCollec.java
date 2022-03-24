package fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.locationtech.jts.algorithm.match.HausdorffSimilarityMeasure;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OpOnCollec {

//    public static void main(String[] args) throws IOException {
//        sortDiffGeom(new File("/home/mc/workspace/parcelmanager/src/main/resources/ParcelComparison/parcel2003.gpkg"),
//                new File("/home/mc/workspace/parcelmanager/src/main/resources/ParcelComparison/parcel2018.gpkg"),new File("/tmp"), true, true);
//    }

    /**
     * Get statistics about a field of a collection
     *
     * @param sfc input collection
     * @param attribute the field's name on which doing the statistical study
     * @param stat given type of statistic
     * @return the statistic
     */
    public static double getCollectionAttributeDescriptiveStat(SimpleFeatureCollection sfc, String attribute, StatisticOperation stat) {
        try {
            DescriptiveStatistics ds = new DescriptiveStatistics();
            try (SimpleFeatureIterator polyIt = sfc.features()) {
                while (polyIt.hasNext()) {
                    String s = String.valueOf(polyIt.next().getAttribute(attribute));
                    ds.addValue(Double.parseDouble((s == null || s.equals("null")) ? "0" : s));
                }
            } catch (ClassCastException e) {
                System.out.println("Cannot calculate mean for " + attribute + ". Might be that the filed values are not numbers");
                return 0;
            }
            if (stat == StatisticOperation.MEAN)
                return ds.getMean();
            if (stat == StatisticOperation.MEDIAN)
                return ds.getPercentile(50);
            if (stat == StatisticOperation.STANDEV)
                return ds.getStandardDeviation();
            if (stat == StatisticOperation.SUM)
                return ds.getSum();
            throw new NullPointerException();
        } catch (ClassCastException e) {
            System.out.println("Cannot calculate mean for " + attribute + ". Might be that the filed values are not numbers");
            return 0;
        }
    }

    /**
     * Get the number of feature for which a given attribute is equal to a fixed value
     * @param sfc input collection
     * @param attributeName field's name
     * @param attribute attribute's value
     * @return the number of feature which attribute matches the given value
     */
    public static int getCollectionAttributeCount(SimpleFeatureCollection sfc, String attributeName, String attribute) {
//		return Arrays.stream(sfc.toArray(new SimpleFeature[0])).filter(feat -> String.valueOf(feat.getAttribute(attributeName)).equals(attribute)).count();
        return getCollectionAttributeCount(sfc, attributeName, attribute, CommonFactoryFinder.getFilterFactory2());
    }

    /**
     * Get the number of feature for which a given attribute is equal to a fixed value
     * @param sfc input collection
     * @param attributeName field's name
     * @param attribute attribute's value
     * @param ff if a filter factory already exists (that may fast the process if count is made numerous times)
     * @return the number of feature which attribute matches the given value
     */

    public static int getCollectionAttributeCount(SimpleFeatureCollection sfc, String attributeName, String attribute, FilterFactory2 ff) {
//		return Arrays.stream(sfc.toArray(new SimpleFeature[0])).filter(feat -> String.valueOf(feat.getAttribute(attributeName)).equals(attribute)).count();
        return sfc.subCollection(ff.like(ff.property(attributeName), attribute)).size();
    }


    /**
     * Return the sum of area of every features of a simpleFeatureCollection
     *
     * @param parcels input {@link SimpleFeatureCollection}
     * @return The sum of area of every features
     */
    public static double area(SimpleFeatureCollection parcels) {
        double totArea = 0.0;
        try (SimpleFeatureIterator parcelIt = parcels.features()) {
            while (parcelIt.hasNext())
                totArea = totArea + ((Geometry) parcelIt.next().getDefaultGeometry()).getArea();
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        return totArea;
    }

    /**
     * Check if a given {@link SimpleFeature} intersects the input {@link SimpleFeatureCollection}.
     *
     * @param inputFeat input {@link SimpleFeature}
     * @param inputSFC  input {@link SimpleFeatureCollection}
     * @return true if there's an intersection, false otherwise
     */
    public static boolean isFeatIntersectsSFC(SimpleFeature inputFeat, SimpleFeatureCollection inputSFC) {
        Geometry geom = (Geometry) inputFeat.getDefaultGeometry();
        try (SimpleFeatureIterator cellsCollectionIt = Objects.requireNonNull(CollecTransform.selectIntersection(inputSFC, geom)).features()) {
            while (cellsCollectionIt.hasNext())
                if (((Geometry) cellsCollectionIt.next().getDefaultGeometry()).intersects(geom))
                    return true;
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        return false;
    }

    /**
     *
     * Method that compares two collections of simple features sort them regarding id they differ or not.
     * It compares the parcels area of the reference parcel to the ones that are intersected.
     * If they are similar with a 3% error rate, we conclude that they are the same.
     * This method creates two geographic files (shapefile or geopackages, regarding the projects default format) in the parcelOutFolder:
     * <ul>
     * <li><b>same</b> contains the reference parcels that have not evolved</li>
     * <li><b>notSame</b> contains the reference parcels that have evolved</li>
     * </ul>
     *
     * @param parcelRefFile          The reference parcel plan
     * @param parcelToCompareFile    The parcel plan to compare
     * @param parcelOutFolder        Folder where are stored the result geopackages
     * @param overwrite if false and files already exists, we cancel the process (and return null)
     * @return an array of {@link SimpleFeatureCollection} with initial {@link SimpleFeature}. Indices represents:
     * <ul>
     *     <li><b>0:</b><i>same</i> geometries</li>
     *     <li><b>1:</b><i>notSame</i> geometries</li>
     * </ul>
     * @throws IOException reading and writing files.
     */
    public static SimpleFeatureCollection[] sortDiffGeom(File parcelRefFile, File parcelToCompareFile,File parcelOutFolder,boolean addDeletedGeom, boolean overwrite) throws IOException {
        File fSame = new File(parcelOutFolder, "same" + CollecMgmt.getDefaultGISFileType());
        File fNotSame = new File(parcelOutFolder, "notSame" + CollecMgmt.getDefaultGISFileType());

        if (!overwrite && fSame.exists()  && fNotSame.exists() ) {
            System.out.println("markDiffParcel(...) already calculated");
            return null;
        }

        DataStore dsParcelToCompare = CollecMgmt.getDataStore(parcelToCompareFile);
        SimpleFeatureCollection parcelToCompare = dsParcelToCompare.getFeatureSource(dsParcelToCompare.getTypeNames()[0]).getFeatures();
        DataStore dsRef = CollecMgmt.getDataStore(parcelRefFile);
        SimpleFeatureCollection parcelRef = dsRef.getFeatureSource(dsRef.getTypeNames()[0]).getFeatures();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        PropertyName pName = ff.property(parcelRef.getSchema().getGeometryDescriptor().getLocalName());
        DefaultFeatureCollection same = new DefaultFeatureCollection();
        DefaultFeatureCollection notSame = new DefaultFeatureCollection();
        HausdorffSimilarityMeasure hausSim = new HausdorffSimilarityMeasure();
        // for every reference parcels, we check if an intersection with the intersection compared parcels are +/- 5% of its area and their shapes are similar regarding to the Hausdorff distance mesure
        try (SimpleFeatureIterator itRef = parcelRef.features()) {
            refParcel:
            while (itRef.hasNext()) {
                SimpleFeature pRef = itRef.next();
                Geometry geomPRef = (Geometry) pRef.getDefaultGeometry();
                double geomArea = geomPRef.getArea();
                //for every intersected parcels, we check if it is close to (as tiny geometry changes)
                SimpleFeatureCollection parcelsCompIntersectRef = parcelToCompare.subCollection(ff.intersects(pName, ff.literal(geomPRef)));
                try (SimpleFeatureIterator itParcelIntersectRef = parcelsCompIntersectRef.features()) {
                    while (itParcelIntersectRef.hasNext()) {
                        Geometry g = (Geometry) itParcelIntersectRef.next().getDefaultGeometry();
                        double inter = Objects.requireNonNull(Geom.scaledGeometryReductionIntersection(Arrays.asList(geomPRef, g))).getArea();
                        // if there are parcel intersection and a similar area, we conclude that parcel haven't changed. We put it in the \"same\" collection and stop the search
                        if ((inter > 0.95 * geomArea && inter < 1.05 * geomArea) || hausSim.measure(g, geomPRef) > 0.95) {
                            same.add(pRef);
                            continue refParcel;
                        }
                    }
                } catch (Exception problem) {
                    problem.printStackTrace();
                }
                // we check if the parcel has been intentionally deleted by generating new polygons (same technique of area comparison, but with a way smaller error bound)
                // if it has been cleaned, we don't add it to no additional parcels
                List<Geometry> geomList = Arrays.stream(parcelsCompIntersectRef.toArray(new SimpleFeature[0])).map(x -> (Geometry) x.getDefaultGeometry()).collect(Collectors.toList());
                geomList.add(geomPRef);
                for (Polygon polygon : FeaturePolygonizer.getPolygons(geomList))
                    if (!addDeletedGeom && polygon.getArea() > geomArea * 0.9 && polygon.getArea() < geomArea * 1.1 && polygon.buffer(1).contains(geomPRef))
                        continue refParcel;
                notSame.add(pRef);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        CollecMgmt.exportSFC(same, fSame);
        CollecMgmt.exportSFC(notSame, fNotSame);
        dsRef.dispose();
        dsParcelToCompare.dispose();
        return new SimpleFeatureCollection[]{same, notSame};
    }
}
