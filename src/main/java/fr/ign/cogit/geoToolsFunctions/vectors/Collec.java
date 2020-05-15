package fr.ign.cogit.geoToolsFunctions.vectors;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import org.geotools.grid.Grids;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.FilterVisitor;

import fr.ign.cogit.geoToolsFunctions.Attribute;
import fr.ign.cogit.geoToolsFunctions.Schemas;

public class Collec {
	
//	public static void main(String[] args) throws Exception {
//		ShapefileDataStore shpDSParcel = new ShapefileDataStore((new File("/home/ubuntu/workspace/ParcelManager/src/main/resources/testData/parcelle.shp")).toURI().toURL());
//		SimpleFeatureCollection parcel = shpDSParcel.getFeatureSource().getFeatures();
//		WKTReader w = new WKTReader();
////		Geometry g = w.read("MULTIPOLYGON (((937159.28 6688272.91, 937171.18 6688277.86, 937175.76 6688271.59, 937192.91 6688247.08, 937194.51 6688244.77, 937210.81 6688220.7, 937272.28 6688116.74, 937259.65 6688107.96, 937259.64 6688107.96, 937230.38 6688159.07, 937200.23 6688210.7, 937193.91 6688221.42, 937181.49 6688240.06, 937163.63 6688265.93, 937159.28 6688272.91)))");
//		Geometry g = w.read("MultiPolygon(((937183.97284507064614445 6688228.66999999899417162, 937203.95069014118053019 6688237.79283098503947258, 937210.53298591589555144 6688230.05574647802859545, 937190.5551408453611657 6688216.66019718162715435, 937183.97284507064614445 6688228.66999999899417162)))");
//		System.out.println(getSimpleFeatureFromSFC(g, parcel));
//	}
	
	/**
	 * Return the sum of area of every features of a simpleFeatureCollection
	 * 
	 * @param parcels
	 *            input {@link SimpleFeatureCollection}
	 * @return The sum of area of every features
	 * @throws IOException
	 */
	public static double area(SimpleFeatureCollection parcels) throws IOException {
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
	 * clean the {@link SimpleFeatureCollection} of feature which area is inferior to areaMin
	 * 
	 * @param collecIn
	 *            Input {@link SimpleFeatureCollection}
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

	/**
	 * Export a simple feature collection. If the shapefile already exists , either overwrite it or merge it with the existing shapefile.
	 * 
	 * @param toExport
	 * @param fileOut
	 * @param overwrite
	 *            If true, the shapefile is overwritten if it exists. If false, the shapefiles (ne existing and the export) are merged together with the
	 *            {@link fr.ign.cogit.geoToolsFunctions.vectors.Shp#mergeVectFiles(List, File)} method
	 * @return the ShapeFile
	 * @throws Exception
	 */
	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, boolean overwrite) throws Exception {
		if (toExport.isEmpty()) {
			System.out.println(fileOut.getName() + " is empty");
			return fileOut;
		}
		List<File> file2MergeIn = new ArrayList<File>();
		// copyShp(String shpName, String newShpName, File fromFolder, File toFolder)

		if (fileOut.exists() && !overwrite) {
			String fileName = fileOut.getName().substring(0, fileOut.getName().length()-4);
			File newFile = new File(fileOut.getParentFile(), fileName + "tmp.shp");
			Shp.copyShp(fileName, fileName + "tmp", fileOut.getParentFile(), fileOut.getParentFile());
			file2MergeIn.add(newFile);
		}
		File datFile = exportSFC(toExport, fileOut, toExport.getSchema());
		file2MergeIn.add(datFile);
		File result = Shp.mergeVectFiles(file2MergeIn, fileOut);
		Shp.deleteShp(fileOut.getName().substring(0, fileOut.getName().length()-4) + "tmp", fileOut.getParentFile());
		return result;
	}

	private static void coord2D(Coordinate c) {
		if (!CoordinateXY.class.isInstance(c))
			c.setZ(Double.NaN);
	}
		
	
	/**
	 * Export a simple feature collection. Overwrite file if already exists
	 * 
	 * @param toExport
	 * @param fileOut
	 * @return the ShapeFile
	 * @throws IOException
	 */
	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut) throws IOException {
		if (toExport.isEmpty()) {
			System.out.println(fileOut.getName() + " is empty");
			return fileOut;
		}
		return exportSFC(toExport, fileOut, toExport.getSchema());
	}
	

	/**
	 * Export a simple feature collection. If the shapefile already exists, either overwrite it or merge it with the existing shapefile.
	 * 
	 * @param toExport
	 * @param fileOut
	 * @return the ShapeFile
	 * @throws IOException
	 */
	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft)
			throws IOException {

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		
		if (!fileOut.getName().endsWith(".shp")) {
			fileOut = new File(fileOut + ".shp");
		}
		
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileOut.toURI().toURL());
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
		return fileOut;
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile) throws IOException {
		return snapDatas(SFCIn, boxFile, 0);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile, double distance) throws IOException {
		ShapefileDataStore shpDSZone = new ShapefileDataStore(boxFile.toURI().toURL());
		Geometry bBox = Geom.unionSFC(DataUtilities.collection(shpDSZone.getFeatureSource().getFeatures()));
		shpDSZone.dispose();
		if (distance != 0)
			bBox = bBox.buffer(distance);
		return snapDatas(SFCIn, bBox);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, SimpleFeatureCollection bBox) {
		return snapDatas(SFCIn, Geom.unionSFC(bBox));
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, Geometry bBox) {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		return DataUtilities.collection(SFCIn.subCollection(ff.intersects(ff.property(SFCIn.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(bBox))));
	}
	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String code, String attribute)
			throws IOException {
		String[] attributes = { attribute };
		return getSFCPart(sFCToDivide, code, attributes);
	}

	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String code, String[] attributes) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		Arrays.stream(sFCToDivide.toArray(new SimpleFeature[0])).forEach(feat -> {
			String attribute = "";
			for (String a : attributes)
				attribute = attribute + ((String) feat.getAttribute(a));
			if (attribute.equals(code))
				result.add(feat);
		});
		return result.collection();
	}
	
	/**
	 * Sort a SimpleFeatureCollection by its feature's area (must be a collection of polygons). Uses a sorted collection and a stream method.
	 * 
	 * @param sFCToSort
	 *            SimpleFeature
	 * @return The sorted {@link SimpleFeatureCollection}
	 * @throws IOException
	 */
	public static SimpleFeatureCollection sortSFCWithArea(SimpleFeatureCollection sFCToSort) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SortedMap<Double, SimpleFeature> parcelMap = new TreeMap<>();
		Arrays.stream(sFCToSort.toArray(new SimpleFeature[0])).forEach(parcel -> {
			parcelMap.put(((Geometry) parcel.getDefaultGeometry()).getArea(), parcel);
		});
		for (Entry<Double, SimpleFeature> entry : parcelMap.entrySet())
			result.add(entry.getValue());
		return result.collection();
	}
	
	/**
	 * Check if a given {@link SimpleFeature} intersects the input {@link SimpleFeatureCollection}.
	 * 
	 * @param inputFeat
	 *            input {@link SimpleFeature}
	 * @param inputSFC
	 *            input {@link SimpleFeatureCollection}
	 * @return true if there's an intersection, false otherwise
	 */
	public static boolean isFeatIntersectsSFC(SimpleFeature inputFeat, SimpleFeatureCollection inputSFC){
		Geometry geom = (Geometry) inputFeat.getDefaultGeometry();
		// import of the cells of MUP-City outputs
		try (SimpleFeatureIterator cellsCollectionIt = Collec.snapDatas(inputSFC, geom).features()) {
			while (cellsCollectionIt.hasNext()) {
				if (((Geometry) cellsCollectionIt.next().getDefaultGeometry()).intersects(geom))
					return true;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		}
		return false;
	}

	/**
	 * Check if the given Simple Feature contains the given field name. Uses the {@link #isSchemaContainsAttribute(SimpleFeatureType, String)} method.
	 * 
	 * @param feat
	 *            input feature
	 * @param attributeFiledName
	 *            name of the field (must respect case)
	 * @return true if the feature contains the field name, false otherwise
	 */
	public static boolean isSimpleFeatureContainsAttribute(SimpleFeature feat, String attributeFiledName) {
		return isSchemaContainsAttribute(feat.getFeatureType(), attributeFiledName);
	}

	/**
	 * Check if the given collection contains the given field name.
	 * 
	 * @param collec
	 *            Input SimpleFeatureCollecton
	 * @param attributeFiledName
	 *            Name of the field (must respect case)
	 * @return true if the collec contains the field name, false otherwise
	 */
	public static boolean isCollecContainsAttribute(SimpleFeatureCollection collec, String attributeFiledName) {
		return isSchemaContainsAttribute(collec.getSchema(), attributeFiledName);
	}
	
	/**
	 * Check if the given schema contains the given field name.
	 * 
	 * @param schema
	 *            SimpleFeatureType schema
	 * @param attributeFiledName
	 *            Name of the field (must respect case)
	 * @return true if the collec contains the field name, false otherwise
	 */
	public static boolean isSchemaContainsAttribute(SimpleFeatureType schema, String attributeFiledName) {
		if (schema.getAttributeDescriptors().stream().filter(s -> s.getName().toString().equals(attributeFiledName)).collect(Collectors.toList())
				.size() == 0)
			return false;
		else
			return true;
	}

	/**
	 * Convert a collection of simple feature (which geometries are either {@link Polygon} or {@link MultiPolygon}) to a list of {@link LineString}. It takes into account the
	 * exterior and the interior lines.
	 * 
	 * @param inputSFC
	 * @return A list of {@link LineString}
	 */
	public static List<LineString> fromPolygonSFCtoListRingLines(SimpleFeatureCollection inputSFC) {
		List<LineString> lines = new ArrayList<>();
		try (SimpleFeatureIterator iterator = inputSFC.features()){
			while (iterator.hasNext()) {
				Geometry geom = (Geometry) iterator.next().getDefaultGeometry();
				if (geom instanceof MultiPolygon) {
					for (int i = 0; i < ((MultiPolygon) geom).getNumGeometries(); i++) {
						MultiLineString mls = Geom.generateLineStringFromPolygon(((Polygon) ((MultiPolygon) geom).getGeometryN(i)));
						for (int j = 0; j < mls.getNumGeometries(); j++)
							lines.add((LineString) mls.getGeometryN(j));
					}
				} else {
					MultiLineString mls = Geom.generateLineStringFromPolygon((Polygon) geom);
					for (int j = 0; j < mls.getNumGeometries(); j++)
						lines.add((LineString) mls.getGeometryN(j));
				}
			}
		} 
		return lines;
	}

	/**
	 * convert a collection of simple feature (which geometries are either {@link org.locationtech.jts.geom.Polygon} or {@link org.locationtech.jts.geom.MultiPolygon}) to a
	 * {@link MultiLineString}. It takes into account the exterior and the interior lines.
	 * 
	 * @param inputSFC
	 * @return A list of {@link LineString}
	 */
	public static MultiLineString fromPolygonSFCtoRingMultiLines(SimpleFeatureCollection inputSFC) {
		return Geom.getListAsGeom(fromPolygonSFCtoListRingLines(inputSFC), new GeometryFactory());
	}
	
	public static File exportSFC(List<SimpleFeature> listFeature, File fileOut) throws Exception {
		return exportSFC(listFeature, fileOut, true);
	}
	
	public static File exportSFC(List<SimpleFeature> listFeature, File fileOut, boolean overwrite) throws Exception {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		for (SimpleFeature feat : listFeature)
			result.add(feat);
		return exportSFC(result.collection(), fileOut, overwrite);
	}
	
	/**
	 * Get the value of a feature's field from a SimpleFeatureCollection that intersects a given Simplefeature (that is most of the time, a parcel or building). If the given
	 * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection.
	 * 
	 * @param geometry
	 *            input {@link Geometry}
	 * @param sfc
	 *            Input {@link SimpleFeatureCollection}
	 * @param fieldName
	 *            The name of the field in which to look for the attribute
	 * @return the wanted filed from the (most) intersecting {@link SimpleFeature}}
	 */
	public static String getFieldFromSFC(Geometry geometry, SimpleFeatureCollection sfc, String fieldName) {
		SimpleFeature feat = getSimpleFeatureFromSFC(geometry, sfc);
		return feat != null ? (String) feat.getAttribute(fieldName) : null ; 
	}
	
	/**
	 * Get the {@link SimpleFeature} out of a {@link SimpleFeatureCollection} that intersects a given Geometry (that is most of the time, a parcel or building). If the given
	 * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection.
	 * 
	 * @param geometry
	 *            input {@link Geometry}
	 * @param parcels
	 * @return the (most) intersecting {@link SimpleFeature}}
	 */
	public static SimpleFeature getSimpleFeatureFromSFC(Geometry geometry, SimpleFeatureCollection parcels) {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Geometry givenFeatureGeom = GeometryPrecisionReducer.reduce(geometry, new PrecisionModel(10));
		SortedMap<Double, SimpleFeature> index = new TreeMap<>();
		SimpleFeatureCollection collec = parcels
				.subCollection(ff.intersects(ff.property(parcels.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
		if (collec.isEmpty()) {
			System.out.println("intersection between " + geometry + " and " + parcels.getSchema().getName() + " null");
			return null;
		}
		try (SimpleFeatureIterator collecIt = collec.features()) {
			while (collecIt.hasNext()) {
				SimpleFeature theFeature = collecIt.next();
				Geometry theFeatureGeom = GeometryPrecisionReducer.reduce((Geometry) theFeature.getDefaultGeometry(), new PrecisionModel(10)).buffer(1);
				if (theFeatureGeom.contains(givenFeatureGeom))
					return theFeature;
				// if the parcel is in between two features, we put the feature in a sorted collection
				else if (theFeatureGeom.intersects(givenFeatureGeom))
					index.put(Geom.scaledGeometryReductionIntersection(Arrays.asList(theFeatureGeom, givenFeatureGeom)).getArea(), theFeature);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		}
		return index.size() > 0 ? index.get(index.lastKey()) : null ;
	}
	
	/**
	 * Discretize the input {@link SimpleFeatureCollection} by generating a grid and cuting features by it. Should preserve attributes (untested).
	 * 
	 * @param in
	 *            Input {@link SimpleFeatureCollection}
	 * @param gridResolution
	 *            Resolution of the grid's mesh
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
				List<Object> attr = new ArrayList<Object>();
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
