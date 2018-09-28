package fr.ign.cogit.GTFunctions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.precision.SimpleGeometryPrecisionReducer;

public class Vectors {
	
	public static void main(String[] args) throws Exception {
		
		ShapefileDataStore shpDSCells = new ShapefileDataStore((new File("/media/mcolomb/Data_2/resultFinal/stab/extra/intersecNU-ZC/NUManuPhyDecoup.shp")).toURI().toURL());
		SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();
		
		Geometry cellsUnion = unionSFC(cellsCollection);

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
		
		sfBuilder.add(cellsUnion);
		SimpleFeature feature = sfBuilder.buildFeature("0");
		toSplit.add(feature);

		shpDSCells.dispose();
		exportSFC(toSplit.collection(),new File ("/home/mcolomb/tmp/mergeSmth.shp"));
	}
	
	public static File mergeVectFiles(List<File> file2MergeIn, File f) throws Exception {
		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
		for (File file : file2MergeIn) {
			ShapefileDataStore SDSParcel = new ShapefileDataStore(file.toURI().toURL());
			SimpleFeatureIterator parcelIt = SDSParcel.getFeatureSource().getFeatures().features();
			try {
				while (parcelIt.hasNext()) {
					newParcel.add(parcelIt.next());
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				parcelIt.close();
			}
			SDSParcel.dispose();
		}

		if (!newParcel.isEmpty()) {
			Vectors.exportSFC(newParcel.collection(), f);
		}
		return f;
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
		
		
		return exportSFC(DFC.collection(),fileName);
	}
	
	public static File exportSFC(SimpleFeatureCollection toExport, File fileName) throws IOException {
		if (toExport.isEmpty()){
			System.out.println(fileName.getName()+" is empty");
			return fileName;
		}
		return exportSFC(toExport, fileName, toExport.getSchema());
	}
	
	public static File exportSFC(SimpleFeatureCollection toExport, File fileName, SimpleFeatureType ft) throws IOException {

		ShapefileDataStoreFactory  dataStoreFactory = new ShapefileDataStoreFactory();

		if (!fileName.getName().endsWith(".shp")) {
			fileName = new File(fileName+".shp"); 
		}
		
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileName.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(ft);

		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
		System.out.println("SHAPE:" + SHAPE_TYPE);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
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

	public static Geometry unionSFC(SimpleFeatureCollection collection) throws IOException {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0])).map(sf -> GeometryPrecisionReducer.reduce((Geometry) sf.getDefaultGeometry(),	new PrecisionModel(1000)));
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}
	
	public static Geometry unionGeom(List<Geometry> lG) throws IOException {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = lG.stream();
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}
	
	public static SimpleFeatureCollection cropSFC(SimpleFeatureCollection inSFC, File empriseFile) throws MalformedURLException, IOException {
		if (inSFC.isEmpty()) {
			return inSFC;
		}
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		ShapefileDataStore envSDS = new ShapefileDataStore(empriseFile.toURI().toURL());
		ReferencedEnvelope env = (envSDS.getFeatureSource().getFeatures()).getBounds();
		String geometryPropertyName = inSFC.getSchema().getGeometryDescriptor().getLocalName();
		Filter filter = ff.bbox(ff.property(geometryPropertyName), env);
		envSDS.dispose();
		return inSFC.subCollection(filter);
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
	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, File boxFile) throws Exception {

		ShapefileDataStore shpDSZone = new ShapefileDataStore(boxFile.toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();
		Geometry bBox = unionSFC(zoneCollection);
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
}
