package fr.ign.artiscales.tools.geoToolsFunctions.vectors;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static DataStore getDataStore(URL url) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
        map.put(GeoPkgDataStoreFactory.DATABASE.key, url.toExternalForm());
        return DataStoreFinder.getDataStore(map);
    }

    public static DataStore getDataStore(File file) throws IOException {
        if (!file.exists() || !file.getName().endsWith(".gpkg")) {
            System.out.println(file + " doesn't exists or is not a geopackage");
            return null;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
        map.put(GeoPkgDataStoreFactory.DATABASE.key, file.getPath());
        return DataStoreFinder.getDataStore(map);
    }

    public static File exportSFCtoGPKG(SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft, boolean overwrite) throws IOException {
        if (!fileOut.getName().endsWith(".gpkg"))
            fileOut = new File(fileOut + ".gpkg");
        if (fileOut.exists())
            if (overwrite)
                Files.delete(fileOut.toPath());
            else
                return mergeGpkg(toExport, fileOut);
        Map<String, Object> params = new HashMap<>();
        params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
        params.put(GeoPkgDataStoreFactory.DATABASE.key, fileOut.getPath());
        params.put("create spatial index", Boolean.TRUE);
        DataStore newDataStore = DataStoreFinder.getDataStore(params);
        newDataStore.createSchema(ft);
        return CollecMgmt.makeTransaction(newDataStore, toExport, fileOut, ft);
    }

    public static File mergeGpkg(SimpleFeatureCollection toAdd, File existingGpkg) throws IOException {
        File tmpFile = new File(existingGpkg.getParentFile(), existingGpkg.getName().replace(".gpkg", "-temp.gpkg"));
        Files.copy(existingGpkg.toPath(), new FileOutputStream(tmpFile));
        Files.delete(existingGpkg.toPath());
        DataStore ds = getDataStore(tmpFile);
        assert ds != null;
        CollecMgmt.exportSFC(CollecMgmt.mergeSFC(Arrays.asList(toAdd, ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()),
                true, null), existingGpkg);
        ds.dispose();
        Files.delete(tmpFile.toPath());
        return existingGpkg;
    }

    public static File mergeGpkgFiles(List<File> file2MergeIn, File f) throws IOException {
        return mergeGpkgFiles(file2MergeIn, f, null, true);
    }

    public static File mergeGpkgFiles(List<File> filesToMerge, File fileOut, File boundFile, boolean keepAttributes) throws IOException {
        // stupid basic checkout
        if (filesToMerge.isEmpty()) {
            System.out.println("mergeGpkgFiles: list empty, " + fileOut + " null");
            return null;
        }
        // verify that every file exists and start a new function with clean list if not
        if (filesToMerge.stream().anyMatch(f -> !f.exists())) {
            List<File> rightListFile = new ArrayList<>();
            for (File file : filesToMerge)
                if (!file.exists())
                    System.out.println(file + " doesn't exists");
                else
                    rightListFile.add(file);
            return mergeGpkgFiles( rightListFile ,fileOut, boundFile, keepAttributes);
        }
        List<SimpleFeatureCollection> sfcs = new ArrayList<>(filesToMerge.size());
        for (File f : filesToMerge) {
            DataStore ds = getDataStore(f);
            assert ds != null;
            sfcs.add(DataUtilities.collection(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()));
            ds.dispose();
        }
        return CollecMgmt.exportSFC(CollecMgmt.mergeSFC(sfcs, keepAttributes, boundFile), fileOut, ".gpkg", true);
    }
}
