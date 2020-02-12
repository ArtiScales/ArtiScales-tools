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
import java.util.stream.Stream;

import org.geotools.coverage.util.IntersectUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Vectors {

//	public static void main(String[] args) throws Exception {
//		ShapefileDataStore sds = new ShapefileDataStore(new File("/home/thema/donnees/dataGeo/nonUrbaSys.shp").toURI().toURL());
//	SimpleFeatureCollection sfc = sds.getFeatureSource().getFeatures();
//		exportGeom(unionSFC(sfc), new File("/home/thema/donnees/dataGeo/nonUrbaSysUnion.shp"));
//		sds.dispose();
//	}

	// ShapefileDataStore shpDSCells = new ShapefileDataStore(
	// (new
	// File("/media/mcolomb/Data_2/resultFinal/stab/extra/intersecNU-ZC/NUManuPhyDecoup.shp")).toURI().toURL());
	// SimpleFeatureCollection cellsCollection =
	// shpDSCells.getFeatureSource().getFeatures();
	//
	// Geometry cellsUnion = unionSFC(cellsCollection);
	//
	// SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
	// CoordinateReferenceSystem sourceCRS = CRS.decode(
	// "EPSG:2154");sfTypeBuilder.setName("testType");sfTypeBuilder.setCRS(sourceCRS);sfTypeBuilder.add("the_geom",MultiPolygon.class);sfTypeBuilder.setDefaultGeometry("the_geom");
	//
	// SimpleFeatureBuilder sfBuilder = new
	// SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	// DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
	//
	// sfBuilder.add(cellsUnion);
	// SimpleFeature feature = sfBuilder.buildFeature("0");toSplit.add(feature);
	//
	// shpDSCells.dispose();
	//
	// exportSFC(toSplit.collection(), new File("/home/mcolomb/tmp/mergeSmth.shp"));
	// }

	public static File mergeVectFiles(List<File> file2MergeIn, File f) throws Exception {
		return mergeVectFiles(file2MergeIn, f, true);
	}
	
	public static File mergeVectFiles(List<File> file2MergeIn,File fileOut, boolean keepAttributes) throws Exception {
		return  mergeVectFiles(file2MergeIn, fileOut, null, keepAttributes) ;
	}


	public static File mergeVectFiles(List<File> file2MergeIn, File fileOut, File empriseFile, boolean keepAttributes) throws Exception {
//		org.geotools.util.logging.Logging.getLogger("org.geotools.feature").setLevel(Level.OFF);
		// stupid basic checkout
		if (file2MergeIn.isEmpty()) {
			System.out.println("mergeVectFiles: list empty, " + fileOut + " null");
			return null;
		}

		// verify that every shapefile exists and remove them from the list if not
		int nbFile = file2MergeIn.size();
		for (int i = 0; i < nbFile; i++) {
			if (!file2MergeIn.get(i).exists()) {
				System.out.println(file2MergeIn.get(i) + " doesn't exists");
				file2MergeIn.remove(i);
				i--;
				nbFile--;
			}
		}

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();

		// check to prevent event in case of a willing of keeping attributes
		File fRef = file2MergeIn.get(0);

		ShapefileDataStore dSref = new ShapefileDataStore(fRef.toURI().toURL());
		SimpleFeatureType schemaRef = dSref.getFeatureSource().getFeatures().getSchema();

		dSref.dispose();
		
		// sfBuilder used only if number of attributes's the same but with different
		// schemas
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.init(schemaRef);
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder defaultSFBuilder = new SimpleFeatureBuilder(featureType);
		boolean sameSchemas = true;
		lookOutAttribute: if (keepAttributes) {
			// check if the schemas of the shape are the same and if not, if they have the
			// same number of attributes
			int nbAttr = schemaRef.getAttributeCount();
			for (File f : file2MergeIn) {
				if (f.equals(fRef)) {
					continue;
				}
				ShapefileDataStore dSComp = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureType schemaComp = dSComp.getFeatureSource().getFeatures().getSchema();
				if (!schemaRef.equals(schemaComp)) {
					System.out.println(f + " have not the same schema as " + fRef
							+ ". Try to still add attribute if number is the same but output may be fuzzy");
					sameSchemas = false;
				}
				if (nbAttr != schemaComp.getAttributeCount()) {
					System.out.println(
							"Not the same amount of attributes in the shapefile : Output won't have any attributes");
					keepAttributes = false;
					break lookOutAttribute;
				}
				dSComp.dispose();
			}
		}
		dSref.dispose();

		for (File file : file2MergeIn) {
			ShapefileDataStore SDSParcel = new ShapefileDataStore(file.toURI().toURL());
			SimpleFeatureCollection SFCParcel = SDSParcel.getFeatureSource().getFeatures();
			SimpleFeatureIterator parcelIt;
			try {
				parcelIt = SFCParcel.features();
			} catch (Exception e) {
				System.out.println("magic number wrong : " + file);
				SDSParcel.dispose();
				continue;
			}
			if (keepAttributes) {
				// easy way
				if (sameSchemas) {
					System.out.println("keep attribute & same schema");
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
				}
				// complicate case : if they doesn't have the exactly same schema but the same
				// number of attributes, we add every attribute regarding their position
				else {
					try {
						while (parcelIt.hasNext()) {
							SimpleFeature feat = parcelIt.next();
							Object[] attr = new Object[feat.getAttributeCount() - 1];
							for (int h = 1; h < feat.getAttributeCount(); h++) {
								attr[h - 1] = feat.getAttribute(h);
							}
							defaultSFBuilder.add((Geometry) feat.getDefaultGeometry());
							SimpleFeature feature = defaultSFBuilder.buildFeature(null, attr);
							newParcel.add(feature);
						}
					} catch (Exception problem) {
						problem.printStackTrace();
					} finally {
						parcelIt.close();
					}
				}
			} else {
				// if we don't want to keep attributes, we create features out of new features
				// containing only geometry
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						defaultSFBuilder.set("the_geom", feat.getDefaultGeometry());
						newParcel.add(defaultSFBuilder.buildFeature(null));
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
			}
			SDSParcel.dispose();
		}

		SimpleFeatureCollection output = newParcel.collection();
		Vectors.exportSFC(output, new File("/tmp/saqs"));
		if (empriseFile != null && empriseFile.exists()) {
			output = Vectors.cropSFC(output, empriseFile);
		}
		return Vectors.exportSFC(output, fileOut);
	}

	/**
	 * TODO look that algo and make a description
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File discretizeShp(File inFile, File outFile, String name)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		
		ShapefileDataStore sds = new ShapefileDataStore(inFile.toURI().toURL());
		SimpleFeatureCollection input = sds.getFeatureSource().getFeatures();
		
		DefaultFeatureCollection dfCuted = new DefaultFeatureCollection();
		
//		SimpleFeatureTypeBuilder finalBuilder = new SimpleFeatureTypeBuilder();
//		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
//		finalBuilder.setName("road");
//		finalBuilder.setCRS(sourceCRS);
//		finalBuilder.add("the_geom", MultiPolygon.class);
//		finalBuilder.setDefaultGeometry("the_geom");
//
//		SimpleFeatureType featureFinalType = finalBuilder.buildFeatureType();
		SimpleFeatureBuilder finalFeatureBuilder = Schemas.getBasicSchemaMultiPolygon(name+"-discretized");

		SpatialIndexFeatureCollection sifc = new SpatialIndexFeatureCollection(input);
		SimpleFeatureCollection gridFeatures = Grids.createSquareGrid(input.getBounds(), 500).getFeatures();
		SimpleFeatureIterator iterator = gridFeatures.features();
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		int finalId = 0;
		try {
			while (iterator.hasNext()) {
				SimpleFeature featureGrid = iterator.next();
				Geometry gridGeometry = (Geometry) featureGrid.getDefaultGeometry();
				BBOX boundsCheck = ff.bbox(ff.property("the_geom"), featureGrid.getBounds());
				SimpleFeatureIterator chosenFeatIterator = sifc.subCollection(boundsCheck).features();
				Geometry unionGeom = null;
				List<Geometry> list = new ArrayList<>();
				while (chosenFeatIterator.hasNext()) {
					SimpleFeature f = chosenFeatIterator.next();
					Geometry g = (Geometry) f.getDefaultGeometry();

					if (g.intersects(gridGeometry)) {

						list.add(g);
					}
				}
				Geometry coll = gridGeometry.getFactory()
						.createGeometryCollection(list.toArray(new Geometry[list.size()]));
				try {
					Geometry y = coll.union();
					if (y.isValid())
						coll = y;
				} catch (Exception e) {
				}
				unionGeom = IntersectUtils.intersection(coll, gridGeometry);
				try {
					Geometry y = unionGeom.buffer(0);
					if (y.isValid()) {
						unionGeom = y;
					}
				} catch (Exception e) {
				}
				if (unionGeom != null) {
					finalFeatureBuilder.add(unionGeom);
					SimpleFeature feature = finalFeatureBuilder.buildFeature(String.valueOf(finalId++));
					dfCuted.add(feature);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			iterator.close();
		}
		return exportSFC(dfCuted.collection(), outFile);
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

	public static SimpleFeatureCollection delTinyParcels(SimpleFeatureCollection collecIn, double areaMin)
			throws Exception {

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

	public static File exportGeom(Geometry geom, File fileName)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

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

	private static void coord2D(Coordinate c) {
		if (!CoordinateXY.class.isInstance(c))
			c.setZ(Double.NaN);
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName, SimpleFeatureType ft)
			throws IOException {

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

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
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
						g.apply((Coordinate c) -> coord2D(c));
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
//				toExport.accepts((Feature f) -> System.out.println(((SimpleFeature)f).getDefaultGeometry()), null);
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

	public static Geometry unionSFC(SimpleFeatureCollection collection) throws IOException {
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

	public static Geometry unionGeom(List<Geometry> lG) throws IOException {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = lG.stream();
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}

	public static File cropSFC(File inFile, File empriseFile, File tmpFile) throws MalformedURLException, IOException {

		ShapefileDataStore envSDS = new ShapefileDataStore(empriseFile.toURI().toURL());
		ShapefileDataStore inSDS = new ShapefileDataStore(inFile.toURI().toURL());

		SimpleFeatureCollection result = cropSFC(inSDS.getFeatureSource().getFeatures(),
				envSDS.getFeatureSource().getFeatures());
		envSDS.dispose();
		inSDS.dispose();
		// return exportSFC(result, new File(tmpFile, inFile.getName().replace(".shp",
		// "")+"-croped.shp"));
		return exportSFC(result, new File(tmpFile, inFile.getName()));
	}

	public static SimpleFeatureCollection cropSFC(SimpleFeatureCollection inSFC, File empriseFile)
			throws MalformedURLException, IOException {
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

	public static SimpleFeatureCollection snapDatas(File fileIn, SimpleFeatureCollection box) throws Exception {

		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection inCollection = shpDSIn.getFeatureSource().getFeatures();

		Geometry bBox = unionSFC(box);

		return snapDatas(inCollection, bBox);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile) throws Exception {
		return snapDatas(SFCIn, boxFile, 0);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile, double distance)
			throws Exception {
		ShapefileDataStore shpDSZone = new ShapefileDataStore(boxFile.toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();
		Geometry bBox = unionSFC(zoneCollection);
		if (distance != 0) {
			bBox = bBox.buffer(distance);
		}
		shpDSZone.dispose();
		return snapDatas(SFCIn, bBox);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, SimpleFeatureCollection bBox)
			throws Exception {
		Geometry geomBBox = unionSFC(bBox);
		return snapDatas(SFCIn, geomBBox);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, Geometry bBox) throws Exception {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryInPropertyName = SFCIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter filterIn = ff.intersects(ff.property(geometryInPropertyName), ff.literal(bBox));
		SimpleFeatureCollection inTown = DataUtilities.collection(SFCIn.subCollection(filterIn));
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

	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String code, String attribute)
			throws IOException {
		String[] attributes = { attribute };

		return getSFCPart(sFCToDivide, code, attributes);
	}

	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String code,
			String[] attributes) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator it = sFCToDivide.features();
		try {
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				String attribute = "";
				for (String a : attributes) {
					attribute = attribute + ((String) ft.getAttribute(a));
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

	// public static HashMap<String, SimpleFeatureCollection>
	// divideSFCIntoPart(SimpleFeatureCollection sFCToDivide, String attribute) {
	// HashMap<String, SimpleFeatureCollection> result = new HashMap<String,
	// SimpleFeatureCollection>();
	//
	// SimpleFeatureIterator it = sFCToDivide.features();
	// try {
	// while (it.hasNext()) {
	// SimpleFeature ft = it.next();
	// String key = (String) ft.getAttribute(attribute);
	// DefaultFeatureCollection temp = new DefaultFeatureCollection();
	// if (result.containsKey(key)) {
	// temp.addAll(result.remove(key));
	// temp.add(ft);
	// result.put(key, temp.collection());
	// } else {
	// temp.add(ft);
	// result.put(key, temp.collection());
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// it.close();
	// }
	// return result;
	// }
}
