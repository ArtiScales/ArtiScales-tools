package fr.ign.cogit.geoToolsFunctions.vectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.geoToolsFunctions.Schemas;

public class Geom {
//	public static void main(String[] args) throws Exception {
//		WKTReader w = new WKTReader();
//		Geometry g1 = w.read(
//				"MULTIPOLYGON(((673440.63000000000465661 6861804.29600000008940697, 673460.79399999999441206 6861808.7630000002682209, 673462.83400000003166497 6861767.34200000017881393, 673463.73699999996460974 6861748.78500000014901161, 673447.48300000000745058 6861747.04299999959766865, 673445.22400000004563481 6861765.99000000022351742, 673445.17700000002514571 6861766.35099999979138374, 673440.63000000000465661 6861804.29600000008940697)))");
//		Geometry g2 = w.read(
//				"MULTIPOLYGON(((673421.24399999994784594 6861799.9419999998062849, 673440.63000000000465661 6861804.29600000008940697, 673445.17700000002514571 6861766.35099999979138374, 673445.22400000004563481 6861765.99000000022351742, 673447.48300000000745058 6861747.04299999959766865, 673431.51699999999254942 6861744.02900000009685755, 673430.20799999998416752 6861751.13599999994039536, 673428.08600000001024455 6861762.71600000001490116, 673427.69499999994877726 6861764.86799999978393316, 673421.24399999994784594 6861799.9419999998062849)))");
//		Geometry g3 = w.read(
//				"MULTIPOLYGON(((673408.16000000003259629 6861730.82299999985843897, 673418.18299999996088445 6861733.1380000002682209, 673425.25300000002607703 6861734.76699999999254942, 673429.11499999999068677 6861718.72400000039488077, 673429.4529999999795109 6861717.3030000003054738, 673435.87399999995250255 6861690.64499999955296516, 673423.22699999995529652 6861688.84200000017881393, 673421.97600000002421439 6861688.63300000037997961, 673413.99600000004284084 6861713.00499999988824129, 673413.67500000004656613 6861713.96700000017881393, 673411.13199999998323619 6861721.74399999994784594, 673410.95600000000558794 6861722.28600000031292439, 673408.16000000003259629 6861730.82299999985843897)))");
//		System.out.println(getBiggestIntersectingGeometry(Arrays.asList(g1, g2), g3));
//	}
	public static DefaultFeatureCollection addSimpleGeometry(SimpleFeatureBuilder sfBuilder,
			DefaultFeatureCollection result, String geometryOutputName, Geometry geom) {
		return addSimpleGeometry(sfBuilder, result, geometryOutputName, geom, null);
	}

	public static DefaultFeatureCollection addSimpleGeometry(SimpleFeatureBuilder sfBuilder,
			DefaultFeatureCollection result, String geometryOutputName, Geometry geom, String id) {
		if (geom instanceof MultiPolygon) {
			for (int i = 0; i < geom.getNumGeometries(); i++) {
				sfBuilder.set(geometryOutputName, geom.getGeometryN(i));
				result.add(sfBuilder.buildFeature(id));
			}
		} else if (geom instanceof GeometryCollection) {
			for (int i = 0; i < geom.getNumGeometries(); i++) {
				Geometry g = geom.getGeometryN(i);
				if (g instanceof Polygon) {
					sfBuilder.set("the_geom", g.buffer(1).buffer(-1));
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
	 * export a simple geometry in a shapeFile
	 * @param geom
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File exportGeom(Geometry geom, File fileName)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaMultiPolygon("geom");
		sfBuilder.add(geom);
		SimpleFeature feature = sfBuilder.buildFeature(null);
		DefaultFeatureCollection dFC = new DefaultFeatureCollection();
		dFC.add(feature);
		return Collec.exportSFC(dFC.collection(), fileName);
	}
	


	public static Geometry scaledGeometryReductionIntersection(List<Geometry> geoms) {
		try {
			Geometry geomResult = geoms.get(0);
			for (int i = 1; i < geoms.size(); i++) {
				geomResult = geomResult.intersection(geoms.get(i));
			}
			return geomResult;
		} catch (TopologyException e) {
			try {
				Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(1000));
				for (int i = 1; i < geoms.size(); i++) {
					geomResult = geomResult
							.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(1000)));
				}
				return geomResult;
			} catch (TopologyException ex) {
				try {
					Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(100));
					for (int i = 1; i < geoms.size(); i++) {
						geomResult = geomResult
								.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(100)));
					}
					return geomResult;
				} catch (TopologyException ee) {
					try {
						Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(10));
						for (int i = 1; i < geoms.size(); i++) {
							geomResult = geomResult.intersection(
									GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(10)));
						}
						return geomResult;
					} catch (TopologyException eee) {
						try {
							System.out.println("last hope for precision reduction");
							Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(1));
							for (int i = 1; i < geoms.size(); i++) {
								geomResult = geomResult.intersection(
										GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(1)));
							}
							return geomResult;
						} catch (TopologyException eeee) {
							return null;
						}
					}
				}
			}
		}
	}

	public static Geometry unionPrecisionReduce(SimpleFeatureCollection collection, int scale) {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[collection.size()])).map(
				sf -> GeometryPrecisionReducer.reduce((Geometry) sf.getDefaultGeometry(), new PrecisionModel(scale)));
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}

	public static Geometry unionSFC(SimpleFeatureCollection collection) {
		if (collection.size() == 1) {
			return (Geometry) collection.features().next().getDefaultGeometry();
		}
		try {
			Geometry union = unionPrecisionReduce(collection, 1000);
			return union;
		} catch (TopologyException e) {
			try {
				System.out.println("precision reduced");
				Geometry union = unionPrecisionReduce(collection, 100);
				return union;
			} catch (TopologyException ee) {
				System.out.println("precision reduced again");
				Geometry union = unionPrecisionReduce(collection, 10);
				return union;
			}
		}
	}

	public static Geometry unionGeom(List<Geometry> lG) {
		if (lG.size() == 1) {
			return lG.get(0);
		}
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = lG.stream();
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}
	
	public static Geometry unionGeom(Geometry g1, Geometry g2) {
		if (g1 instanceof GeometryCollection) {
			if (g2 instanceof GeometryCollection) {
				return union((GeometryCollection) g1, (GeometryCollection) g2);
			} else {
				List<Geometry> ret = unionGeom((GeometryCollection) g1, g2);
				return g1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
			}
		} else {
			if (g2 instanceof GeometryCollection) {
				List<Geometry> ret = unionGeom((GeometryCollection) g2, g1);
				return g1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
			} else {
				return g1.intersection(g2);
			}
		}
	}

	private static List<Geometry> unionGeom(GeometryCollection gc, Geometry g) {
		List<Geometry> ret = new ArrayList<Geometry>();
		final int size = gc.getNumGeometries();
		for (int i = 0; i < size; i++) {
			Geometry g1 = (Geometry) gc.getGeometryN(i);
			collect(g1.union(g), ret);
		}
		return ret;
	}

	/**
	 * Helper method for {@link #union(Geometry, Geometry) union(Geometry,
	 * Geometry)}
	 */
	private static GeometryCollection union(GeometryCollection gc1, GeometryCollection gc2) {
		List<Geometry> ret = new ArrayList<Geometry>();
		final int size = gc1.getNumGeometries();
		for (int i = 0; i < size; i++) {
			Geometry g1 = (Geometry) gc1.getGeometryN(i);
			List<Geometry> partial = unionGeom(gc2, g1);
			ret.addAll(partial);
		}
		return gc1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
	}

	/**
	 * Adds into the <TT>collector</TT> the Geometry <TT>g</TT>, or, if <TT>g</TT>
	 * is a GeometryCollection, every geometry in it.
	 *
	 * @param g         the Geometry (or GeometryCollection to unroll)
	 * @param collector the Collection where the Geometries will be added into
	 */
	private static void collect(Geometry g, List<Geometry> collector) {
		if (g instanceof GeometryCollection) {
			GeometryCollection gc = (GeometryCollection) g;
			for (int i = 0; i < gc.getNumGeometries(); i++) {
				Geometry loop = gc.getGeometryN(i);
				if (!loop.isEmpty())
					collector.add(loop);
			}
		} else {
			if (!g.isEmpty())
				collector.add(g);
		}
	}
	
	/**
	 * get a polygon and return a multiplolygon
	 * @param geom
	 * @return
	 */
	public static Geometry getMultiPolygonGeom(Geometry geom) {
		// force the cast into multipolygon
		if (geom instanceof Polygon) {
			GeometryFactory gf = new GeometryFactory();
			Polygon[] pols = { (Polygon) geom };
			return geom = gf.createMultiPolygon(pols);
		} else if (geom instanceof MultiPolygon) {
			return geom;
		} else {
			System.out.println("getMultiPolygonGeom() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
			return null;
		}
	}
	
	/**
	 * Return the intersecting geometry with the highest area of intersection.
	 * 
	 * @param lG:
	 *            input list of geometries
	 * @param geom:
	 *            intersection polygon
	 * @return the largest
	 * @throws Exception
	 */
	public static Geometry getBiggestIntersectingGeometry(List<Geometry> lG, Geometry geom) {
		HashMap<Geometry, Double> result = new HashMap<Geometry, Double>();
		for (Geometry g : lG) {
			double area = (scaledGeometryReductionIntersection(Arrays.asList(g, geom)).getArea());
			if (area > 0) {
				result.put(g, area);
			}
		}
		List<Entry<Geometry, Double>> sorted = result.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
		// if list is empty, we return null
		if (sorted.isEmpty()) {
			return null;
		}
		return sorted.get(sorted.size() - 1).getKey();
	}
	
	/**
	 * Get geometry as a Polygon. If it already is a Polygon, returns it. If a MultiPolygon, returns the biggest Polygon Geometry of the list. 
	 * @param geom
	 * @return
	 */
	public static Geometry getPolygon(Geometry geom) {
		return getBiggestIntersectingGeometry(getPolygons(geom),geom);
	}
	
	/**
	 * Get geometry as a Polygon. If it already is a Polygon, returns it. If a MultiPolygon, return the list of polygons. 
	 * @param geom input geometry 
	 * @return a list of Polygon Geometry
	 */
	public static List<Geometry> getPolygons(Geometry geom) {
		if (geom instanceof Polygon) {
			return Arrays.asList(geom) ;
		} else if (geom instanceof MultiPolygon) {
			List<Geometry> lG = new ArrayList<Geometry>(); 			
			for (int i = 0 ; i<((MultiPolygon) geom).getNumGeometries(); i++ ) {
				lG.add(geom.getGeometryN(i));
			}
    		 return lG;
		} else {
			System.out.println("getPolygonGeom() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
			return null;
		}
	}
}
