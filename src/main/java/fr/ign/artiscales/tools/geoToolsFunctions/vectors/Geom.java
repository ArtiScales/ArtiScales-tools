package fr.ign.artiscales.tools.geoToolsFunctions.vectors;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Geom {
//	public static void main(String[] args) throws Exception {
////		WKTReader w = new WKTReader();
////		Geometry g1 = w.read(
////				"MULTIPOLYGON(((673440.63000000000465661 6861804.29600000008940697, 673460.79399999999441206 6861808.7630000002682209, 673462.83400000003166497 6861767.34200000017881393, 673463.73699999996460974 6861748.78500000014901161, 673447.48300000000745058 6861747.04299999959766865, 673445.22400000004563481 6861765.99000000022351742, 673445.17700000002514571 6861766.35099999979138374, 673440.63000000000465661 6861804.29600000008940697)))");
////		Geometry g2 = w.read(
////				"MULTIPOLYGON(((673421.24399999994784594 6861799.9419999998062849, 673440.63000000000465661 6861804.29600000008940697, 673445.17700000002514571 6861766.35099999979138374, 673445.22400000004563481 6861765.99000000022351742, 673447.48300000000745058 6861747.04299999959766865, 673431.51699999999254942 6861744.02900000009685755, 673430.20799999998416752 6861751.13599999994039536, 673428.08600000001024455 6861762.71600000001490116, 673427.69499999994877726 6861764.86799999978393316, 673421.24399999994784594 6861799.9419999998062849)))");
////		Geometry g3 = w.read(
////				"MULTIPOLYGON(((673408.16000000003259629 6861730.82299999985843897, 673418.18299999996088445 6861733.1380000002682209, 673425.25300000002607703 6861734.76699999999254942, 673429.11499999999068677 6861718.72400000039488077, 673429.4529999999795109 6861717.3030000003054738, 673435.87399999995250255 6861690.64499999955296516, 673423.22699999995529652 6861688.84200000017881393, 673421.97600000002421439 6861688.63300000037997961, 673413.99600000004284084 6861713.00499999988824129, 673413.67500000004656613 6861713.96700000017881393, 673411.13199999998323619 6861721.74399999994784594, 673410.95600000000558794 6861722.28600000031292439, 673408.16000000003259629 6861730.82299999985843897)))");
//		ShapefileDataStore sd = new ShapefileDataStore(((new File("/tmp/tmp.shp")).toURI().toURL()));
//		System.out.println(createBufferBorder(sd.getFeatureSource().getFeatures()));
//	}
	

	
	/**
	 * Export a simple geometry in a shapeFile
	 * 
	 * @param geom
	 * @param fileName
	 * @return A ShapeFile containing the exported {@link Geometry}
	 * @throws IOException
	 */
	public static File exportGeom(Geometry geom, File fileName) throws IOException {
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaMultiPolygon("geom");
		sfBuilder.add(geom);
		SimpleFeature feature = sfBuilder.buildFeature(Attribute.makeUniqueId());
		DefaultFeatureCollection dFC = new DefaultFeatureCollection();
		dFC.add(feature);
		return CollecMgmt.exportSFC(dFC.collection(), fileName);
	}
	
	/**
	 * Export a list of geometries in a shapeFile.
	 * 
	 * @param geoms {@link List} of objects extending {@link Geometry} type
	 * @param fileName
	 * @return A ShapeFile containing the exported {@link Geometry}
	 * @throws IOException
	 */
	public static File exportGeom(List<? extends Geometry> geoms, File fileName) throws IOException {
		return CollecMgmt.exportSFC(geomsToCollec(geoms, Schemas.getBasicSchemaMultiPolygon(CollecMgmt.getDefaultGeomName())), fileName);
	}
	
	/**
	 * Export a list of {@link Geometry}s in a {@link DefaultFeatureCollection}.
	 * 
	 * @param geoms
	 *            List of objects extending {@link Geometry} type
	 * @param sfBuilder
	 *            Builder for simple features
	 * @return the collection of {@link Geometry}s 
	 * @throws IOException
	 */
	public static SimpleFeatureCollection geomsToCollec(List<? extends Geometry > geoms, SimpleFeatureBuilder sfBuilder) throws IOException {
		DefaultFeatureCollection dFC = new DefaultFeatureCollection();
		for (Geometry geom : geoms) {
			sfBuilder.add(geom);
			dFC.add(sfBuilder.buildFeature(Attribute.makeUniqueId()));
		}
		return dFC.collection();
	}
	
	/**
	 * Make an intersection of a list of {@link Geometry} and catch {@link TopologyException} to redo the intersection with a reduced precision. Precision reduction comes from 2 to
	 * 1000.
	 * 
	 * @param geoms
	 *            {@link List} of {@link Geometry}
	 * @return the intersected {@link Geometry}
	 */
	public static Geometry scaledGeometryReductionIntersection(List<Geometry> geoms) {
		try {
			Geometry geomResult = geoms.get(0);
			for (int i = 1; i < geoms.size(); i++)
				geomResult = geomResult.intersection(geoms.get(i));
			return geomResult;
		} catch (TopologyException e) {
			try {
				Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(2));
				for (int i = 1; i < geoms.size(); i++)
					geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(2)));
				return geomResult;
			} catch (TopologyException ex) {
				try {
					Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(10));
					for (int i = 1; i < geoms.size(); i++)
						geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(10)));
					return geomResult;
				} catch (TopologyException ee) {
					try {
						Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(100));
						for (int i = 1; i < geoms.size(); i++)
							geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(100)));
						return geomResult;
					} catch (TopologyException eee) {
						try {
							System.out.println("last hope for precision reduction");
							Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(1000));
							for (int i = 1; i < geoms.size(); i++)
								geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(1000)));
							return geomResult;
						} catch (TopologyException eeee) {
							return null;
						}
					}
				}
			}
		}
	}

	public static Geometry unionPrecisionReduce(List<Geometry> collection, int scale) {
		if (collection.size() == 1)
			return GeometryPrecisionReducer.reduce(collection.get(0), new PrecisionModel(scale));
		GeometryFactory factory = new GeometryFactory();
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(
				collection.stream().map(g -> GeometryPrecisionReducer.reduce(g, new PrecisionModel(scale))).collect(Collectors.toList()));
		return geometryCollection.union();
	}
	
	public static Geometry unionPrecisionReduce(SimpleFeatureCollection collection, double scale) {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[collection.size()])).map(
				sf -> GeometryPrecisionReducer.reduce((Geometry) sf.getDefaultGeometry(), new PrecisionModel(scale)));
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}

	public static Geometry unionSFC(SimpleFeatureCollection collection) {
		if (collection.size() == 1)
			return (Geometry) collection.features().next().getDefaultGeometry();
		try {
			return unionPrecisionReduce(collection, 1000);
		} catch (TopologyException e) {
			try {
				return unionPrecisionReduce(collection, 100);
			} catch (TopologyException ee) {
				try {
					return unionPrecisionReduce(collection, 10);
				} catch (TopologyException eee) {
					try {
						return unionPrecisionReduce(collection, 2);
					} catch (TopologyException eeee) {
						try {
							return unionPrecisionReduce(collection, 0.1);
						} catch (TopologyException eeeee) {
							return unionPrecisionReduce(collection, 0.001);
						}
					}
				}
			}
		}
	}

	public static List<Geometry> unionTouchingGeometries(List<Geometry> geomsIn){
		List<Geometry> result = new ArrayList<>();
		for (Geometry gIn : geomsIn) {
			List<Geometry> intersectingGeom = result.stream().filter(g -> g.intersects(gIn)).collect(Collectors.toList());
			if (intersectingGeom.isEmpty()) {
				result.add(gIn);
			} else {
				result.removeAll(intersectingGeom);
				intersectingGeom.add(gIn);
				result.add(unionGeom(intersectingGeom));
			}
		}
		return result; 
	}
	
	public static Geometry unionGeom(List<? extends Geometry> lG) {
		if (lG.size() == 1)
			return lG.get(0);
		GeometryFactory factory = new GeometryFactory();
		Stream<? extends Geometry> s = lG.stream();
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}
	
//	public static Geometry unionGeom(Geometry g1, Geometry g2) {
//		if (g1 instanceof GeometryCollection) {
//			if (g2 instanceof GeometryCollection)
//				return union((GeometryCollection) g1, (GeometryCollection) g2);
//			else {
//				List<Geometry> ret = unionGeom((GeometryCollection) g1, g2);
//				return g1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
//			}
//		} else {
//			if (g2 instanceof GeometryCollection) {
//				List<Geometry> ret = unionGeom((GeometryCollection) g2, g1);
//				return g1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
//			} else
//				return g1.intersection(g2);
//		}
//	}
//
//	private static List<Geometry> unionGeom(GeometryCollection gc, Geometry g) {
//		List<Geometry> ret = new ArrayList<Geometry>();
//		final int size = gc.getNumGeometries();
//		for (int i = 0; i < size; i++) {
//			Geometry g1 = (Geometry) gc.getGeometryN(i);
//			collect(g1.union(g), ret);
//		}
//		return ret;
//	}
//
//	/**
//	 * Helper method for {@link #union(Geometry, Geometry) union(Geometry,
//	 * Geometry)}
//	 */
//	private static GeometryCollection union(GeometryCollection gc1, GeometryCollection gc2) {
//		List<Geometry> ret = new ArrayList<Geometry>();
//		final int size = gc1.getNumGeometries();
//		for (int i = 0; i < size; i++) {
//			Geometry g1 = (Geometry) gc1.getGeometryN(i);
//			List<Geometry> partial = unionGeom(gc2, g1);
//			ret.addAll(partial);
//		}
//		return gc1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
//	}
//
//	/**
//	 * Adds into the <TT>collector</TT> the Geometry <TT>g</TT>, or, if <TT>g</TT>
//	 * is a GeometryCollection, every geometry in it.
//	 *
//	 * @param g         the Geometry (or GeometryCollection to unroll)
//	 * @param collector the Collection where the Geometries will be added into
//	 */
//	private static void collect(Geometry g, List<Geometry> collector) {
//		if (g instanceof GeometryCollection) {
//			GeometryCollection gc = (GeometryCollection) g;
//			for (int i = 0; i < gc.getNumGeometries(); i++) {
//				Geometry loop = gc.getGeometryN(i);
//				if (!loop.isEmpty())
//					collector.add(loop);
//			}
//		} else {
//			if (!g.isEmpty())
//				collector.add(g);
//		}
//	}
	
	/**
	 * Return the intersecting geometry with the highest area of intersection.
	 * 
	 * @param lG
	 *            Input list of geometries
	 * @param geom
	 *            Intersection polygon
	 * @return the largest {@link Geometry}
	 */
	public static Geometry getBiggestIntersectingGeometry(List<? extends Geometry> lG, Geometry geom) {
		HashMap<Geometry, Double> result = new HashMap<>();
		for (Geometry g : lG) {
			double area = (Objects.requireNonNull(scaledGeometryReductionIntersection(Arrays.asList(g, geom))).getArea());
			if (area > 0)
				result.put(g, area);
		}
		List<Entry<Geometry, Double>> sorted = result.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
		// if list is empty, we return null
		if (sorted.isEmpty())
			return null;
		return sorted.get(sorted.size() - 1).getKey();
	}
}
