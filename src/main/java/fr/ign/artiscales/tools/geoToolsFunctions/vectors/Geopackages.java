package fr.ign.artiscales.tools.geoToolsFunctions.vectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.opengis.feature.simple.SimpleFeatureType;

public class Geopackages {

//	public static void main(String[] args) throws IOException {
//		try {
//			ArrayList<File> fs = new ArrayList<File>();
//			for (File f : (new File("/tmp/CampSkeletonValues")).listFiles())
//				if (f.getName().endsWith(".gpkg"))
//					fs.add(f);
//			mergeGpkgFiles(fs, new File("/tmp/merged"));
//		}catch (Error e) {
//			
//		}
//	}
	
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
	
	public static File mergeGpkgFiles(List<File> file2MergeIn, File f) throws IOException {
		return mergeGpkgFiles(file2MergeIn, f,null, true);
	}
	
	public static File mergeGpkgFiles(List<File> file2MergeIn, File fileOut, File boundFile, boolean keepAttributes) throws IOException  {
		// stupid basic checkout
		if (file2MergeIn.isEmpty()) {
			System.out.println("mergeGpkgFiles: list empty, " + fileOut + " null");
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
		// check to prevent event in case of a willing of keeping attributes
		File fRef = file2MergeIn.get(0);
		DataStore dSref = getDataStore(fRef);
		SimpleFeatureType schemaRef = dSref.getFeatureSource(dSref.getTypeNames()[0]).getFeatures().getSchema();
		dSref.dispose();
		List<SimpleFeatureCollection> sfcs = new ArrayList<>();
		for (File f : file2MergeIn) {
			DataStore sds = getDataStore(f);
			sfcs.add(DataUtilities.collection(sds.getFeatureSource(sds.getTypeNames()[0]).getFeatures()));
			sds.dispose();
		}
		return Collec.exportSFC(Collec.mergeSFC(sfcs, schemaRef, keepAttributes, boundFile), fileOut, ".gpkg", true);
	}
}
