package fr.ign.artiscales.tools;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.LinearComponentExtracter;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FeaturePolygonizer {
	private static final GeometryFactory fact = new GeometryFactory();
	public static Boolean DEBUG = false;

	private static List<Geometry> getLines(List<Geometry> inputFeatures) {
		List<Geometry> linesList = new ArrayList<>();
		LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
		for (Geometry feature : inputFeatures)
			feature.apply(lineFilter);
		return linesList;
	}

	private static Point extractPoint(List<Geometry> lines) {
		Point point = null;
		// extract first point from first non-empty geometry
		for (Geometry geometry : lines) {
			if (!geometry.isEmpty()) {
				Coordinate p = geometry.getCoordinate();
				point = geometry.getFactory().createPoint(p);
				break;
			}
		}
		return point;
	}

	private static Geometry nodeLines(List<Geometry> lines) {
		MultiLineString linesGeom = fact.createMultiLineString(lines.toArray(new LineString[lines.size()]));
		Geometry unionInput = fact.createMultiLineString(null);
		Point point = extractPoint(lines);
		if (point != null)
			unionInput = point;
		return linesGeom.union(unionInput);
	}

	private static List<Geometry> getFeatures(File aFile, Function<SimpleFeature, Boolean> filter) throws IOException {
		ShapefileDataStore store = new ShapefileDataStore(aFile.toURI().toURL());
		ArrayList<Geometry> array = new ArrayList<>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
		while (reader.hasNext()) {
			SimpleFeature feature = reader.next();
			if (filter.apply(feature))
				array.add((Geometry) feature.getDefaultGeometry());
		}
		reader.close();
		store.dispose();
		return array;
	}

	private static void addFeatures(Polygonizer p, List<Geometry> inputFeatures) {
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " node lines");
		List<Geometry> reduced = inputFeatures.stream().map(g->GeometryPrecisionReducer.reduce(g, new PrecisionModel(100))).collect(Collectors.toList());
		// extract linear components from input geometries
		List<Geometry> lines = getLines(reduced);
		// node all geometries together
		Geometry nodedLines = nodeLines(lines);
		if (nodedLines instanceof MultiLineString) {
			// noding a second time to be sure
			MultiLineString mls = (MultiLineString) nodedLines;
			List<Geometry> geoms = new ArrayList<>(mls.getNumGeometries());
			for (int i = 0; i < mls.getNumGeometries(); i++)
				geoms.add(mls.getGeometryN(i));
			nodedLines = nodeLines(geoms);
		}
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " insert lines");
		p.add(nodedLines);
	}

	@SuppressWarnings("unchecked")
	public static List<Polygon> getPolygons(List<Geometry> features) {
		Polygonizer polygonizer = new Polygonizer();
		addFeatures(polygonizer, features);
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " now with the real stuff");
		List<Polygon> result = new ArrayList<>(polygonizer.getPolygons());
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " all done now");
		// for (Polygon p : result)
		// System.out.println(p);
		// System.out.println(Calendar.getInstance().getTime() + " all done now");
		return result;
	}

	public static List<Polygon> getPolygons(File[] files) throws IOException {
		List<Geometry> features = new ArrayList<>();
		for (File file : files) {
			if (DEBUG)
				System.out.println(Calendar.getInstance().getTime() + " handling " + file);
			features.addAll(getFeatures(file, f -> true));
		}
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " adding features");
		return getPolygons(features);
	}

	public static List<Polygon> getPolygons(SimpleFeatureCollection sFC) {
		List<Geometry> features = new ArrayList<>();
		try (SimpleFeatureIterator sFCit = sFC.features()) {
			while (sFCit.hasNext()) {
				features.add((Geometry) sFCit.next().getDefaultGeometry());
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		}
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " adding features");
		return getPolygons(features);
	}

	public static void saveGeometries(List<? extends Geometry> geoms, File file, String geomType)
			throws IOException, SchemaException {
		String specs = "geom:" + geomType + ":srid=2154";
		ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
		FileDataStore dataStore = factory.createDataStore(file.toURI().toURL());
		String featureTypeName = "Object";
		SimpleFeatureType featureType = DataUtilities.createType(featureTypeName, specs);
		dataStore.createSchema(featureType);
		String typeName = dataStore.getTypeNames()[0];
		FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT);
		System.setProperty("org.geotools.referencing.forceXY", "true");
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " write shapefile");
		for (Geometry g : geoms) {
			SimpleFeature feature = writer.next();
			feature.setAttributes(new Object[] { g });
			writer.write();
		}
		if (DEBUG)
			System.out.println(Calendar.getInstance().getTime() + " done");
		writer.close();
		dataStore.dispose();
	}
	
  public static Geometry getIntersection(List<Geometry> features)  {
    List<Polygon> polygons = getPolygons(features);
    List<Polygon> buffer = new ArrayList<>();
    for (Polygon p : polygons) {
      Point point = p.getInteriorPoint();
      if (features.stream().allMatch(g->g.intersects(point))) {
        buffer.add(p);
      }
    }
    return fact.createGeometryCollection(buffer.toArray(new Geometry[buffer.size()])).union();
  }
  @SuppressWarnings("unchecked")
  public static Geometry getDifference(List<? extends Geometry> features, List<? extends Geometry> featuresToRemove) {
    Polygonizer polygonizer = new Polygonizer();
    List<Geometry> allFeatures = new ArrayList<>(features);
    allFeatures.addAll(featuresToRemove);
    addFeatures(polygonizer, allFeatures);
//    addFeatures(polygonizer, features);
//    addFeatures(polygonizer, featuresToRemove);
    List<Polygon> polygons = new ArrayList<>();
    polygons.addAll(polygonizer.getPolygons());
    List<Polygon> buffer = new ArrayList<>();
    for (Polygon p : polygons) {
      Point point = p.getInteriorPoint();
      if (features.stream().anyMatch(g->g.intersects(point)) && featuresToRemove.stream().noneMatch(g-> g.intersects(point))) {
        buffer.add(p);
      }
    }
    return fact.createGeometryCollection(buffer.toArray(new Geometry[buffer.size()])).union();
  }
  @SuppressWarnings("unchecked")
  public static Pair<Geometry,Geometry> getIntersectionDifference(List<Geometry> features, List<Geometry> featuresToRemove) {
    Polygonizer polygonizer = new Polygonizer();
    List<Geometry> allFeatures = new ArrayList<>(features);
    allFeatures.addAll(featuresToRemove);
    addFeatures(polygonizer, allFeatures);
//    addFeatures(polygonizer, features);
//    addFeatures(polygonizer, featuresToRemove);
    List<Polygon> polygons = new ArrayList<>();
    polygons.addAll(polygonizer.getPolygons());
    List<Polygon> intersectionBuffer = new ArrayList<>();
    List<Polygon> differenceBuffer = new ArrayList<>();
    for (Polygon p : polygons) {
      Point point = p.getInteriorPoint();
      if (features.stream().anyMatch(g->g.intersects(point))) {
        if (featuresToRemove.stream().anyMatch(g->g.intersects(point))) {
          intersectionBuffer.add(p);
        } else { //if (featuresToRemove.stream().noneMatch(g->g.intersects(point))) {
          differenceBuffer.add(p);
        }
      }
    }
    Geometry[] reducedIntersection = intersectionBuffer.stream().map(g->GeometryPrecisionReducer.reduce(g, new PrecisionModel(100))).toArray(Geometry[]::new);
    Geometry[] reducedDifference = differenceBuffer.stream().map(g->GeometryPrecisionReducer.reduce(g, new PrecisionModel(100))).toArray(Geometry[]::new);
    Geometry intersection = fact.createGeometryCollection(reducedIntersection).union();
    Geometry difference = fact.createGeometryCollection(reducedDifference).union();
    return new ImmutablePair<>(intersection, difference);
  }

	public static void main(String[] args) throws IOException, SchemaException {
		// input folder for shapefiles
		// File folderData = new File("./data/pau");
		// take all shapefiles in the folder
		// File[] files = folderData.listFiles((dir, name) -> name.endsWith(".shp"));
		File[] files = { new File("./ArtiScales20190204/tmp/tmpParcel.shp"), new File("./ArtiScales20190204/dataRegulation/zoning.shp") };
		// output folder for shapefiles
		File folder = new File("./out");
		folder.mkdirs();
		File out = new File(folder, "polygon.shp");
		// build polygons
		List<Polygon> polygons = getPolygons(files);
		saveGeometries(polygons, out, "Polygon");
	}
}
