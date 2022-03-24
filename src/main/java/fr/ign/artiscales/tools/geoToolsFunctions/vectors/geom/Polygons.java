package fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Polygons {
    public static DefaultFeatureCollection addSimplePolygonialGeometries(SimpleFeatureBuilder sfBuilder, String geometryOutputName, List<Geometry> geoms) {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        for (Geometry g : geoms)
            result.addAll((FeatureCollection<?, ?>) addSimplePolygonialGeometry(sfBuilder, result, geometryOutputName, g));
        return result;
    }

    public static DefaultFeatureCollection addSimplePolygonialGeometry(SimpleFeatureBuilder sfBuilder, DefaultFeatureCollection result,
                                                                       String geometryOutputName, Geometry geom) {
        return addSimplePolygonialGeometry(sfBuilder, result, geometryOutputName, geom, Attribute.makeUniqueId());
    }

    public static DefaultFeatureCollection addSimplePolygonialGeometry(SimpleFeatureBuilder sfBuilder, DefaultFeatureCollection result,
                                                                       String geometryOutputName, Geometry geom, String id) {
        if (geom instanceof MultiPolygon) {
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                sfBuilder.set(geometryOutputName, geom.getGeometryN(i));
                result.add(sfBuilder.buildFeature(id));
            }
        } else if (geom instanceof GeometryCollection) {
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                Geometry g = geom.getGeometryN(i);
                if (g instanceof Polygon) {
                    sfBuilder.set(sfBuilder.getFeatureType().getGeometryDescriptor().getName(), g.buffer(1).buffer(-1));
                    result.add(sfBuilder.buildFeature(id));
                }
            }
        } else if (geom instanceof Polygon) {
            sfBuilder.set(geometryOutputName, geom);
            result.add(sfBuilder.buildFeature(id));
        }
        return result;
    }

    /**
     * Get {@link Geometry} as a Polygon. If it already is a Polygon, returns it. If a MultiPolygon, returns the biggest Polygon Geometry of the list.
     *
     * @param geom input {@link Geometry}
     * @return the {@link Geometry} as a {@link Polygon}
     */
    public static Polygon getPolygon(Geometry geom) {
        List<Polygon> lP = getPolygons(geom);
        return lP.size() == 1 ? lP.get(0) : (Polygon) Geom.getBiggestIntersectingGeometry(lP, geom);
    }

    /**
     * Get geometry as a Polygon. If it already is a Polygon, returns it. If a MultiPolygon, return the list of polygons.
     *
     * @param geom input geometry
     * @return a list of Polygon Geometry
     */
    public static List<Polygon> getPolygons(Geometry geom) {
        if (geom instanceof Polygon)
            return Collections.singletonList((Polygon) geom);
        else if (geom instanceof MultiPolygon) {
            List<Polygon> lG = new ArrayList<>();
            for (int i = 0; i < geom.getNumGeometries(); i++)
                lG.add((Polygon) geom.getGeometryN(i));
            return lG;
        } else if (geom instanceof GeometryCollection) {
            List<Polygon> lG = new ArrayList<>();
            for (int i = 0; i < geom.getNumGeometries(); i++)
                lG.addAll(getPolygons(geom.getGeometryN(i)));
            return lG;
        } else {
//			System.out.println("getPolygons() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
            return Collections.emptyList();
        }
    }

    public static Polygon polygonDifference(List<Polygon> a, List<Polygon> b) {
        List<Polygon> p = getPolygons(FeaturePolygonizer.getDifference(a, b)); // .stream().map(x -> (Polygon) x).collect(Collectors.toList());
        if (p.size() != 1)
            return null;
        return p.get(0);
    }

    public static Polygon polygonUnionWithoutHoles(List<Polygon> list, GeometryPrecisionReducer reducer) {
        Polygon union = polygonUnion(list, reducer);
        return Objects.requireNonNull(union).getFactory().createPolygon(union.getExteriorRing().getCoordinates());
    }

    public static Polygon polygonUnion(List<Polygon> list, GeometryPrecisionReducer reducer) {
        if (list.isEmpty())
            return null;
        Geometry p = new CascadedPolygonUnion(list.stream().filter(Objects::nonNull).map(reducer::reduce).toList()).union();
        try {
            return (Polygon) p;
        } catch (Exception e) {
            System.out.println("Polygons.polygonUnion() : cannot return MultiPolygon " + p);
            Polygon largest = (Polygon) p.getGeometryN(0);
            for (int i = 1; i < p.getNumGeometries(); i++)
                if (p.getGeometryN(i).getArea() > largest.getArea())
                    largest = (Polygon) p.getGeometryN(i);
            System.out.println("Return the largest polygon : " + largest);
            return largest;
        }
    }

    /**
     * Get a polygon and return a multiplolygon
     *
     * @param geom input {@link Geometry}
     * @return The {@link Geometry} as a {@link MultiPolygon}
     */
    public static MultiPolygon getMultiPolygonGeom(Geometry geom) {
        // force the cast into multipolygon
        if (geom instanceof Polygon) {
            GeometryFactory gf = new GeometryFactory();
            Polygon[] pols = {(Polygon) geom};
            return gf.createMultiPolygon(pols);
        } else if (geom instanceof MultiPolygon)
            return (MultiPolygon) geom;
        else {
            System.out.println("getMultiPolygonGeom() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
            return null;
        }
    }
}
