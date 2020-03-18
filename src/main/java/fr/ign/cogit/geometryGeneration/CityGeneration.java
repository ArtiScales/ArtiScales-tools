package fr.ign.cogit.geometryGeneration;

import java.io.File;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.geoToolsFunctions.Schemas;
import fr.ign.cogit.geoToolsFunctions.vectors.Collec;
import fr.ign.cogit.geoToolsFunctions.vectors.Geom;
/**
 * Class to generate shapefiles related to community shape. 
 * 
 * @author mcolomb
 *
 */
public class CityGeneration {
	
//	public static void main(String[] args) throws NoSuchAuthorityCodeException, IOException, FactoryException {
//		createUrbanIslet(new File("/home/ubuntu/PMtest/SeineEtMarne/PARCELLE03.SHP"),  new File ("/home/ubuntu/PMtest/SeineEtMarne/"));
//	}
	
	/**
	 * Generate urban islet out of a parcel plan. Urban islet can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
	 * 
	 * @param parcelFile : input parcel
	 * @param outFolder : folder where goes the generated urban islet
	 * @return a collection of urban islet
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File createUrbanIslet(File parcelFile, File outFolder) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		File result = new File(outFolder, "islet.shp");
		if (result.exists()) {
			System.out.println("createUrbanIslet(): islet already exists" );
			return result;
		}
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		Geometry bigGeom = Geom.unionSFC(parcelSDS.getFeatureSource().getFeatures());
		DefaultFeatureCollection df = new DefaultFeatureCollection();
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaID("islet");
		int count = 0;
		for (int i = 0; i < bigGeom.getNumGeometries(); i++) {
			sfBuilder.add(bigGeom.getGeometryN(i));
			Object[] obj = {i};
			df.add(sfBuilder.buildFeature(String.valueOf(count++), obj));
		}
		parcelSDS.dispose();
		return Collec.exportSFC(df.collection(), result);
	}
}
