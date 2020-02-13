package fr.ign.cogit.geometryGeneration;

import java.io.File;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.GTFunctions.Schemas;
import fr.ign.cogit.GTFunctions.Vectors;

public class CityGeneration {
	public static File CreateIlots(File parcelFile, File outFolder) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		File result = new File(outFolder, "ilot.shp");
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcelSFC = parcelSDS.getFeatureSource().getFeatures();
		Geometry bigGeom = Vectors.unionSFC(parcelSFC);
		DefaultFeatureCollection df = new DefaultFeatureCollection();

		int nbGeom = bigGeom.getNumGeometries();
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaID("ilot");
		int count = 0;
		for (int i = 0; i < nbGeom; i++) {
			sfBuilder.add(bigGeom.getGeometryN(i));
			Object[] obj = {i};
			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(count), obj);
			
			df.add(feature);
			count++;
		}
		parcelSDS.dispose();
		return Vectors.exportSFC(df.collection(), result);
	}
}
