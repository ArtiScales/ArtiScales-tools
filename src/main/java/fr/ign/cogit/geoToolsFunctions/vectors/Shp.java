package fr.ign.cogit.geoToolsFunctions.vectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.util.IntersectUtils;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.grid.Grids;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.geoToolsFunctions.Attribute;
import fr.ign.cogit.geoToolsFunctions.Schemas;

public class Shp {
	public static File mergeVectFiles(List<File> file2MergeIn, File f) throws Exception {
		return mergeVectFiles(file2MergeIn, f, true);
	}
	
	public static File mergeVectFiles(List<File> file2MergeIn,File fileOut, boolean keepAttributes) throws Exception {
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
		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
		// check to prevent event in case of a willing of keeping attributes
		File fRef = file2MergeIn.get(0);
		ShapefileDataStore dSref = new ShapefileDataStore(fRef.toURI().toURL());
		SimpleFeatureType schemaRef = dSref.getFeatureSource().getFeatures().getSchema();
		dSref.dispose();
		// sfBuilder used only if number of attributes's the same but with different schemas
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.init(schemaRef);
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder defaultSFBuilder = new SimpleFeatureBuilder(featureType);
		boolean sameSchemas = true;
		lookOutAttribute: if (keepAttributes) {
			// check if the schemas of the shape are the same and if not, if they have the
			// same number of attributes
			int nbAttr = schemaRef.getAttributeCount();
			for (File f : file2MergeIn) {
				if (f.equals(fRef)) {
					continue;
				}
				ShapefileDataStore dSComp = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureType schemaComp = dSComp.getFeatureSource().getFeatures().getSchema();
				if (!schemaRef.equals(schemaComp)) {
					System.out.println(f + " have not the same schema as " + fRef
							+ ". Try to still add attribute if number is the same but output may be fuzzy");
					sameSchemas = false;
				}
				if (nbAttr != schemaComp.getAttributeCount()) {
					System.out.println(
							"Not the same amount of attributes in the shapefile : Output won't have any attributes");
					keepAttributes = false;
					break lookOutAttribute;
				}
				dSComp.dispose();
			}
		}
		dSref.dispose();
		for (File file : file2MergeIn) {
			ShapefileDataStore parcelSDS = new ShapefileDataStore(file.toURI().toURL());
			SimpleFeatureCollection parcelSFC = parcelSDS.getFeatureSource().getFeatures();
			if (keepAttributes) {
				// easy way
				if (sameSchemas) {
					Arrays.stream(parcelSFC.toArray(new SimpleFeature[0])).forEach(feat -> {
						newParcel.add(feat);
					});
				}
				// complicate case : if they doesn't have the exactly same schema but the same
				// number of attributes, we add every attribute regarding their position
				else {
					Arrays.stream(parcelSFC.toArray(new SimpleFeature[0])).forEach(feat -> {
						Object[] attr = new Object[feat.getAttributeCount() - 1];
						for (int h = 1; h < feat.getAttributeCount(); h++) {
							attr[h - 1] = feat.getAttribute(h);
						}
						defaultSFBuilder.add((Geometry) feat.getDefaultGeometry());
						newParcel.add(defaultSFBuilder.buildFeature(null, attr));
					});
				}
			} else {
				// if we don't want to keep attributes, we create features out of new features
				// containing only geometry
				Arrays.stream(parcelSFC.toArray(new SimpleFeature[0])).forEach(feat -> {
					defaultSFBuilder.set("the_geom", feat.getDefaultGeometry());
					newParcel.add(defaultSFBuilder.buildFeature(Attribute.makeUniqueId()));
				});
			}
			parcelSDS.dispose();
		}
		SimpleFeatureCollection output = newParcel.collection();
		if (boundFile != null && boundFile.exists()) {
			output = Collec.snapDatas(output, boundFile);
		}
		return Collec.exportSFC(output, fileOut);
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
	 * Warning !! untested since MC's PhD
	 * 
	 * @param inFile
	 *            Input shapeFile
	 * @param outFile
	 *            Output shapeFile
	 * @param name
	 *            Name of the simpleFeatureCollection
	 * @param gridResolution
	 *            Size of a side of the squared mesh
	 * 
	 * @return a shapefile with the cuted features
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File discretizeShp(File inFile, File outFile, String name, int gridResolution)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		ShapefileDataStore sds = new ShapefileDataStore(inFile.toURI().toURL());
		SimpleFeatureCollection input = sds.getFeatureSource().getFeatures();
		DefaultFeatureCollection dfCuted = new DefaultFeatureCollection();
		SimpleFeatureBuilder finalFeatureBuilder = Schemas.getBasicSchemaMultiPolygon(name+"-discretized");

		SpatialIndexFeatureCollection sifc = new SpatialIndexFeatureCollection(input);
		SimpleFeatureCollection gridFeatures = Grids.createSquareGrid(input.getBounds(), gridResolution).getFeatures();
		SimpleFeatureIterator iterator = gridFeatures.features();
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		int finalId = 0;
		try {
			while (iterator.hasNext()) {
				SimpleFeature featureGrid = iterator.next();
				Geometry gridGeometry = (Geometry) featureGrid.getDefaultGeometry();
				SimpleFeatureIterator chosenFeatIterator = sifc.subCollection(ff.bbox(ff.property("the_geom"), featureGrid.getBounds())).features();
				List<Geometry> list = new ArrayList<>();
				while (chosenFeatIterator.hasNext()) {
					SimpleFeature f = chosenFeatIterator.next();
					Geometry g = (Geometry) f.getDefaultGeometry();
					if (g.intersects(gridGeometry)) {
						list.add(g);
					}
				}
				Geometry coll = gridGeometry.getFactory()
						.createGeometryCollection(list.toArray(new Geometry[list.size()]));
				try {
					Geometry y = coll.union();
					if (y.isValid())
						coll = y;
				} catch (Exception e) {
				}
				Geometry unionGeom = IntersectUtils.intersection(coll, gridGeometry);
				try {
					Geometry y = unionGeom.buffer(0);
					if (y.isValid()) {
						unionGeom = y;
					}
				} catch (Exception e) {
				}
				if (unionGeom != null) {
					finalFeatureBuilder.add(unionGeom);
					dfCuted.add(finalFeatureBuilder.buildFeature(String.valueOf(finalId++)));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			iterator.close();
		}
		return Collec.exportSFC(dfCuted.collection(), outFile);
	}
	

	public static File snapDatas(File fileIn, File fileOut, SimpleFeatureCollection box) throws IOException {
		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		File result = Collec.exportSFC(Collec.snapDatas(shpDSIn.getFeatureSource().getFeatures(), Geom.unionSFC(box)), fileOut);
		shpDSIn.dispose();
		return result;
	}
	
	public static File snapDatas(File fileIn, File bBoxFile, File fileOut) throws IOException {
		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		// load the file to make the bbox and selectin with
		ShapefileDataStore shpDSZone = new ShapefileDataStore(bBoxFile.toURI().toURL());
		File result = Collec.exportSFC(
				Collec.snapDatas(shpDSIn.getFeatureSource().getFeatures(), Geom.unionSFC(shpDSZone.getFeatureSource().getFeatures())), fileOut);
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
			if (f.getName().startsWith(name) && f.getName().substring(0, f.getName().length()-4).equals(name)) {
				Files.delete(f.toPath());
			}
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
				String ext = f.getName().substring(f.getName().length() - 4, f.getName().length());
				FileOutputStream out = new FileOutputStream(new File(toFolder, newShpName+ext));
				Files.copy(f.toPath(), out);
				out.close();
			}
		}
	}
}
