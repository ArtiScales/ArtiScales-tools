package fr.ign.cogit.geoToolsFunctions.vectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	
	public static Geometry getPolygon(Geometry geom) {
		if (geom instanceof Polygon) {
			return geom ;
		} else if (geom instanceof MultiPolygon) {
			List<Geometry> lG = new ArrayList<Geometry>(); 			
			for (int i = 0 ; i<((MultiPolygon) geom).getNumGeometries(); i++ ) {
				lG.add(geom.getGeometryN(i));
			}
    		 return Geom.unionGeom(lG);
		} else {
			System.out.println("getPolygonGeom() problem with type of the geometry " + geom + " : " + geom.getGeometryType());
			return null;
		}
	}
}
