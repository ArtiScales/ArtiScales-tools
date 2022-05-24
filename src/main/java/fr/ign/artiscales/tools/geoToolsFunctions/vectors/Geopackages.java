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

//    public static void main(String[] args) throws IOException {
//        mergeGpkgFiles((Arrays.stream(new File("/home/mcolomb/INRIA/popSynth/Paris/").listFiles()).filter(f -> f.getName().startsWith("Individual")).toList()), new File("/tmp/merged.gpkg"), null, true, true);
//    }

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
        return mergeGpkgFiles(filesToMerge, fileOut, boundFile, keepAttributes, false);
    }

    public static File mergeGpkgFiles(List<File> filesToMerge, File fileOut, File boundFile, boolean keepAttributes, boolean largeFiles) throws IOException {
        // stupid basic checkout
        if (filesToMerge.isEmpty()) {
            System.out.println("mergeGpkgFiles: list empty, " + fileOut + " null");
            return null;
        }
        // verify that every file exists and start a new function with clean list if not
        if (filesToMerge.stream().anyMatch(f -> !f.exists())) {
            filesToMerge.stream().filter(f -> !f.exists()).forEach(file -> System.out.println(file + " doesn't exists"));
            return mergeGpkgFiles(filesToMerge.stream().filter(File::exists).toList(), fileOut, boundFile, keepAttributes);
        }
        if (!largeFiles) {
            List<SimpleFeatureCollection> sfcs = new ArrayList<>(filesToMerge.size());
            for (File f : filesToMerge) {
                DataStore ds = getDataStore(f);
                assert ds != null;
                sfcs.add(DataUtilities.collection(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()));
                ds.dispose();
            }
            return CollecMgmt.exportSFC(CollecMgmt.mergeSFC(sfcs, keepAttributes, boundFile), fileOut, ".gpkg", true);
        } else {
            SimpleFeatureCollection sfc = null;
            for (File f : filesToMerge) {
                DataStore ds = getDataStore(f);
                assert ds != null;
                if (sfc == null)
                    sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
                else {
                    DataStore dsAlreadyMerged = CollecMgmt.getDataStore(fileOut);
                    sfc = CollecMgmt.mergeSFC(Arrays.asList(dsAlreadyMerged.getFeatureSource(dsAlreadyMerged.getTypeNames()[0]).getFeatures(), ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()), keepAttributes, boundFile);
                    dsAlreadyMerged.dispose();
                }
                CollecMgmt.exportSFC(sfc, fileOut, ".gpkg", true);
                ds.dispose();
            }
            return fileOut;
        }
    }
}
