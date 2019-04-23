package fr.ign.cogit.GTFunctions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

public class Vectors {

	// public static void main(String[] args) throws Exception {
	// List<File> zob = new ArrayList<File>();
	// File root = new File("/home/ubuntu/boulot/these/result0313/ParcelSelectionDepot/DDense/variantMvGrid1/");
	// for (File f : root.listFiles()) {
	// zob.add(new File(f, "parcelOut-" + f.getName() + ".shp"));
	//
	// }
	// mergeVectFiles(zob, new File("/tmp/dqz.shp"), true);
	// }

	// ShapefileDataStore shpDSCells = new ShapefileDataStore((new File("/media/mcolomb/Data_2/resultFinal/stab/extra/intersecNU-ZC/NUManuPhyDecoup.shp")).toURI().toURL());
	// SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();
	//
	// Geometry cellsUnion = unionSFC(cellsCollection);
	//
	// SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
	// CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
	// sfTypeBuilder.setName("testType");
	// sfTypeBuilder.setCRS(sourceCRS);
	// sfTypeBuilder.add("the_geom", MultiPolygon.class);
	// sfTypeBuilder.setDefaultGeometry("the_geom");
	//
	// SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	// DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
	//
	// sfBuilder.add(cellsUnion);
	// SimpleFeature feature = sfBuilder.buildFeature("0");
	// toSplit.add(feature);
	//
	// shpDSCells.dispose();
	// exportSFC(toSplit.collection(), new File("/home/mcolomb/tmp/mergeSmth.shp"));
	// }

	public static File mergeVectFiles(List<File> file2MergeIn, File f) throws Exception {
		return mergeVectFiles(file2MergeIn, f, true);
	}

	public static File mergeVectFiles(List<File> file2MergeIn, File f, boolean keepAttribute) throws Exception {
		org.geotools.util.logging.Logging.getLogger("org.geotools.feature").setLevel(Level.OFF);

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("merge");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureBuilder bt = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		for (File file : file2MergeIn) {
			ShapefileDataStore SDSParcel = new ShapefileDataStore(file.toURI().toURL());
			SimpleFeatureIterator parcelIt;
			try {
				parcelIt = SDSParcel.getFeatureSource().getFeatures().features();
			} catch (Exception e) {
				System.out.println("magic number wrong : " + file);
				SDSParcel.dispose();
				continue;
			}
			if (keepAttribute) {
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						newParcel.add(feat);
						// System.out.println("schema of merged shape : "+feat.getFeatureType());
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
			} else {
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						bt.set("the_geom", feat.getDefaultGeometry());
						newParcel.add(bt.buildFeature(null));
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
			}
			SDSParcel.dispose();
		}

		if (!newParcel.isEmpty()) {
			Vectors.exportSFC(newParcel.collection(), f);
		}
		return f;
	}

	/**
	 * clean the shapefile of feature which area is inferior to areaMin
	 * 
	 * @param fileIn
	 * @param areaMin
	 * @return
	 * @throws Exception
	 */
	public static File delTinyParcels(File fileIn, double areaMin) throws Exception {
		ShapefileDataStore SDSParcel = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection sfc = SDSParcel.getFeatureSource().getFeatures();
		SimpleFeatureCollection result = delTinyParcels(sfc, areaMin);
		Vectors.exportSFC(result, fileIn);
		SDSParcel.dispose();
		return fileIn;
	}

	public static DefaultFeatureCollection addSimpleGeometry(SimpleFeatureBuilder sfBuilder, DefaultFeatureCollection result,
			String geometryOutputName, Geometry geom) {
		if (geom instanceof MultiPolygon) {
			for (int i = 0; i < geom.getNumGeometries(); i++) {
				sfBuilder.set(geometryOutputName, geom.getGeometryN(i));
				result.add(sfBuilder.buildFeature(null));
			}
		} else if (geom instanceof GeometryCollection) {
			for (int i = 0; i < geom.getNumGeometries(); i++) {
				Geometry g = geom.getGeometryN(i);
				if (g instanceof Polygon) {
					sfBuilder.set("the_geom", g.buffer(1).buffer(-1));
					result.add(sfBuilder.buildFeature(null));
				}
			}
		} else if (geom instanceof Polygon) {
			sfBuilder.set(geometryOutputName, geom);
			result.add(sfBuilder.buildFeature(null));
		}
		return result;

	}

	public static SimpleFeatureCollection delTinyParcels(SimpleFeatureCollection collecIn, double areaMin) throws Exception {

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
		SimpleFeatureIterator it = collecIn.features();

		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				try {
					if (((Geometry) feat.getDefaultGeometry()).getArea() > areaMin) {
						newParcel.add(feat);
					}
				} catch (NullPointerException np) {
					System.out.println("this feature has no gemoetry : TODO check if normal " + feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		return newParcel.collection();
	}

	public static File exportGeom(Geometry geom, File fileName) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("someGeom");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		DefaultFeatureCollection DFC = new DefaultFeatureCollection();

		sfBuilder.add(geom);
		SimpleFeature feature = sfBuilder.buildFeature("0");
		DFC.add(feature);

		return exportSFC(DFC.collection(), fileName);
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName) throws IOException {
		if (toExport.isEmpty()) {
			System.out.println(fileName.getName() + " is empty");
			return fileName;
		}
		return exportSFC(toExport, fileName, toExport.getSchema());
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName, SimpleFeatureType ft) throws IOException {

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		if (!fileName.getName().endsWith(".shp")) {
			fileName = new File(fileName + ".shp");
		}
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileName.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(ft);

		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		// System.out.println("SHAPE:" + featureSource.getSchema());

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			// System.out.println(featureStore.getSchema());
			try {
				featureStore.addFeatures(toExport);
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
		newDataStore.dispose();
		return fileName;
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
					geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(1000)));
				}
				return geomResult;
			} catch (TopologyException ex) {
				try {
					Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(100));
					for (int i = 1; i < geoms.size(); i++) {
						geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(100)));
					}
					return geomResult;
				} catch (TopologyException ee) {
					try {
						Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(10));
						for (int i = 1; i < geoms.size(); i++) {
							geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(10)));
						}
						return geomResult;
					} catch (TopologyException eee) {
						try {
							System.out.println("last hope for precision reduction");
							Geometry geomResult = GeometryPrecisionReducer.reduce(geoms.get(0), new PrecisionModel(1));
							for (int i = 1; i < geoms.size(); i++) {
								geomResult = geomResult.intersection(GeometryPrecisionReducer.reduce(geoms.get(i), new PrecisionModel(1)));
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

	public static Geometry unionSFC(SimpleFeatureCollection collection) throws IOException {
		try {
			GeometryFactory factory = new GeometryFactory();
			Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0]))
					.map(sf -> GeometryPrecisionReducer.reduce((Geometry) sf.getDefaultGeometry(), new PrecisionModel(1000)));
			GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
			Geometry union = geometryCollection.union();
			return union;
		} catch (TopologyException e) {
			try {
				System.out.println("precision reduced");
				GeometryFactory factory = new GeometryFactory();
				Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0]))
						.map(sf -> GeometryPrecisionReducer.reduce((Geometry) sf.getDefaultGeometry(), new PrecisionModel(100)));
				GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
				return geometryCollection.union();
			} catch (TopologyException ee) {
				System.out.println("precision reduced again");
				GeometryFactory factory = new GeometryFactory();
				Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0]))
						.map(sf -> GeometryPrecisionReducer.reduce((Geometry) sf.getDefaultGeometry(), new PrecisionModel(10)));
				GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
				return geometryCollection.union();
			}
		}
	}

	public static Geometry unionGeom(List<Geometry> lG) throws IOException {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = lG.stream();
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}

	public static File cropSFC(File inFile, File empriseFile, File tmpFile) throws MalformedURLException, IOException {
	
		ShapefileDataStore envSDS = new ShapefileDataStore(empriseFile.toURI().toURL());
		ShapefileDataStore inSDS = new ShapefileDataStore(inFile.toURI().toURL());

		SimpleFeatureCollection result = cropSFC(inSDS.getFeatureSource().getFeatures(), envSDS.getFeatureSource().getFeatures());
		envSDS.dispose();
		inSDS.dispose();
//		return exportSFC(result, new File(tmpFile, inFile.getName().replace(".shp", "")+"-croped.shp"));
		return exportSFC(result, new File(tmpFile, inFile.getName()));
	}
	
	public static SimpleFeatureCollection cropSFC(SimpleFeatureCollection inSFC, File empriseFile) throws MalformedURLException, IOException {
		if (inSFC.isEmpty()) {
			return inSFC;
		}
		ShapefileDataStore envSDS = new ShapefileDataStore(empriseFile.toURI().toURL());
		SimpleFeatureCollection result = cropSFC(inSFC, envSDS.getFeatureSource().getFeatures());
		envSDS.dispose();
		return result;
	}

	public static SimpleFeatureCollection cropSFC(SimpleFeatureCollection inSFC, SimpleFeatureCollection empriseSFC)
			throws MalformedURLException, IOException {
		if (inSFC.isEmpty()) {
			return inSFC;
		}
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		ReferencedEnvelope env = empriseSFC.getBounds();
		String geometryPropertyName = inSFC.getSchema().getGeometryDescriptor().getLocalName();
		Filter filter = ff.bbox(ff.property(geometryPropertyName), env);
		return DataUtilities.collection(inSFC.subCollection(filter));
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
	 * Helper method for {@link #union(Geometry, Geometry) union(Geometry, Geometry)}
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
	 * Adds into the <TT>collector</TT> the Geometry <TT>g</TT>, or, if <TT>g</TT> is a GeometryCollection, every geometry in it.
	 *
	 * @param g
	 *            the Geometry (or GeometryCollection to unroll)
	 * @param collector
	 *            the Collection where the Geometries will be added into
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

	public static File snapDatas(File fileIn, File bBoxFile, File fileOut) throws Exception {

		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection inCollection = shpDSIn.getFeatureSource().getFeatures();

		// load the file to make the bbox and selectin with
		ShapefileDataStore shpDSZone = new ShapefileDataStore(bBoxFile.toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();
		Geometry bBox = unionSFC(zoneCollection);
		shpDSZone.dispose();
		return exportSFC(snapDatas(inCollection, bBox), fileOut);
	}

	public static SimpleFeatureCollection snapDatas(File fileIn, SimpleFeatureCollection collec) throws Exception {

		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection inCollection = shpDSIn.getFeatureSource().getFeatures();

		Geometry bBox = unionSFC(collec);

		return snapDatas(inCollection, bBox);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile) throws Exception {
		return snapDatas(SFCIn, boxFile, 0);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile, double distance) throws Exception {

		ShapefileDataStore shpDSZone = new ShapefileDataStore(boxFile.toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();
		Geometry bBox = unionSFC(zoneCollection);
		if (distance != 0) {
			bBox = bBox.buffer(distance);
		}
		shpDSZone.dispose();
		return snapDatas(SFCIn, bBox);

	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, Geometry bBox) throws Exception {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryInPropertyName = SFCIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter filterIn = ff.intersects(ff.property(geometryInPropertyName), ff.literal(bBox));
		SimpleFeatureCollection inTown = SFCIn.subCollection(filterIn);

		return inTown;

	}

	public static void copyShp(String name, File fromFile, File destinationFile) throws IOException {
		for (File f : fromFile.listFiles()) {
			if (f.getName().startsWith(name)) {
				FileOutputStream out = new FileOutputStream(new File(destinationFile, f.getName()));
				Files.copy(f.toPath(), out);
				out.close();
			}
		}
	}
	
	public static void copyShp(String name, String nameOut, File fromFile, File destinationFile) throws IOException {
		for (File f : fromFile.listFiles()) {
			if (f.getName().startsWith(name)) {
				FileOutputStream out = new FileOutputStream(new File(destinationFile, f.getName()));
				Files.copy(f.toPath(), out);
				out.close();
			}
		}
	}

	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String code, String attribute) throws IOException {
		String[] attributes = {attribute};

		return getSFCPart(sFCToDivide, code, attributes);
	}

	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String code, String[] attributes) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator it = sFCToDivide.features();
		try {
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				String attribute ="";
				for (String a : attributes) {
					attribute= attribute + ((String) ft.getAttribute(a)); 
				}
				if (attribute.equals(code)) {
					result.add(ft);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		return result.collection();
	}

//	public static HashMap<String, SimpleFeatureCollection> divideSFCIntoPart(SimpleFeatureCollection sFCToDivide, String attribute) {
//		HashMap<String, SimpleFeatureCollection> result = new HashMap<String, SimpleFeatureCollection>();
//
//		SimpleFeatureIterator it = sFCToDivide.features();
//		try {
//			while (it.hasNext()) {
//				SimpleFeature ft = it.next();
//				String key = (String) ft.getAttribute(attribute);
//				DefaultFeatureCollection temp = new DefaultFeatureCollection();
//				if (result.containsKey(key)) {
//					temp.addAll(result.remove(key));
//					temp.add(ft);
//					result.put(key, temp.collection());
//				} else {
//					temp.add(ft);
//					result.put(key, temp.collection());
//				}
//			}
//		} catch (Exception problem) {
//			problem.printStackTrace();
//		} finally {
//			it.close();
//		}
//		return result;
//	}
}
