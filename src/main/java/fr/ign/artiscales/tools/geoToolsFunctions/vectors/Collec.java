package fr.ign.artiscales.tools.geoToolsFunctions.vectors;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom.Lines;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.FilterVisitor;
import org.opengis.referencing.FactoryException;
import si.uom.SI;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class Collec {

	private static String defaultGISFileType = ".gpkg";

	// public static void main(String[] args) throws Exception {
	// String[] vals = { "DEPCOM", "LIBELLE" };
	// DataStore ds = Geopackages.getDataStore(new File("/home/thema/Documents/MC/workspace/ParcelManager/src/main/resources/ParcelComparison/zoning.gpkg"));
	// SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
	// System.out.println(getEachUniqueFieldFromSFC(sfc, vals));
	//
	// File shp1F = new File("/tmp/shp1.shp");
	// File shp2F = new File("/tmp/shp2.shp");
	// File gp1F = new File("/tmp/gp1.gpkg");
	// File gp2F = new File("/tmp/gp2.gpkg");
	// File gp3F = new File("/tmp/gp3.gpkg");
	//// TODO tester ça
	// List<File> shpL = Arrays.asList(shp1F, shp2F);
	// List<File> gp2L = Arrays.asList(gp1F, gp2F);
	// DataStore ds = Geopackages.getDataStore(gp2F);
	// SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
	//// Shp.mergeVectFiles(shpL, new File("/tmp/sh.shp"));
	// Collec.exportSFC(sfc, new File("/tmp/g.gpkg"));
	// Collec.exportSFC(sfc, gp2F);
	// Collec.exportSFC(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), gp3F, false);
	// ds.dispose();
	// ShapefileDataStore shpDSParcel = new ShapefileDataStore((new File(
	// "/home/thema/Documents/MC/workspace/ParcelManager/src/main/resources/ParcelComparisonOM/out/parcelsInZone.shp"))
	// .toURI().toURL());
	// SimpleFeatureCollection parcel = shpDSParcel.getFeatureSource().getFeatures();
	// File tmp = new File("/tmp/shp.shp");
	// Collec.exportSFC(parcel, tmp);
	// Collec.exportSFC(parcel, new File("/tmp/ex.gpkg"));
	// shpDSParcel.dispose();
	//
	// DataStore ds = Geopackages.getDataStore(new File("/tmp/djadja.gpkg"));
	// SimpleFeatureCollection parcel2 = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
	// Collec.exportSFC(parcel2, new File("/tmp/shp2.gpkg"));
	// ds.dispose();
	// }

	/**
	 * Get statistics about a field of a collection
	 * 
	 * @param sfc input {@link SimpleFeatureCollection}
	 * * @param attribute
	 * @return
	 */
	public static double getCollectionAttributeDescriptiveStat(SimpleFeatureCollection sfc, String attribute, StatisticOperation stat) {
		try {
			DescriptiveStatistics ds = new DescriptiveStatistics();
			try (SimpleFeatureIterator polyIt = sfc.features()) {
				while (polyIt.hasNext())
					ds.addValue(Double.parseDouble(String.valueOf(polyIt.next().getAttribute(attribute))));
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
	 * Return the sum of area of every features of a simpleFeatureCollection
	 * 
	 * @param parcels
	 *            input {@link SimpleFeatureCollection}
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

	private static void coord2D(Coordinate c) {
		if (!(c instanceof CoordinateXY))
			c.setZ(Double.NaN);
	}

	public static File exportSFC(List<SimpleFeature> listFeature, File fileOut) throws IOException {
		return exportSFC(listFeature, fileOut, true);
	}

	public static File exportSFC(List<SimpleFeature> listFeature, File fileOut, boolean overwrite) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(listFeature);
		return exportSFC(result.collection(), fileOut, overwrite);
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut) throws IOException {
		return exportSFC(toExport, fileOut, true);
	}

	/**
	 * get the corresponding Data Store looking the file's attribute
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static DataStore getDataStore(File f) throws IOException {
		switch (f.getName().split("\\.")[f.getName().split("\\.").length - 1].toLowerCase()) {
		case "gpkg":
			return Geopackages.getDataStore(f);
		case "shp":
			return new ShapefileDataStore(f.toURI().toURL());
		case "geojson":
		case "json":
			return GeoJSON.getGeoJSONDataStore(f);
		}
		return null;
	}

	/**
	 * Export a simple feature collection. Overwrite file if already exists
	 * 
	 * @param toExport
	 * @param fileOut
	 * @return the ShapeFile
	 * @throws IOException
	 */
	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, boolean overwrite) throws IOException {
		if (toExport.isEmpty()) {
			System.out.println(fileOut.getName() + " is empty");
			return fileOut;
		}
		String n = fileOut.getName();
		String[] ext = n.split("\\.");
		return exportSFC(toExport, fileOut, ext.length > 1 ? "." + ext[1] : defaultGISFileType, overwrite);
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, String outputType, boolean overwrite) throws IOException {
		return exportSFC(toExport, fileOut, toExport.getSchema(), outputType, overwrite);
	}

	/**
	 * Export a simple feature collection. If the shapefile already exists, either overwrite it or merge it with the existing shapefile.
	 * 
	 * @param toExport
	 * @param fileOut
	 * @return the ShapeFile
	 * @throws IOException
	 */
	public static File exportSFC(SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft, String outputType, boolean overwrite)
			throws IOException {
		if (outputType.equals(".shp"))
			return Shp.exportSFCtoSHP(toExport, fileOut, ft, overwrite);
		else if (outputType.equals(".gpkg"))
			return Geopackages.exportSFCtoGPKG(toExport, fileOut, ft, overwrite);
		else if (defaultGISFileType != null && !defaultGISFileType.equals(""))
			return exportSFC(toExport, fileOut, ft, defaultGISFileType, overwrite);
		else
			return null;
	}

	static File makeTransaction(DataStore newDataStore, SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft) throws IOException {
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
						g.apply(Collec::coord2D);
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
				// toExport.accepts((Feature f) -> System.out.println(((SimpleFeature)f).getDefaultGeometry()), null);
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

	public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, File boxFile) throws IOException {
		return selectIntersection(SFCIn, boxFile, 0);
	}

	public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, File boxFile, double distance) throws IOException {
		ShapefileDataStore shpDSZone = new ShapefileDataStore(boxFile.toURI().toURL());
		Geometry bBox = Geom.unionSFC(DataUtilities.collection(shpDSZone.getFeatureSource().getFeatures()));
		SimpleFeatureCollection result = selectIntersection(SFCIn, bBox, distance);
		shpDSZone.dispose();
		return result;
	}

	public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection collection, Geometry geom, double distance) {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		return collection.subCollection(ff.dwithin(ff.property(collection.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geom),
				distance, SI.METRE.toString()));
	}

	public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, SimpleFeatureCollection bBox) {
		return selectIntersection(SFCIn, Geom.unionSFC(bBox));
	}

	public static SimpleFeatureCollection selectIntersection(SimpleFeatureCollection SFCIn, Geometry bBox) {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		return SFCIn.subCollection(ff.intersects(ff.property(SFCIn.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(bBox)));
	}

	/**
	 * Create a {@link SimpleFeatureCollection} with features which designed attribute field matches a precise attribute value.
	 * 
	 * @param sFCToDivide
	 *            SimpleFeatureCollection to sort
	 * @param fieldName
	 *            field name to select features from
	 * @param attribute
	 *            wanted field
	 * @return a collection with matching features
	 * @throws IOException
	 */
	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String fieldName, String attribute) throws IOException {
		String[] attributes = { attribute };
		String[] fieldNames = { fieldName };
		return getSFCPart(sFCToDivide, fieldNames, attributes);
	}

	/**
	 * Create a {@link SimpleFeatureCollection} with features which a list of attribute field matches a list of strings. The index of the couple fieldName/attribute must match.
	 * 
	 * @param sFCToDivide
	 *            SimpleFeatureCollection to sort
	 * @param fieldNames
	 *            array of field names
	 * @param attributes
	 *            array of values
	 * @return a collection with matching features
	 * @throws IOException
	 */
	public static SimpleFeatureCollection getSFCPart(SimpleFeatureCollection sFCToDivide, String[] fieldNames, String[] attributes)
			throws IOException {
		int shortestIndice = Math.min(fieldNames.length, attributes.length);
		if (fieldNames.length != attributes.length)
			System.out.println("not same number of indices between fieldNames and attributes. Took the shortest one");
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		Arrays.stream(sFCToDivide.toArray(new SimpleFeature[0])).forEach(feat -> {
			boolean add = true;
			for (int i = 0; i < shortestIndice; i++)
				if (!feat.getAttribute(fieldNames[i]).equals(attributes[i])) {
					add = false;
					break;
				}
			if (add)
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
	 * @throws IOException from {@link DefaultFeatureCollection}
	 */
	public static SimpleFeatureCollection sortSFCWithArea(SimpleFeatureCollection sFCToSort) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SortedMap<Double, SimpleFeature> parcelMap = new TreeMap<>();
		Arrays.stream(sFCToSort.toArray(new SimpleFeature[0])).forEach(parcel -> parcelMap.put(((Geometry) parcel.getDefaultGeometry()).getArea(), parcel));
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
	public static boolean isFeatIntersectsSFC(SimpleFeature inputFeat, SimpleFeatureCollection inputSFC) {
		Geometry geom = (Geometry) inputFeat.getDefaultGeometry();
		// import of the cells of MUP-City outputs
		try (SimpleFeatureIterator cellsCollectionIt = Collec.selectIntersection(inputSFC, geom).features()) {
			while (cellsCollectionIt.hasNext())
				if (((Geometry) cellsCollectionIt.next().getDefaultGeometry()).intersects(geom))
					return true;
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
		return schema.getAttributeDescriptors().stream().anyMatch(s -> s.getName().toString().equals(attributeFiledName));
	}

	/**
	 * Convert a collection of simple feature (which geometries are either {@link Polygon} or {@link MultiPolygon}) to a list of {@link LineString}. It takes into account the
	 * exterior and the interior lines.
	 * 
	 * @param inputSFC input {@link SimpleFeatureCollection}
	 * @return A list of {@link LineString}
	 */
	public static List<LineString> fromPolygonSFCtoListRingLines(SimpleFeatureCollection inputSFC) {
		List<LineString> lines = new ArrayList<>();
		try (SimpleFeatureIterator iterator = inputSFC.features()) {
			while (iterator.hasNext()) {
				Geometry geom = (Geometry) iterator.next().getDefaultGeometry();
				if (geom instanceof MultiPolygon) {
					for (int i = 0; i < geom.getNumGeometries(); i++) {
						MultiLineString mls = Lines.getMultiLineString(geom.getGeometryN(i));
						for (int j = 0; j < mls.getNumGeometries(); j++)
							lines.add((LineString) mls.getGeometryN(j));
					}
				} else {
					MultiLineString mls = Lines.getMultiLineString(geom);
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
	 * @param inputSFC input {@link SimpleFeatureCollection}
	 * @return A list of {@link LineString}
	 */
	public static MultiLineString fromPolygonSFCtoRingMultiLines(SimpleFeatureCollection inputSFC) {
		return Lines.getListLineStringAsMultiLS(fromPolygonSFCtoListRingLines(inputSFC), new GeometryFactory());
	}

	/**
	 * 
	 * @param sfcToSort
	 * @param sfcIntersection
	 * @return
	 */
	public static SimpleFeatureCollection getSFCfromSFCIntersection(SimpleFeatureCollection sfcToSort, SimpleFeatureCollection sfcIntersection) {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Geometry geometry = Geom.unionSFC(sfcIntersection);
		SimpleFeatureCollection collec = sfcToSort
				.subCollection(ff.intersects(ff.property(sfcToSort.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
		if (collec.isEmpty())
			return null;
		Arrays.stream(collec.toArray(new SimpleFeature[0])).forEach(theFeature -> {
			Geometry theFeatureGeom = (Geometry) theFeature.getDefaultGeometry();
			if (geometry.contains(theFeatureGeom)
					|| (theFeatureGeom.intersects(geometry) && geometry.intersection(theFeatureGeom).getArea() > theFeatureGeom.getArea() * 0.5))
				result.add(theFeature);
		});
		return result;
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
	public static String getIntersectingFieldFromSFC(Geometry geometry, SimpleFeatureCollection sfc, String fieldName) {
		SimpleFeature feat = getIntersectingSimpleFeatureFromSFC(geometry, sfc);
		return feat != null ? (String) feat.getAttribute(fieldName) : null;
	}

	/**
	 * Get the {@link SimpleFeature} out of a {@link SimpleFeatureCollection} that intersects a given Geometry (that is most of the time, a parcel or building). If the given
	 * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection.
	 * 
	 * @param geometry
	 *            input {@link Geometry}
	 * @param inputSFC
	 * @return the (most) intersecting {@link SimpleFeature}}
	 */
	public static SimpleFeature getIntersectingSimpleFeatureFromSFC(Geometry geometry, SimpleFeatureCollection inputSFC) {
		try {
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
			SortedMap<Double, SimpleFeature> index = new TreeMap<>();
			SimpleFeatureCollection collec = inputSFC
					.subCollection(ff.intersects(ff.property(inputSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
			if (collec.isEmpty()) {
				// logger.debug("intersection between " + geometry + " and " + parcels.getSchema().getName() + " null");
				return null;
			}
			try (SimpleFeatureIterator collecIt = collec.features()) {
				while (collecIt.hasNext()) {
					SimpleFeature theFeature = collecIt.next();
					Geometry theFeatureGeom = ((Geometry) theFeature.getDefaultGeometry()).buffer(1);
					if (theFeatureGeom.contains(geometry))
						return theFeature;
					// if the parcel is in between two features, we put the feature in a sorted collection
					else if (theFeatureGeom.intersects(geometry))
						index.put(Geom.scaledGeometryReductionIntersection(Arrays.asList(theFeatureGeom, geometry)).getArea(), theFeature);
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			}
			return index.size() > 0 ? index.get(index.lastKey()) : null;
		} catch (Exception e) {
			return getIntersectingSimpleFeatureFromSFC(geometry, inputSFC, new PrecisionModel(10));
		}
	}

	/**
	 * Get the {@link SimpleFeature} out of a {@link SimpleFeatureCollection} that intersects a given Geometry (that is most of the time, a parcel or building). If the given
	 * feature is overlapping multiple SimpleFeatureCollection's features, we calculate which has the more area of intersection. Reduce the precision of the {@link Geometry}s
	 * 
	 * @param geometry
	 *            input {@link Geometry}
	 * @param inputSFC
	 * @param precisionModel
	 * @return the (most) intersecting {@link SimpleFeature}}
	 */
	public static SimpleFeature getIntersectingSimpleFeatureFromSFC(Geometry geometry, SimpleFeatureCollection inputSFC,
			PrecisionModel precisionModel) {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Geometry givenFeatureGeom = GeometryPrecisionReducer.reduce(geometry, precisionModel);
		SortedMap<Double, SimpleFeature> index = new TreeMap<>();
		SimpleFeatureCollection collec = inputSFC
				.subCollection(ff.intersects(ff.property(inputSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(geometry)));
		if (collec.isEmpty()) {
			// logger.debug("intersection between " + geometry + " and " + parcels.getSchema().getName() + " null");
			return null;
		}
		try (SimpleFeatureIterator collecIt = collec.features()) {
			while (collecIt.hasNext()) {
				SimpleFeature theFeature = collecIt.next();
				Geometry theFeatureGeom = GeometryPrecisionReducer.reduce((Geometry) theFeature.getDefaultGeometry(), precisionModel).buffer(1);
				if (theFeatureGeom.contains(givenFeatureGeom))
					return theFeature;
				// if the parcel is in between two features, we put the feature in a sorted
				// collection
				else if (theFeatureGeom.intersects(givenFeatureGeom))
					index.put(Objects.requireNonNull(Geom.scaledGeometryReductionIntersection(Arrays.asList(theFeatureGeom, givenFeatureGeom))).getArea(), theFeature);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		}
		return index.size() > 0 ? index.get(index.lastKey()) : null;
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
				List<Object> attr = new ArrayList<>();
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

	public static SimpleFeatureCollection transformGeomToMultiPolygon(SimpleFeatureCollection parcel) {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		try (SimpleFeatureIterator it = parcel.features()) {
			while (it.hasNext()) {
				result.add(Schemas.setSFBSchemaWithMultiPolygon(it.next()).buildFeature(Attribute.makeUniqueId()));
			}
		} catch (Error r) {
			r.printStackTrace();
		}
		return result;
	}

	public static String getDefaultGISFileType() {
		return defaultGISFileType;
	}

	public static void setDefaultGISFileType(String defaultGISFileType) {
		Collec.defaultGISFileType = defaultGISFileType;
	}

	public static String getDefaultGeomName() {
		if (defaultGISFileType.equals(".shp") || defaultGISFileType.equals(".geojson"))
			return "the_geom";
		else if (defaultGISFileType.equals(".gpkg"))
			return "geom";
		else
			return "";
	}

	public static SimpleFeatureCollection mergeSFC(List<SimpleFeatureCollection> sfcs, boolean keepAttributes, File boundFile) throws IOException {
		return mergeSFC(sfcs, sfcs.get(0).getSchema(), keepAttributes, boundFile);
	}

	public static SimpleFeatureCollection mergeSFC(List<SimpleFeatureCollection> sfcs, SimpleFeatureType schemaRef, boolean keepAttributes,
			File boundFile) throws IOException {
		// sfBuilder used only if number of attributes's the same but with different schemas
		SimpleFeatureBuilder defaultSFBuilder = new SimpleFeatureBuilder(schemaRef);
		DefaultFeatureCollection newParcelCollection = new DefaultFeatureCollection();

		lookOutAttribute: if (keepAttributes) {
			// check if the schemas of the shape are the same and if not, if they have the same number of attributes
			int nbAttr = schemaRef.getAttributeCount();
			for (SimpleFeatureCollection sfc : sfcs) {
				SimpleFeatureType schemaComp = sfc.getSchema();
				if (schemaComp.equals(schemaRef))
					continue;
				// System.out.println(f + " have not the same schema as " + fRef + ". Try to still add attribute if number is the same but output may be fuzzy"); TODO put that in a
				// logger
				if (nbAttr != schemaComp.getAttributeCount()) {
					System.out.println("Not the same amount of attributes in the shapefile : Output won't have any attributes");
					keepAttributes = false;
					break lookOutAttribute;
				}
			}
		}
		for (SimpleFeatureCollection sfc : sfcs) {
			if (keepAttributes) {
				// Merge the feature and assignate a new id number. If collections doesn't have the exactly same schema but the same number of attributes,
				// we add every attribute regarding their position
				Arrays.stream(sfc.toArray(new SimpleFeature[0])).forEach(feat -> {
					Object[] attr = new Object[feat.getAttributeCount() - 1];
					for (int h = 1; h < feat.getAttributeCount(); h++)
						attr[h - 1] = feat.getAttribute(h);
					defaultSFBuilder.add(feat.getDefaultGeometry());
					newParcelCollection.add(defaultSFBuilder.buildFeature(Attribute.makeUniqueId(), attr));
				});
			} else {
				// if we don't want to keep attributes, we create features out of new features
				// containing only geometry
				Arrays.stream(sfc.toArray(new SimpleFeature[0])).forEach(feat -> {
					defaultSFBuilder.set("the_geom", feat.getDefaultGeometry());
					newParcelCollection.add(defaultSFBuilder.buildFeature(Attribute.makeUniqueId()));
				});
			}
		}
		SimpleFeatureCollection output = newParcelCollection.collection();
		if (boundFile != null && boundFile.exists())
			output = Collec.selectIntersection(output, boundFile);
		return output;
	}

	public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String[] attributes) {
		return getEachUniqueFieldFromSFC(sfcIn, attributes, false);
	}

	/**
	 * Get the unique values of a SimpleFeatureCollection from a combination of fields. Each fields are separated with a "-" character.
	 * 
	 * @param sfcIn
	 *            input {@link SimpleFeatureCollection}
	 * @param attributes
	 *            field name to create the combination of unique values
	 * @return the list of unique values
	 */
	public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String[] attributes, boolean dontCheckAttribute) {
		if (!dontCheckAttribute)
			for (String attribute : attributes)
				if (!Collec.isCollecContainsAttribute(sfcIn, attribute)) {
					System.out.println("getEachUniqueFieldFromSFC:  no " + attribute + " found");
					return null;
				}
		List<String> result = new ArrayList<>();
		Arrays.stream(sfcIn.toArray(new SimpleFeature[0])).forEach(sf -> {
			StringBuilder val = new StringBuilder();
			for (String attribute : attributes)
				try {
					val.append("-").append(((String) sf.getAttribute(attribute)).replace(",", "-"));
				} catch (Exception ignored) {
				}
			if (val.toString().startsWith("-"))
				val = new StringBuilder(val.substring(1, val.length()));
			if (!result.contains(val.toString()))
				result.add(val.toString());
		});
		return result;
	}

	/**
	 * Get the unique values of a SimpleFeatureCollection from a single field.
	 * 
	 * @param sfcIn
	 *            input {@link SimpleFeatureCollection}
	 * @param attribute
	 *            field name to create the unique list
	 * @return the list of unique values
	 */
	public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String attribute) {
		String[] attributes = { attribute };
		return getEachUniqueFieldFromSFC(sfcIn, attributes);
	}

	/**
	 * Get the unique values of a SimpleFeatureCollection from a single field.
	 * 
	 * @param sfcIn
	 *            input {@link SimpleFeatureCollection}
	 * @param attribute
	 *            field name to create the unique list
	 * @return the list of unique values
	 */
	public static List<String> getEachUniqueFieldFromSFC(SimpleFeatureCollection sfcIn, String attribute, boolean dontCheckAttribute) {
		String[] attributes = { attribute };
		return getEachUniqueFieldFromSFC(sfcIn, attributes, dontCheckAttribute);
	}

	/**
	 * Return a SimpleFeature with the merged geometries and the schema of the input collection but no attribute
	 * 
	 * @param collec
	 *            input {@link SimpleFeatureCollection}
	 * @return a {@link SimpleFeature} with no values
	 */
	public static SimpleFeature unionSFC(SimpleFeatureCollection collec) {
		SimpleFeatureBuilder builder = Schemas.getSFBSchemaWithMultiPolygon(collec.getSchema());
		builder.set(collec.getSchema().getGeometryDescriptor().getLocalName(), Geom.unionSFC(collec));
		return builder.buildFeature(Attribute.makeUniqueId());
	}

	/**
	 * Return a SimpleFeature with the merged geometries and the schema of the input collection but no attribute
	 * 
	 * @param collec
	 *            input {@link SimpleFeatureCollection}
	 * @return a {@link SimpleFeature} with no values
	 */
	public static SimpleFeature unionSFC(SimpleFeatureCollection collec, int precision) {
		SimpleFeatureBuilder builder = Schemas.getSFBSchemaWithMultiPolygon(collec.getSchema());
		builder.set(collec.getSchema().getGeometryDescriptor().getLocalName(), Geom.unionPrecisionReduce(collec, precision));
		return builder.buildFeature(Attribute.makeUniqueId());
	}

	/**
	 * Return a SimpleFeature with the schema of the collection with an attribute on the corresponding field.
	 * 
	 * @param collec
	 *            input {@link SimpleFeatureCollection}
	 * @param field
	 *            Field name to copy attribute in
	 * @param attribute
	 *            {@link String} to copy in the feature
	 * @return The {@link SimpleFeature} with only field and its value
	 */
	public static SimpleFeature unionSFC(SimpleFeatureCollection collec, String field, String attribute) {
		String geomName = collec.getSchema().getGeometryDescriptor().getLocalName();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName("union");
		sfTypeBuilder.add(geomName, MultiPolygon.class);
		sfTypeBuilder.add(field, String.class);
		sfTypeBuilder.setDefaultGeometry(geomName);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		builder.set(geomName, unionSFC(collec));
		builder.set(field, attribute);
		return builder.buildFeature(Attribute.makeUniqueId());
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
