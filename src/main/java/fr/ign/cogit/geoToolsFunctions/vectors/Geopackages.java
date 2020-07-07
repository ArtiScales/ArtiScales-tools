package fr.ign.cogit.geoToolsFunctions.vectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.opengis.feature.simple.SimpleFeatureType;

public class Geopackages {

	public static DataStore getDataStore(File file) throws IOException {
		if (!file.exists() || !file.getName().endsWith(".gpkg")) {
			System.out.println(file + " doesn't exists or is not a geopackage");
			return null;
		}
		HashMap<String, Object> map = new HashMap<>();
		map.put(GeoPkgDataStoreFactory.DBTYPE.key,"geopkg");
		map.put(GeoPkgDataStoreFactory.DATABASE.key, file.getPath());
		return DataStoreFinder.getDataStore(map);
	}
	/*
	 * ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()
	 */

	
	public static File exportSFCtoGPKG(SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft, boolean overwrite)
			throws IOException {
		if (!fileOut.getName().endsWith(".gpkg"))
			fileOut = new File(fileOut + ".gpkg");
		if (fileOut.exists())
			if (overwrite)
				Files.delete(fileOut.toPath());
			else
				return mergeGpkg(toExport, fileOut);
		Map<String, Object> params = new HashMap<>();
		params.put(GeoPkgDataStoreFactory.DBTYPE.key,"geopkg");
		params.put(GeoPkgDataStoreFactory.DATABASE.key, fileOut.getPath());
		params.put("create spatial index", Boolean.TRUE);
		DataStore newDataStore = DataStoreFinder.getDataStore(params);
		newDataStore.createSchema(ft);
		return Collec.makeTransaction(newDataStore, toExport,  fileOut, ft);
	}
	
	public static File mergeGpkg(SimpleFeatureCollection toAdd, File existingGpkg) throws IOException {
		File tmpFile = new File(existingGpkg.getParentFile(), existingGpkg.getName().replace(".gpkg", "-temp.gpkg"));
		Files.copy(existingGpkg.toPath(), new FileOutputStream(tmpFile));
		Files.delete(existingGpkg.toPath());
		DataStore ds = getDataStore(tmpFile);
		Collec.exportSFC(Collec.mergeSFC(Arrays.asList(toAdd, ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()),
				true, null), existingGpkg);
		ds.dispose();
		Files.delete(tmpFile.toPath());
		return existingGpkg;
	}
}
