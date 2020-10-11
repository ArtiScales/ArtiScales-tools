package fr.ign.artiscales.tools.geometryGeneration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geopackages;

/**
 * Class to generate shapefiles related to community shape.
 * 
 * @author Maxime Colomb
 *
 */
public class CityGeneration {

	public static void main(String[] args) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		createUrbanIslet(new File("/home/ubuntu/PMtest/SeineEtMarne/PARCELLE03.SHP"), new File("/home/ubuntu/PMtest/SeineEtMarne/"));
	}

	/**
	 * Generate urban islet shapefile out of a parcel plan. Urban islet can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
	 * 
	 * @param parcelFile
	 *            input parcel
	 * @param outFolder
	 *            folder where goes the generated urban islet
	 * @return a collection of urban islet
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File createUrbanIsletShp(File parcelFile, File outFolder) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		File result = new File(outFolder, "islet.shp");
		if (result.exists()) {
			System.out.println("createUrbanIslet(): islet already exists");
			return result;
		}
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection islet = createUrbanIslet(parcelSDS.getFeatureSource().getFeatures());
		parcelSDS.dispose();
		return Collec.exportSFC(islet, new File(outFolder, "islet.shp"));
	}

	/**
	 * Generate urban islet shapefile out of a parcel plan. Urban islet can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
	 * 
	 * @param parcelFile
	 *            input parcel
	 * @param outFolder
	 *            folder where goes the generated urban islet
	 * @return a collection of urban islet
	 * @throws IOException
	 */
	public static File createUrbanIslet(File parcelFile, File outFolder) throws IOException {
		File result = new File(outFolder, "islet.gpkg");
		if (result.exists()) {
			System.out.println("createUrbanIslet(): islet already exists");
			return result;
		}
		DataStore parcelDS = Geopackages.getDataStore(parcelFile);
		SimpleFeatureCollection islet = parcelDS.getFeatureSource(parcelDS.getTypeNames()[0]).getFeatures();
		parcelDS.dispose();
		return Collec.exportSFC(islet, result);
	}

	/**
	 * Generate urban islet out of a parcel plan. Urban islet can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
	 * 
	 * @param parcel
	 *            input parcel
	 * @return a {@link SimpleFeatureCollection} of urban islet
	 * @throws IOException
	 */
	public static SimpleFeatureCollection createUrbanIslet(SimpleFeatureCollection parcel) throws IOException {
		Geometry bigGeom = Geom.unionSFC(parcel).buffer(1).buffer(-1);
		DefaultFeatureCollection df = new DefaultFeatureCollection();
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaID("islet");
		int count = 0;
		for (int i = 0; i < bigGeom.getNumGeometries(); i++) {
			sfBuilder.add(bigGeom.getGeometryN(i));
			Object[] obj = { i };
			df.add(sfBuilder.buildFeature(String.valueOf(count++), obj));
		}
		return new SpatialIndexFeatureCollection(df.collection());
	}

	/**
	 * Get the border of a studied zone. Buffers have fixed values and could be parametrized.
	 * 
	 * @param in
	 *            input {@link SimpleFeatureCollection}
	 * @return the border without the inside geometry
	 */
	public static Geometry createBufferBorder(SimpleFeatureCollection in) {
		Geometry hull = Geom.unionSFC(in).buffer(20).buffer(-20);
		List<Geometry> list = Arrays.asList(hull, hull.buffer(50));
		return Geom.unionGeom(FeaturePolygonizer.getPolygons(list).stream().filter(x -> !hull.buffer(1).contains(x)).collect(Collectors.toList()));
	}
}
