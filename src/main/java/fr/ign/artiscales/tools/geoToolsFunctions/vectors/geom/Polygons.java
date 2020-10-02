package fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;

public class Polygons {
	public static DefaultFeatureCollection addSimplePolygonialGeometries(SimpleFeatureBuilder sfBuilder, DefaultFeatureCollection result,
			String geometryOutputName, List<Geometry> geoms) {
		for (Geometry g : geoms)
			result = addSimplePolygonialGeometry(sfBuilder, result, geometryOutputName, g);
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
	 * @param geom
	 * @return the {@link Geometry} as a {@link Polygon}
	 */
	public static Polygon getPolygon(Geometry geom) {
		return (Polygon) Geom.getBiggestIntersectingGeometry(getPolygons(geom), geom);
	}

	/**
	 * Get geometry as a Polygon. If it already is a Polygon, returns it. If a MultiPolygon, return the list of polygons.
	 * 
	 * @param geom
	 *            input geometry
	 * @return a list of Polygon Geometry
	 */
	public static List<Polygon> getPolygons(Geometry geom) {
		if (geom instanceof Polygon)
			return Arrays.asList((Polygon) geom);
		else if (geom instanceof MultiPolygon) {
			List<Polygon> lG = new ArrayList<Polygon>();
			for (int i = 0; i < ((MultiPolygon) geom).getNumGeometries(); i++)
				lG.add((Polygon) geom.getGeometryN(i));
			return lG;
		} else if (geom instanceof GeometryCollection) {
			List<Polygon> lG = new ArrayList<Polygon>();
			for (int i = 0; i < ((GeometryCollection) geom).getNumGeometries(); i++)
				lG.addAll(getPolygons(geom.getGeometryN(i)));
			return lG;
		} else {
//			System.out.println("getPolygons() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
			return Collections.emptyList();
		}
	}

	public static Polygon polygonDifference(List<Polygon> a, List<Polygon> b) {
		Geometry difference = FeaturePolygonizer.getDifference(a, b);
		List<Polygon> p = getPolygons(difference); // .stream().map(x -> (Polygon) x).collect(Collectors.toList());
		if (p.size() != 1) {
			System.out.println("polygonDifference():" + p.size() + " polygons");
			p.forEach(pp -> System.out.println(pp));
			return null;
		}
		return p.get(0);
	}

	public static Polygon polygonUnionWithoutHoles(List<Polygon> list, GeometryPrecisionReducer reducer) {
		Polygon union = polygonUnion(list, reducer);
		return union.getFactory().createPolygon(union.getExteriorRing().getCoordinates());
	}

	public static Polygon polygonUnion(List<Polygon> list, GeometryPrecisionReducer reducer) {
		if (list.isEmpty())
			return null;
		List<Geometry> reducedList = list.stream().filter(g -> g != null).map(g -> reducer.reduce(g)).collect(Collectors.toList());
		return (Polygon) new CascadedPolygonUnion(reducedList).union();
	}

	/**
	 * Get a polygon and return a multiplolygon
	 * 
	 * @param geom
	 * @return The {@link Geometry} as a {@link MultiPolygon}
	 */
	public static MultiPolygon getMultiPolygonGeom(Geometry geom) {
		// force the cast into multipolygon
		if (geom instanceof Polygon) {
			GeometryFactory gf = new GeometryFactory();
			Polygon[] pols = { (Polygon) geom };
			return gf.createMultiPolygon(pols);
		} else if (geom instanceof MultiPolygon)
			return (MultiPolygon) geom;
		else {
			System.out.println("getMultiPolygonGeom() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
			return null;
		}
	}
}
