package fr.ign.artiscales.tools.geoToolsFunctions.vectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

public class Shp {
	public static File mergeVectFiles(List<File> file2MergeIn, File f) throws IOException {
		return mergeVectFiles(file2MergeIn, f, true);
	}
	
	public static File mergeVectFiles(List<File> file2MergeIn,File fileOut, boolean keepAttributes) throws IOException {
		return  mergeVectFiles(file2MergeIn, fileOut, null, keepAttributes) ;
	}

	/**
	 * Merge a list of shapeFiles. The method employed is depending on the schemas of the shapefiles and if the attributes need to be kept.
	 * <ul>
	 * <li>If shemas are the same, the simplefeatures are added to a defaultfeature collection</li>
	 * <li>If shemas aren't the same but have the same number of attributes, simple features are (a warning is sent)</li>
	 * <li>If shemas aren't the same with different number of attributes, only the geometry of the file are kept.</li>
	 * </ul>
	 * Possible to define an geometric bound on which only the intersecting data are kept.
	 * 
	 * @param file2MergeIn
	 *            List of shapefiles to merge
	 * @param fileOut
	 *            Output shapefile
	 * @param boundFile
	 *            Bound shapefile
	 * @param keepAttributes
	 *            Do we need to keep the attributes or not
	 * @return The merged ShapeFile
	 * @throws IOException
	 */
	public static File mergeVectFiles(List<File> file2MergeIn, File fileOut, File boundFile, boolean keepAttributes) throws IOException  {
		//		org.geotools.util.logging.Logging.getLogger("org.geotools.feature").setLevel(Level.OFF);
		// stupid basic checkout
		if (file2MergeIn.isEmpty()) {
			System.out.println("mergeVectFiles: list empty, " + fileOut + " null");
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
		ShapefileDataStore dSref = new ShapefileDataStore(fRef.toURI().toURL());
		SimpleFeatureType schemaRef = dSref.getFeatureSource().getFeatures().getSchema();
		dSref.dispose();
		List<SimpleFeatureCollection> sfcs = new ArrayList<>();
		for (File f : file2MergeIn) {
			ShapefileDataStore sds = new ShapefileDataStore(f.toURI().toURL());
			sfcs.add(sds.getFeatureSource().getFeatures());
			sds.dispose();
		}
		return Collec.exportSFC(Collec.mergeSFC(sfcs, schemaRef, keepAttributes, boundFile), fileOut, ".shp", true);
	}
	
	/**
	 * clean the shapefile of feature which area is inferior to areaMin
	 * 
	 * @param fileIn Input shapefile
	 * @param areaMin
	 * @return The {@link SimpleFeatureCollection} without tiny parcels
	 * @throws IOException 
	 */
	public static File delTinyParcels(File fileIn, File fileOut, double areaMin) throws IOException {
		ShapefileDataStore SDSParcel = new ShapefileDataStore(fileIn.toURI().toURL());
		File result = Collec.exportSFC(Collec.delTinyParcels(SDSParcel.getFeatureSource().getFeatures(), areaMin), fileOut);
		SDSParcel.dispose();
		return result;
	}
	
	/**
	 * Algorithm to spit a shapefile with a squared grid.
	 * 
	 * @param inFile
	 *            Input shapeFile
	 * @param outFile
	 *            Output shapeFile
	 * @param gridResolution
	 *            Size of a side of the squared mesh
	 * 
	 * @return a shapefile with the cuted features
	 * @throws IOException
	 */
	public static File gridDiscretizeShp(File inFile, File outFile, int gridResolution) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(inFile.toURI().toURL());
		SimpleFeatureCollection input = sds.getFeatureSource().getFeatures();
		File result = Collec.exportSFC(Collec.gridDiscretize(input, gridResolution), outFile);
		sds.dispose();
		return result;
	}

	public static File snapDatas(File fileIn, File fileOut, SimpleFeatureCollection box) throws IOException {
		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		File result = Collec.exportSFC(Collec.selectIntersection(shpDSIn.getFeatureSource().getFeatures(), Geom.unionSFC(box)), fileOut);
		shpDSIn.dispose();
		return result;
	}
	
	public static File snapDatas(File fileIn, File bBoxFile, File fileOut) throws IOException {
		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		// load the file to make the bbox and selectin with
		ShapefileDataStore shpDSZone = new ShapefileDataStore(bBoxFile.toURI().toURL());
		File result = Collec.exportSFC(
				Collec.selectIntersection(shpDSIn.getFeatureSource().getFeatures(), Geom.unionSFC(shpDSZone.getFeatureSource().getFeatures())), fileOut);
		shpDSZone.dispose();
		shpDSIn.dispose();
		return result;
	}
	
	/**
	 * copy the files of a shapefile to an other folder
	 * 
	 * @param name
	 *            Name of the shapefile
	 * @param fromFolder
	 *            Folder where the shapefile are located
	 * @param destinationFolder
	 *            Destination to where the folder is located
	 * @throws IOException
	 */
	public static void copyShp(String name, File fromFolder, File destinationFolder) throws IOException {
		for (File f : fromFolder.listFiles()) {
			if (f.getName().startsWith(name)) {
				FileOutputStream out = new FileOutputStream(new File(destinationFolder, f.getName()));
				Files.copy(f.toPath(), out);
				out.close();
			}
		}
	}
	
	/**
	 * delete the files of a shapefile to an other folder
	 * 
	 * @param name
	 *            name of the shapefile
	 * @param fromFolder
	 *            folder where the shapefile are located
	 * @throws IOException
	 */
	public static void deleteShp(String name, File fromFolder) throws IOException {
		for (File f : fromFolder.listFiles()) {
			if (f.getName().startsWith(name) && f.getName().substring(0, f.getName().length()-4).equals(name))
				Files.delete(f.toPath());
		}
	}

	/**
	 * copy the files of a shapefile to an other folder and rename it.
	 * Shapefile names must be withoud extension
	 * @param shpName
	 * @param newShpName
	 * @param fromFolder
	 * @param toFolder
	 * @throws IOException
	 */
	public static void copyShp(String shpName, String newShpName, File fromFolder, File toFolder) throws IOException {
		for (File f : fromFolder.listFiles()) {
			if (f.getName().startsWith(shpName)) {
				String ext = f.getName().substring(f.getName().length() - 4);
				FileOutputStream out = new FileOutputStream(new File(toFolder, newShpName+ext));
				Files.copy(f.toPath(), out);
				out.close();
			}
		}
	}
	
	public static File exportSFCtoSHP(SimpleFeatureCollection toExport, File fileOut, SimpleFeatureType ft, boolean overwrite)
			throws IOException {
		if (toExport.isEmpty()) {
			System.out.println(fileOut.getName() + " is empty");
			return fileOut;
		}
		if (!fileOut.getName().endsWith(".shp"))
			fileOut = new File(fileOut + ".shp");
		List<File> file2MergeIn = new ArrayList<>();
		// copyShp(String shpName, String newShpName, File fromFolder, File toFolder)

		if (fileOut.exists() && !overwrite) {
			String fileName = fileOut.getName().substring(0, fileOut.getName().length()-4);
			File newFile = new File(fileOut.getParentFile(), fileName + "tmp.shp");
			Shp.copyShp(fileName, fileName + "tmp", fileOut.getParentFile(), fileOut.getParentFile());
			file2MergeIn.add(newFile);
		}
		
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileOut.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(ft);
		File datFile = Collec.makeTransaction(newDataStore, toExport, fileOut, ft);
		file2MergeIn.add(datFile);
		File result = Shp.mergeVectFiles(file2MergeIn, fileOut);
		Shp.deleteShp(fileOut.getName().substring(0, fileOut.getName().length() - 4) + "tmp", fileOut.getParentFile());
		return result;
	}

}
