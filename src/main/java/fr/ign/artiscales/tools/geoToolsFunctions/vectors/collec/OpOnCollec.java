package fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec;

import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;

public class OpOnCollec {

    /**
     * Get statistics about a field of a collection
     *
     * @param sfc input {@link SimpleFeatureCollection}
     *            * @param attribute
     * @return
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

    public static int getCollectionAttributeCount(SimpleFeatureCollection sfc, String attributeName, String attribute) {
//		return Arrays.stream(sfc.toArray(new SimpleFeature[0])).filter(feat -> String.valueOf(feat.getAttribute(attributeName)).equals(attribute)).count();
        return getCollectionAttributeCount(sfc, attributeName, attribute, CommonFactoryFinder.getFilterFactory2());
    }

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
        // import of the cells of MUP-City outputs
        try (SimpleFeatureIterator cellsCollectionIt = CollecTransform.selectIntersection(inputSFC, geom).features()) {
            while (cellsCollectionIt.hasNext())
                if (((Geometry) cellsCollectionIt.next().getDefaultGeometry()).intersects(geom))
                    return true;
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        return false;
    }
}
